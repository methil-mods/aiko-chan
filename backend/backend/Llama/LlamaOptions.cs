namespace backend.Llama;

public sealed class LlamaOptions
{
    public const string SectionName = "Llama";

    public string ModelPath { get; set; } = "";
    public Dictionary<string, string> Models { get; set; } = new();

    public uint ContextSize { get; set; } = 4096;
    public int GpuLayers { get; set; } = 0;
    public int MaxTokens { get; set; } = 512;
    public string SystemPrompt { get; set; } = "";
}