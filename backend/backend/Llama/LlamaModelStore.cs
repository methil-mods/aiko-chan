using System.Collections.Concurrent;
using LLama;
using LLama.Common;
using Microsoft.Extensions.Options;

namespace backend.Llama;

public sealed class LlamaModelStore : IDisposable
{
    private readonly ILogger<LlamaModelStore> _logger;
    private readonly LlamaOptions _opt;
    private readonly IHostEnvironment _env;

    // Lazy cache per model key ("default" or mapping keys)
    private readonly ConcurrentDictionary<string, Lazy<LLamaWeights>> _weightsByKey = new();

    public LlamaModelStore(
        IOptions<LlamaOptions> options,
        IHostEnvironment env,
        ILogger<LlamaModelStore> logger)
    {
        _logger = logger;
        _opt = options.Value;
        _env = env;

        // Fail-fast uniquement sur le modèle par défaut (celui de ModelPath)
        var defaultPath = ResolveModelPath(_opt.ModelPath, _env.ContentRootPath);
        if (!File.Exists(defaultPath))
            throw new FileNotFoundException($"GGUF file not found: {defaultPath}");
    }

    public LLamaWeights GetWeights(string? modelKey = null)
    {
        var (key, path) = ResolveKeyAndPath(modelKey);

        var lazy = _weightsByKey.GetOrAdd(key, _ => new Lazy<LLamaWeights>(() =>
        {
            _logger.LogInformation("Loading LLM weights for key '{Key}' from {Path}", key, path);

            var mp = new ModelParams(path)
            {
                // ContextSize/GpuLayers seront surtout utilisés au CreateContext,
                // mais les setter ici ne fait pas de mal.
                ContextSize = _opt.ContextSize,
                GpuLayerCount = _opt.GpuLayers,
            };

            return LLamaWeights.LoadFromFile(mp);
        }, isThreadSafe: true));

        return lazy.Value;
    }

    private (string key, string path) ResolveKeyAndPath(string? requestedKey)
    {
        if (string.IsNullOrWhiteSpace(requestedKey))
        {
            var p = ResolveModelPath(_opt.ModelPath, _env.ContentRootPath);
            if (!File.Exists(p))
                throw new FileNotFoundException($"GGUF file not found: {p}");
            return ("default", p);
        }

        var key = requestedKey.Trim();

        if (_opt.Models is null || _opt.Models.Count == 0)
            throw new ArgumentException("No Llama:Models mapping configured, but a model key was provided.");

        if (!_opt.Models.TryGetValue(key, out var configuredPath) || string.IsNullOrWhiteSpace(configuredPath))
        {
            var available = string.Join(", ", _opt.Models.Keys.OrderBy(k => k));
            throw new ArgumentException($"Unknown model key '{key}'. Available: [{available}]");
        }

        var path = ResolveModelPath(configuredPath, _env.ContentRootPath);
        if (!File.Exists(path))
            throw new FileNotFoundException($"GGUF file not found for key '{key}': {path}");

        return (key, path);
    }

    private static string ResolveModelPath(string configuredPath, string contentRoot)
    {
        if (Path.IsPathRooted(configuredPath))
            return configuredPath;

        return Path.Combine(contentRoot, configuredPath);
    }

    public void Dispose()
    {
        foreach (var kv in _weightsByKey)
        {
            try
            {
                if (kv.Value.IsValueCreated)
                    kv.Value.Value.Dispose();
            }
            catch
            {
                // ignore
            }
        }

        _weightsByKey.Clear();
    }
}