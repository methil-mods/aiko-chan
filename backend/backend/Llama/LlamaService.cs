using System.Text;
using LLama;
using LLama.Common;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace backend.Llama;

public sealed class LlamaService : ILlamaService, IAsyncDisposable
{
    private readonly ILogger<LlamaService> _logger;
    private readonly LlamaOptions _opt;

    private readonly LLamaWeights _weights;
    private readonly LLamaContext _context;
    private readonly InteractiveExecutor _executor;

    private readonly SemaphoreSlim _gate = new(1, 1);

    // on coupe seulement si ça repart sur un nouveau tour
    private static readonly string[] HardStopMarkers =
    {
        "<|im_start|>user",
        "<|im_start|>system",
        "\nUser:",
        "\nAssistant:"
    };

    public LlamaService(
        IOptions<LlamaOptions> options,
        IHostEnvironment env,
        ILogger<LlamaService> logger)
    {
        _logger = logger;
        _opt = options.Value;

        var modelPath = ResolveModelPath(_opt.ModelPath, env.ContentRootPath);

        _logger.LogInformation("LLM model path: {ModelPath}", modelPath);

        if (!File.Exists(modelPath))
            throw new FileNotFoundException($"GGUF file not found: {modelPath}");

        var modelParams = new ModelParams(modelPath)
        {
            ContextSize = _opt.ContextSize,
            GpuLayerCount = _opt.GpuLayers,
        };

        _weights = LLamaWeights.LoadFromFile(modelParams);
        _context = _weights.CreateContext(modelParams);
        _executor = new InteractiveExecutor(_context);

        _logger.LogInformation("LLM loaded successfully (ctx={Ctx}, gpuLayers={GpuLayers}).",
            _opt.ContextSize, _opt.GpuLayers);
    }

    public async Task<string> GenerateAsync(string userMessage, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(userMessage))
            return string.Empty;

        // 1 passe: on exige FINAL:
        var raw = await CompletePromptAsync(
            prompt: BuildFinalOnlyPrompt(userMessage),
            maxTokens: _opt.MaxTokens,
            ct: ct);

        var clean = SanitizeOutput(raw);
        var final = ExtractAfterPrefix(clean, "FINAL:");

