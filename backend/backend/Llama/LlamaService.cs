using System.Text;
using LLama;
using LLama.Common;
using Microsoft.Extensions.Caching.Memory;
using Microsoft.Extensions.Options;

namespace backend.Llama;

public sealed class LlamaService : ILlamaService
{
    private readonly ILogger<LlamaService> _logger;
    private readonly IMemoryCache _cache;
    private readonly LlamaModelStore _modelStore;
    private readonly LlamaOptions _opt;

    // coupe seulement si ça repart sur un nouveau tour
    private static readonly string[] HardStopMarkers =
    {
        "<|im_start|>user",
        "<|im_start|>system",
        "\nUser:",
        "\nAssistant:"
    };

    public LlamaService(
        IMemoryCache cache,
        LlamaModelStore modelStore,
        IOptions<LlamaOptions> options,
        ILogger<LlamaService> logger)
    {
        _cache = cache;
        _modelStore = modelStore;
        _opt = options.Value;
        _logger = logger;
    }

    public async Task<string> GenerateAsync(string userMessage, CancellationToken ct = default)
    {
        // compat: si tu appelles GenerateAsync directement sans promptId,
        // tu peux générer un id ici (mais alors pas de cache partagé)
        var promptId = Guid.NewGuid().ToString("N");
        var raw = await GenerateFinalLineAsync(promptId, userMessage, ct);
        return raw;
    }

    public async Task<(string reasoning, string answer)> GenerateWithReasoningAsync(string userMessage, CancellationToken ct = default)
    {
        var promptId = Guid.NewGuid().ToString("N");
        return await GenerateWithReasoningAsync(promptId, userMessage, ct);
    }

    // version utilisée par l’endpoint (avec PromptId)
    public async Task<(string reasoning, string answer)> GenerateWithReasoningAsync(string promptId, string userMessage, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(promptId))
            throw new ArgumentException("promptId is required", nameof(promptId));

        if (string.IsNullOrWhiteSpace(userMessage))
            return ("", "");

        var session = GetOrCreateSession(promptId);

        // Pass 1 reasoning
        var reasoningRaw = await session.CompleteAsync(
            prompt: BuildReasoningOnlyPrompt(userMessage),
            maxTokens: 500,
            ct: ct);

        var reasoningClean = SanitizeOutput(reasoningRaw);
        var reasoning = ExtractReasoning(reasoningClean);

        // Pass 2 answer (FINAL)
        var answerRaw = await session.CompleteAsync(
            prompt: BuildFinalWithHiddenReasoningPrompt(userMessage, reasoning),
            maxTokens: _opt.MaxTokens,
            ct: ct);

        var answerClean = SanitizeOutput(answerRaw);

        // si sanitize a tout vidé (ça arrive quand il crache des tokens spéciaux), fallback plus soft
        if (string.IsNullOrWhiteSpace(answerClean))
        {
            answerClean = StripThinkBlocks(answerRaw).Trim();
        }

        var answer = ExtractAfterPrefix(answerClean, "FINAL:");
        if (string.IsNullOrWhiteSpace(answer))
            answer = TrimToFirstParagraph(answerClean);

