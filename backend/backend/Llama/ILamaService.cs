namespace backend.Llama;

public interface ILlamaService
{
    Task<string> GenerateAsync(string userMessage, CancellationToken ct = default);

    Task<(string reasoning, string answer)> GenerateWithReasoningAsync(
        string userMessage,
        CancellationToken ct = default);
}