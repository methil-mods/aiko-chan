namespace backend.Llama;

public static class LlamaEndpoints
{
    public sealed record ChatRequest(string Message);

    public static IEndpointRouteBuilder MapLlamaEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapGet("/health", () => Results.Ok(new { status = "ok" }));

        endpoints.MapPost("/chat", async (ChatRequest req, ILlamaService llama, CancellationToken ct) =>
        {
            if (string.IsNullOrWhiteSpace(req.Message))
                return Results.BadRequest(new { error = "Body JSON required: { \"message\": \"...\" }" });

            var text = await llama.GenerateAsync(req.Message, ct);
            return Results.Ok(new { response = text });
        });

        endpoints.MapPost("/chat/split", async (ChatRequest req, ILlamaService llama, CancellationToken ct) =>
        {
            if (string.IsNullOrWhiteSpace(req.Message))
                return Results.BadRequest(new { error = "Body JSON required: { \"message\": \"...\" }" });

            var (reasoning, answer) = await llama.GenerateWithReasoningAsync(req.Message, ct);
            return Results.Ok(new { reasoning, answer });
        });

        return endpoints;
    }
}