        return (reasoning.Trim(), answer.Trim());
    }

    public async Task<string> GenerateFinalLineAsync(string promptId, string userMessage, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(promptId))
            throw new ArgumentException("promptId is required", nameof(promptId));

        if (string.IsNullOrWhiteSpace(userMessage))
            return string.Empty;

        var session = GetOrCreateSession(promptId);

        var raw = await session.CompleteAsync(
            prompt: BuildFinalOnlyPrompt(userMessage),
            maxTokens: _opt.MaxTokens,
            ct: ct);

        var clean = SanitizeOutput(raw);
        if (string.IsNullOrWhiteSpace(clean))
            clean = StripThinkBlocks(raw).Trim();

        var final = ExtractAfterPrefix(clean, "FINAL:");
        if (string.IsNullOrWhiteSpace(final))
        {
            var fallback = TrimToFirstParagraph(clean);
            // if fallback is literally just "FINAL:" then return empty string
            if (string.Equals(fallback.Trim(), "FINAL:", StringComparison.OrdinalIgnoreCase))
                return string.Empty;
            return fallback;
        }

        return final;
    }

    private PromptSession GetOrCreateSession(string promptId)
    {
        return _cache.GetOrCreate(promptId, entry =>
        {
            entry.SlidingExpiration = TimeSpan.FromMinutes(2);

            // quand ça expire, on dispose le context
            entry.RegisterPostEvictionCallback((key, value, reason, state) =>
            {
                if (value is PromptSession s)
                {
                    try { s.Dispose(); } catch { /* ignore */ }
                }
            });

            var weights = _modelStore.GetWeights();

            // 1 context par session (parallélisable entre sessions)
            var p = new ModelParams("unused")
            {
                ContextSize = _opt.ContextSize,
                GpuLayerCount = _opt.GpuLayers
            };

            // LlamaSharp: CreateContext prend un ModelParams; on utilise ceux de l’opt
            var ctx = weights.CreateContext(p);
            return new PromptSession(ctx);
        })!;
    }

    // ===== Session =====

    private sealed class PromptSession : IDisposable
    {
        private readonly LLamaContext _context;
        private readonly InteractiveExecutor _executor;
        private readonly SemaphoreSlim _gate = new(1, 1); // protège CE context

        public PromptSession(LLamaContext context)
        {
            _context = context;
            _executor = new InteractiveExecutor(_context);
        }

        public async Task<string> CompleteAsync(string prompt, int maxTokens, CancellationToken ct)
        {
            await _gate.WaitAsync(ct);
            try
            {
                var ip = new InferenceParams { MaxTokens = maxTokens };
                var sb = new StringBuilder();

                await foreach (var token in _executor.InferAsync(prompt, ip, ct))
                {
                    sb.Append(token);

                    // coupe si nouveau tour
                    var text = sb.ToString();
                    var cut = FindFirstStopIndex(text);
                    if (cut > 0) return text[..cut].Trim();
                }

                return sb.ToString().Trim();
            }
            finally
            {
                _gate.Release();
            }
        }

        public void Dispose()
        {
            _gate.Dispose();
            _context.Dispose();
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

    // ===== Prompts =====

    private string BuildFinalOnlyPrompt(string userMessage)
    {
        return
$@"<|im_start|>system
{_opt.SystemPrompt}
You must output exactly ONE line.
It must start with: FINAL:
No other text. No lists. No explanations.

Example:
FINAL: Bonjour

<|im_end|>
<|im_start|>user
{userMessage}
<|im_end|>
<|im_start|>assistant
FINAL:";
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
PLAN:
- ...
CHECKS:
- ...

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
You must output exactly ONE line.
It must start with: FINAL:
No other text. No lists. No explanations.

Example:
FINAL: Bonjour

<|im_end|>
<|im_start|>user
Message:
{userMessage}

Internal reasoning (do not reveal):
{reasoning}
<|im_end|>
<|im_start|>assistant
FINAL:";
    }

    // ===== Nettoyage / extraction =====

    private static string SanitizeOutput(string text)
    {
        if (string.IsNullOrWhiteSpace(text))
            return string.Empty;

        text = StripThinkBlocks(text);
        text = text.Replace("<|im_end|>", "", StringComparison.Ordinal)
                   .Replace("<|im_start|>", "", StringComparison.Ordinal)
                   .Replace("</think>", "", StringComparison.OrdinalIgnoreCase);

        return text.Trim();
    }

    private static string StripThinkBlocks(string text)
    {
        while (true)
        {
            var s = text.IndexOf("<think>", StringComparison.OrdinalIgnoreCase);
            if (s < 0) break;

            var e = text.IndexOf("</think>", s, StringComparison.OrdinalIgnoreCase);
            if (e < 0) break;

            text = text.Remove(s, (e - s) + "</think>".Length);
        }

        var dangling = text.IndexOf("<think>", StringComparison.OrdinalIgnoreCase);
        if (dangling >= 0) text = text[..dangling];

        return text;
    }

    private static string ExtractAfterPrefix(string text, string prefix)
    {
        var idx = text.IndexOf(prefix, StringComparison.OrdinalIgnoreCase);
        if (idx < 0) return string.Empty;

        var after = text[(idx + prefix.Length)..].Trim();
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
        slice = slice.Trim();
        if (slice.Length > 2000) slice = slice[..2000];

        var lines = slice.Split('\n')
            .Select(l => l.TrimEnd())
            .Where(l =>
                l.StartsWith("INTENT:", StringComparison.OrdinalIgnoreCase) ||
                l.StartsWith("ASSUMPTIONS:", StringComparison.OrdinalIgnoreCase) ||
                l.StartsWith("PLAN:", StringComparison.OrdinalIgnoreCase) ||
                l.StartsWith("CHECKS:", StringComparison.OrdinalIgnoreCase) ||
                l.StartsWith("- "))
            .ToList();

        // stop dès que ça recommence (INTENT qui revient)
        var outLines = new List<string>();
        var seenFirstIntent = false;
        var bullets = 0;

        foreach (var line in lines)
        {
            if (line.StartsWith("INTENT:", StringComparison.OrdinalIgnoreCase))
            {
                if (seenFirstIntent) break;
                seenFirstIntent = true;
            }

            if (line.StartsWith("- "))
            {
                bullets++;
                if (bullets > 24) break;
            }

            outLines.Add(line);
        }

        return string.Join("\n", outLines).Trim();
    }

    private static string TrimToFirstParagraph(string text)
    {
        text = text.Trim();
        var idx = text.IndexOf("\n\n", StringComparison.Ordinal);
        var first = idx >= 0 ? text[..idx] : text;
        if (first.Length > 600) first = first[..600];
        return first.Trim();
    }
}