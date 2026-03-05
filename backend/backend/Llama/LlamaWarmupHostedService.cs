namespace backend.Llama;

public sealed class LlamaWarmupHostedService : IHostedService
{
    private readonly ILogger<LlamaWarmupHostedService> _logger;
    private readonly ILlamaService _llama;

    public LlamaWarmupHostedService(ILogger<LlamaWarmupHostedService> logger, ILlamaService llama)
    {
        _logger = logger;
        _llama = llama;
    }

    public async Task StartAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("LLM warmup starting...");

        // Petite requête pour valider la chaîne complète.
        // Si le modèle/arch est incompatible, ça aura déjà crashé au ctor du service.
        var _ = await _llama.GenerateFinalLineAsync("a", "ping", model: null, ct: cancellationToken);

        _logger.LogInformation("LLM warmup done.");
    }

    public Task StopAsync(CancellationToken cancellationToken) => Task.CompletedTask;
}