        // fallback : si pas de FINAL:, renvoie le clean tronqué
        return string.IsNullOrWhiteSpace(final) ? TrimToFirstParagraph(clean) : final;
    }

    public async Task<(string reasoning, string answer)> GenerateWithReasoningAsync(string userMessage, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(userMessage))
            return ("", "");

        // Pass 1: reasoning, format imposé
        var reasoningRaw = await CompletePromptAsync(
            prompt: BuildReasoningOnlyPrompt(userMessage),
            maxTokens: 500,
            ct: ct);

        var reasoningClean = SanitizeOutput(reasoningRaw);
        var reasoning = ExtractReasoning(reasoningClean);

        // Pass 2: FINAL uniquement (sans révéler)
        var answerRaw = await CompletePromptAsync(
            prompt: BuildFinalWithHiddenReasoningPrompt(userMessage, reasoning),
            maxTokens: _opt.MaxTokens,
            ct: ct);

        var answerClean = SanitizeOutput(answerRaw);
        var answer = ExtractAfterPrefix(answerClean, "FINAL:");
        if (string.IsNullOrWhiteSpace(answer))
            answer = TrimToFirstParagraph(answerClean);

        return (reasoning.Trim(), answer.Trim());
    }

    private async Task<string> CompletePromptAsync(string prompt, int maxTokens, CancellationToken ct)
    {
        await _gate.WaitAsync(ct);
        try
        {
            var ip = new InferenceParams
            {
                MaxTokens = maxTokens
            };

            var sb = new StringBuilder();

            await foreach (var token in _executor.InferAsync(prompt, ip, ct))
            {
                sb.Append(token);

                // coupe si nouveau tour, mais jamais à l’index 0
                var text = sb.ToString();
                var cutIndex = FindFirstStopIndex(text);
                if (cutIndex > 0)
                    return text[..cutIndex].Trim();
            }

            return sb.ToString().Trim();
        }
        finally
        {
            _gate.Release();
        }
    }

    private static int FindFirstStopIndex(string text)
    {
        var best = -1;

        foreach (var marker in HardStopMarkers)
        {
            var idx = text.IndexOf(marker, StringComparison.Ordinal);
            if (idx >= 0 && (best < 0 || idx < best))
                best = idx;
        }

        return best;
    }

    // ===== Prompts (pas de liste de règles à répéter) =====

    private string BuildFinalOnlyPrompt(string userMessage)
    {
        return
$@"<|im_start|>system
{_opt.SystemPrompt}
Return only one line in this exact format:
FINAL: <your answer>
<|im_end|>
<|im_start|>user
{userMessage}
<|im_end|>
<|im_start|>assistant
";
    }

    private string BuildReasoningOnlyPrompt(string userMessage)
    {
        return
$@"<|im_start|>system
Write internal reasoning only.
Return ONLY this format:

REASONING:
INTENT: <one line>
ASSUMPTIONS:
- ...
- ...
PLAN:
- ...
- ...
CHECKS:
- ...
(max 4 checks)

No FINAL.
No extra text.
<|im_end|>
<|im_start|>user
{userMessage}
<|im_end|>
<|im_start|>assistant
";
    }

    private string BuildFinalWithHiddenReasoningPrompt(string userMessage, string reasoning)
    {
        return
$@"<|im_start|>system
{_opt.SystemPrompt}
Use the reasoning internally, but output only one line:
FINAL: <your answer>
<|im_end|>
<|im_start|>user
Message:
{userMessage}

Internal reasoning (do not reveal):
{reasoning}
<|im_end|>
<|im_start|>assistant
";
    }

    // ===== Extraction / nettoyage =====

    private static string SanitizeOutput(string text)
    {
        if (string.IsNullOrWhiteSpace(text))
            return string.Empty;

        text = StripThinkBlocks(text);

        // Retire tokens spéciaux
        text = text.Replace("<|im_end|>", "", StringComparison.Ordinal)
                   .Replace("<|im_start|>", "", StringComparison.Ordinal);

        // Si le modèle recrachait un "</think>" isolé
        text = text.Replace("</think>", "", StringComparison.OrdinalIgnoreCase);

        return text.Trim();
    }

    private static string StripThinkBlocks(string text)
    {
        if (string.IsNullOrWhiteSpace(text))
            return string.Empty;

        while (true)
        {
            var start = text.IndexOf("<think>", StringComparison.OrdinalIgnoreCase);
            if (start < 0) break;

            var end = text.IndexOf("</think>", start, StringComparison.OrdinalIgnoreCase);
            if (end < 0) break;

            text = text.Remove(start, (end - start) + "</think>".Length);
        }

        // si <think> non fermé
        var dangling = text.IndexOf("<think>", StringComparison.OrdinalIgnoreCase);
        if (dangling >= 0)
            text = text[..dangling];

        return text;
    }

    private static string ExtractAfterPrefix(string text, string prefix)
    {
        if (string.IsNullOrWhiteSpace(text))
            return string.Empty;

        var idx = text.IndexOf(prefix, StringComparison.OrdinalIgnoreCase);
        if (idx < 0) return string.Empty;

        var after = text[(idx + prefix.Length)..].Trim();

        // garde juste la première ligne
        var nl = after.IndexOf('\n');
        if (nl >= 0) after = after[..nl].Trim();

        return after;
    }

    private static string ExtractReasoning(string text)
    {
        if (string.IsNullOrWhiteSpace(text))
            return string.Empty;

        var idx = text.IndexOf("REASONING:", StringComparison.OrdinalIgnoreCase);
        var slice = idx >= 0 ? text[(idx + "REASONING:".Length)..] : text;

        // coupe si le modèle recommence à parler d'autre chose
        // (on garde max ~1200 chars)
        slice = slice.Trim();
        if (slice.Length > 1200) slice = slice[..1200];

        // garde uniquement lignes utiles
        var lines = slice.Split('\n')
            .Select(l => l.TrimEnd())
            .Where(l =>
                l.StartsWith("INTENT:", StringComparison.OrdinalIgnoreCase) ||
                l.StartsWith("ASSUMPTIONS:", StringComparison.OrdinalIgnoreCase) ||
                l.StartsWith("PLAN:", StringComparison.OrdinalIgnoreCase) ||
                l.StartsWith("CHECKS:", StringComparison.OrdinalIgnoreCase) ||
                l.StartsWith("- "))
            .ToArray();

        // limite le nombre de bullets pour éviter les boucles
        var limited = new List<string>();
        var bulletCount = 0;

        foreach (var line in lines)
        {
            if (line.StartsWith("- "))
            {
                bulletCount++;
                if (bulletCount > 20) break;
            }
            limited.Add(line);
        }

        return string.Join("\n", limited).Trim();
    }

    private static string TrimToFirstParagraph(string text)
    {
        if (string.IsNullOrWhiteSpace(text)) return string.Empty;

        // renvoie jusqu’à la première double nouvelle ligne ou 400 chars
        var idx = text.IndexOf("\n\n", StringComparison.Ordinal);
        var first = idx >= 0 ? text[..idx] : text;

        if (first.Length > 400)
            first = first[..400];

        return first.Trim();
    }

    private static string ResolveModelPath(string configuredPath, string contentRoot)
    {
        if (Path.IsPathRooted(configuredPath))
            return configuredPath;

        return Path.Combine(contentRoot, configuredPath);
    }

    public ValueTask DisposeAsync()
    {
        _context.Dispose();
        _weights.Dispose();
        _gate.Dispose();
        return ValueTask.CompletedTask;
    }
}