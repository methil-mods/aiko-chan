using LLama;
using LLama.Common;
using Microsoft.Extensions.Options;

namespace backend.Llama;

public sealed class LlamaModelStore : IDisposable
{
    private readonly object _lock = new();
    private readonly ILogger<LlamaModelStore> _logger;
    private readonly LlamaOptions _opt;
    private readonly string _modelPath;

    private LLamaWeights? _weights;

    public LlamaModelStore(
        IOptions<LlamaOptions> options,
        IHostEnvironment env,
        ILogger<LlamaModelStore> logger)
    {
        _logger = logger;
        _opt = options.Value;

        _modelPath = Path.IsPathRooted(_opt.ModelPath)
            ? _opt.ModelPath
            : Path.Combine(env.ContentRootPath, _opt.ModelPath);

        if (!File.Exists(_modelPath))
            throw new FileNotFoundException($"GGUF file not found: {_modelPath}");
    }

    public LLamaWeights GetWeights()
    {
        if (_weights is not null) return _weights;

        lock (_lock)
        {
            if (_weights is not null) return _weights;

            _logger.LogInformation("Loading LLM weights from {ModelPath}", _modelPath);

            var p = new ModelParams(_modelPath)
            {
                // les weights n'ont pas besoin de ContextSize, mais ce ModelParams est requis par l'API
                ContextSize = _opt.ContextSize,
                GpuLayerCount = _opt.GpuLayers,
            };

            _weights = LLamaWeights.LoadFromFile(p);

            _logger.LogInformation("LLM weights loaded.");
            return _weights;
        }
    }

    public void Dispose()
    {
        _weights?.Dispose();
        _weights = null;
    }
}