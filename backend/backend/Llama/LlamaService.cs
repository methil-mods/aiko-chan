using System.Diagnostics;
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
        "\nAssistant:",
        "<think>"
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
        var promptId = Guid.NewGuid().ToString("N");
        var (text, _) = await GenerateFinalLineAsync(promptId, userMessage, model: null, ct: ct);
        return text;
    }

    // ====== API utilisée par endpoints ======

    public async Task<(string text, UsageMetrics usage)> GenerateFinalLineAsync(
        string promptId,
        string userMessage,
        string? model = null,
        CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(promptId))
            throw new ArgumentException("promptId is required", nameof(promptId));

        if (string.IsNullOrWhiteSpace(userMessage))
            return (string.Empty, UsageMetrics.Empty);

        var session = GetOrCreateSession(promptId, model);

        var completion = await session.CompleteAsync(
            prompt: BuildFinalOnlyPrompt(userMessage),
            maxTokens: _opt.MaxTokens,
            ct: ct);

        var clean = SanitizeOutput(completion.Text);
        var final = ExtractAfterPrefix(clean, "FINAL:");
        var text = string.IsNullOrWhiteSpace(final) ? TrimToFirstParagraph(clean) : final;

        return (text, completion.Usage);
    }

    public async IAsyncEnumerable<(string Token, string Response, UsageMetrics Usage, bool Done)> StreamFinalAsync(
        string promptId,
        string userMessage,
        string? model = null,
        [System.Runtime.CompilerServices.EnumeratorCancellation]
        CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(promptId))
            throw new ArgumentException("promptId is required", nameof(promptId));

        if (string.IsNullOrWhiteSpace(userMessage))
        {
            yield return ("", "", UsageMetrics.Empty, true);
            yield break;
        }

        var session = GetOrCreateSession(promptId, model);

        var inThink = false;
        var responseClean = new StringBuilder();

        // anti-spam "Answer:"
        var seenAnswerLabel = false;
        var forceDone = false;

        await foreach (var item in session.StreamAsync(
                           prompt: BuildStreamAnswerPrompt(userMessage),
                           maxTokens: _opt.MaxTokens,
                           ct: ct))
        {
            var rawToken = item.Token ?? string.Empty;
            var usage = item.Usage;
            var done = item.Done;

            // remove obvious control tokens
            rawToken = rawToken.Replace("<|im_end|>", "", StringComparison.Ordinal)
                .Replace("<|im_start|>", "", StringComparison.Ordinal)
                .Replace("</think>", "", StringComparison.OrdinalIgnoreCase);

            // Filter out <think>...</think>
            var visibleToken = FilterThinkToken(rawToken, ref inThink);

            // Strip "Answer:" + stop if it starts looping
            visibleToken = FilterAnswerLabels(
                visibleToken,
                ref seenAnswerLabel,
                alreadyProducedLen: responseClean.Length,
                out var stopBecauseAnswerLoop);

            if (stopBecauseAnswerLoop)
                forceDone = true;

            // ✅ STOP AU PREMIER RETOUR LIGNE (on veut UNE seule ligne)
            var nlInToken = visibleToken.IndexOf('\n');
            if (nlInToken >= 0)
            {
                visibleToken = visibleToken[..nlInToken];
                forceDone = true;
            }

            if (!string.IsNullOrEmpty(visibleToken))
                responseClean.Append(visibleToken);

            var responseSoFar = responseClean.ToString();

            // Si jamais une newline s'est glissée via un token précédent, on coupe
            var nlInResponse = responseSoFar.IndexOf('\n');
            if (nlInResponse >= 0)
            {
                responseSoFar = responseSoFar[..nlInResponse];
                responseClean.Clear();
                responseClean.Append(responseSoFar);
                forceDone = true;
            }

            // ✅ Le stream ne doit pas leak "FINAL:"
            if (responseSoFar.StartsWith("FINAL:", StringComparison.OrdinalIgnoreCase))
            {
                responseSoFar = responseSoFar["FINAL:".Length..].TrimStart();
            }

            // ✅ Si le modèle recommence un second "FINAL:" => boucle => stop
            // (ça arrive quand il “relance” une nouvelle réponse)
            if (responseSoFar.Contains("FINAL:", StringComparison.OrdinalIgnoreCase))
            {
                // garde uniquement la partie après le premier FINAL:
                var firstFinal = responseSoFar.IndexOf("FINAL:", StringComparison.OrdinalIgnoreCase);
                if (firstFinal >= 0)
                    responseSoFar = responseSoFar[(firstFinal + "FINAL:".Length)..].TrimStart();
            }

            var isDone = forceDone || done;

            yield return (visibleToken, responseSoFar, usage, isDone);

            if (isDone)
                yield break;
        }
    }

    private string BuildStreamAnswerPrompt(string userMessage)
    {
        return
            $@"<|im_start|>system
{_opt.SystemPrompt}

You must output exactly ONE line.
It must start with: FINAL:
No other text. No lists. No explanations.
Do NOT output <think>.
Do NOT include role labels like 'User:' or 'Assistant:'.
Do NOT use emojis.
<|im_end|>
<|im_start|>user
{userMessage}
<|im_end|>
<|im_start|>assistant
FINAL:";
    }


    public async Task<(string reasoning, string answer, UsageMetrics totalUsage)> GenerateWithReasoningAsync(
        string promptId,
        string userMessage,
        string? model = null,
        CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(promptId))
            throw new ArgumentException("promptId is required", nameof(promptId));

        if (string.IsNullOrWhiteSpace(userMessage))
            return ("", "", UsageMetrics.Empty);

        var session = GetOrCreateSession(promptId, model);

        // Pass 1 reasoning
        var reasoningCompletion = await session.CompleteAsync(
            prompt: BuildReasoningOnlyPrompt(userMessage),
            maxTokens: 500,
            ct: ct);

        var reasoningClean = SanitizeOutput(reasoningCompletion.Text);
        var reasoning = ExtractReasoning(reasoningClean);

        // Pass 2 answer (FINAL)
        var answerCompletion = await session.CompleteAsync(
            prompt: BuildFinalWithHiddenReasoningPrompt(userMessage, reasoning),
            maxTokens: _opt.MaxTokens,
            ct: ct);

        var answerClean = SanitizeOutput(answerCompletion.Text);

        if (string.IsNullOrWhiteSpace(answerClean))
            answerClean = StripThinkBlocks(answerCompletion.Text).Trim();

        var answer = ExtractAfterPrefix(answerClean, "FINAL:");
        if (string.IsNullOrWhiteSpace(answer))
            answer = TrimToFirstParagraph(answerClean);

        var total = UsageMetrics.Combine(reasoningCompletion.Usage, answerCompletion.Usage);

        return (reasoning.Trim(), answer.Trim(), total);
    }

    private PromptSession GetOrCreateSession(string promptId, string? model)
    {
        var modelKey = string.IsNullOrWhiteSpace(model) ? "default" : model.Trim();
        var cacheKey = $"{modelKey}::{promptId}";

        return _cache.GetOrCreate(cacheKey, entry =>
        {
            entry.SlidingExpiration = TimeSpan.FromMinutes(2);

            entry.RegisterPostEvictionCallback((_, value, _, _) =>
            {
                if (value is PromptSession s)
                {
                    try { s.Dispose(); } catch { /* ignore */ }
                }
            });

            var weights = _modelStore.GetWeights(model);

            var p = new ModelParams("unused")
            {
                ContextSize = _opt.ContextSize,
                GpuLayerCount = _opt.GpuLayers
            };

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

        public async Task<CompletionResult> CompleteAsync(string prompt, int maxTokens, CancellationToken ct)
        {
            await _gate.WaitAsync(ct);
            try
            {
                // Input tokens
                int inputTokens;
                try
                {
                    // LLamaSharp expose Tokenize(text, add_bos, special, encoding)
                    var toks = _context.Tokenize(prompt);
                    inputTokens = toks.Length;
                }
                catch
                {
                    // fallback si jamais Tokenize diffère selon versions/backends
                    inputTokens = 0;
                }

                var sw = Stopwatch.StartNew();

                var ip = new InferenceParams
                {
                    MaxTokens = maxTokens,
                    // on garde l’inférence simple; arrêt via HardStopMarkers ci-dessous
                };

                var sb = new StringBuilder();
                var outputTokens = 0;

                await foreach (var chunk in _executor.InferAsync(prompt, ip, ct))
                {
                    outputTokens++;
                    sb.Append(chunk);

                    // coupe si nouveau tour
                    var text = sb.ToString();
                    var cut = FindFirstStopIndex(text);
                    if (cut > 0)
                    {
                        sw.Stop();
                        var trimmed = text[..cut].Trim();
                        var usage = UsageMetrics.From(inputTokens, outputTokens, sw.ElapsedMilliseconds);
                        return new CompletionResult(trimmed, usage);
                    }
                }

                sw.Stop();
                var final = sb.ToString().Trim();
                return new CompletionResult(final,
                    UsageMetrics.From(inputTokens, outputTokens, sw.ElapsedMilliseconds));
            }
            finally
            {
                _gate.Release();
            }
        }

        public sealed record StreamChunk(string Token, string Response, UsageMetrics Usage, bool Done);

        public async IAsyncEnumerable<StreamChunk> StreamAsync(
            string prompt,
            int maxTokens,
            [System.Runtime.CompilerServices.EnumeratorCancellation]
            CancellationToken ct)
        {
            await _gate.WaitAsync(ct);
            try
            {
                int inputTokens;
                try
                {
                    var toks = _context.Tokenize(prompt);
                    inputTokens = toks.Length;
                }
                catch
                {
                    inputTokens = 0;
                }

                var sw = Stopwatch.StartNew();

                var ip = new InferenceParams
                {
                    MaxTokens = maxTokens,
                };

                var sb = new StringBuilder();
                var outputTokens = 0;

                await foreach (var token in _executor.InferAsync(prompt, ip, ct))
                {
                    outputTokens++;
                    sb.Append(token);

                    // Stop if the model starts a new turn
                    var full = sb.ToString();
                    var cut = FindFirstStopIndex(full);
                    if (cut > 0)
                    {
                        sw.Stop();
                        var trimmed = full[..cut].Trim();
                        var usage = UsageMetrics.From(inputTokens, outputTokens, sw.ElapsedMilliseconds);
                        yield return new StreamChunk("", trimmed, usage, true);
                        yield break;
                    }

                    var usageLive = UsageMetrics.From(inputTokens, outputTokens, sw.ElapsedMilliseconds);
                    yield return new StreamChunk(token, full, usageLive, false);
                }

                sw.Stop();
                var final = sb.ToString().Trim();
                var usageFinal = UsageMetrics.From(inputTokens, outputTokens, sw.ElapsedMilliseconds);
                yield return new StreamChunk("", final, usageFinal, true);
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

    public sealed record CompletionResult(string Text, UsageMetrics Usage);

    public sealed record UsageMetrics(
        int InputTokens,
        int OutputTokens,
        int TokensConsumed,
        double TokensPerSecond,
        long ElapsedMs)
    {
        public static readonly UsageMetrics Empty = new(0, 0, 0, 0, 0);

        public static UsageMetrics From(int inputTokens, int outputTokens, long elapsedMs)
        {
            var consumed = Math.Max(0, inputTokens) + Math.Max(0, outputTokens);
            var sec = Math.Max(0.001, elapsedMs / 1000.0);
            var tps = outputTokens <= 0 ? 0 : outputTokens / sec;
            return new UsageMetrics(inputTokens, outputTokens, consumed, tps, elapsedMs);
        }

        public static UsageMetrics Combine(UsageMetrics a, UsageMetrics b)
        {
            var input = a.InputTokens + b.InputTokens;
            var output = a.OutputTokens + b.OutputTokens;
            var elapsed = a.ElapsedMs + b.ElapsedMs;

            // tps total basé sur output total / temps total
            var sec = Math.Max(0.001, elapsed / 1000.0);
            var tps = output <= 0 ? 0 : output / sec;

            return new UsageMetrics(input, output, input + output, tps, elapsed);
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

    private static string FilterThinkToken(string token, ref bool inThink)
    {
        if (string.IsNullOrEmpty(token))
            return string.Empty;

        var output = new StringBuilder();
        var i = 0;

        while (i < token.Length)
        {
            if (!inThink)
            {
                var start = token.IndexOf("<think>", i, StringComparison.OrdinalIgnoreCase);
                if (start < 0)
                {
                    // no think start -> all remaining is visible
                    output.Append(token.AsSpan(i));
                    break;
                }

                // append visible part before <think>
                if (start > i)
                    output.Append(token.AsSpan(i, start - i));

                // enter think mode
                inThink = true;
                i = start + "<think>".Length;
                continue;
            }
            else
            {
                var end = token.IndexOf("</think>", i, StringComparison.OrdinalIgnoreCase);
                if (end < 0)
                {
                    // still inside think; drop rest of this token
                    break;
                }

                // exit think mode
                inThink = false;
                i = end + "</think>".Length;
                continue;
            }
        }

        return output.ToString();
    }
    
    private static string FilterAnswerLabels(string token, ref bool seenAnswerLabel, int alreadyProducedLen, out bool forceDone)
    {
        forceDone = false;

        if (string.IsNullOrEmpty(token))
            return string.Empty;

        // Certains modèles sortent "Answer:" (et le répètent plusieurs fois).
        // On le supprime. Si on le revoit après avoir déjà produit du texte -> on stop le stream.
        var idx = token.IndexOf("Answer:", StringComparison.OrdinalIgnoreCase);
        if (idx < 0)
            return token;

        // Si on a déjà du contenu ET qu'on a déjà vu un Answer: auparavant => répétition => stop
        if (alreadyProducedLen > 0 && seenAnswerLabel)
        {
            forceDone = true;
            return token[..idx];
        }

        // Première occurrence: on strip le label et on continue
        seenAnswerLabel = true;

        var before = token[..idx];
        var after = token[(idx + "Answer:".Length)..];

        // retire un espace éventuel après "Answer:"
        if (after.Length > 0 && after[0] == ' ')
            after = after[1..];

        return before + after;
    }
}