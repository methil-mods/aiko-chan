namespace backend.Llama;

public interface ILlamaService
{
    Task<string> GenerateFinalLineAsync(
        string promptId,
        string userMessage,
        CancellationToken ct = default);

    Task<(string reasoning, string answer)> GenerateWithReasoningAsync(
        string promptId,
        string userMessage,
        CancellationToken ct = default);
}