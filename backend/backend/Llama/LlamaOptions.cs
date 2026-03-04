namespace backend.Llama;

public sealed class LlamaOptions
{
    public const string SectionName = "Llama";

    /// <summary>Chemin vers le .gguf. Relatif => résolu depuis ContentRootPath.</summary>
    public string ModelPath { get; set; } = "Models/model.gguf";

    public uint ContextSize { get; set; } = 4096;

    /// <summary>Nombre de layers offload GPU (Metal). 0 = CPU only.</summary>
    public int GpuLayers { get; set; } = 0;

    public int MaxTokens { get; set; } = 256;

    /// <summary>Prompt system par défaut (si tu l’utilises).</summary>
    public string SystemPrompt { get; set; } = "You are a helpful assistant.";
}