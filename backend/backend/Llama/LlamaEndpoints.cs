using Microsoft.AspNetCore.Mvc;

namespace backend.Llama;

public static class LlamaEndpoints
{
    public sealed record ChatRequest(string PromptId, string Message);

    public static IEndpointRouteBuilder MapLlamaEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapGet("/health", () => Results.Ok(new { status = "ok" }));

        endpoints.MapPost("/chat", async (
            [FromBody] ChatRequest req,
            [FromServices] ILlamaService llama,
            CancellationToken ct) =>
        {
            if (string.IsNullOrWhiteSpace(req.PromptId) || string.IsNullOrWhiteSpace(req.Message))
                return Results.BadRequest(new { error = "Body JSON required: { \"promptId\": \"...\", \"message\": \"...\" }" });

            var text = await llama.GenerateFinalLineAsync(req.PromptId, req.Message, ct);
            return Results.Ok(new { response = text });
        });

        endpoints.MapPost("/chat/split", async (
            [FromBody] ChatRequest req,
            [FromServices] ILlamaService llama,
            CancellationToken ct) =>
        {
            if (string.IsNullOrWhiteSpace(req.PromptId) || string.IsNullOrWhiteSpace(req.Message))
                return Results.BadRequest(new { error = "Body JSON required: { \"promptId\": \"...\", \"message\": \"...\" }" });

            var (reasoning, answer) = await llama.GenerateWithReasoningAsync(req.PromptId, req.Message, ct);
            return Results.Ok(new { reasoning, answer });
        });
        
        endpoints.MapPost("/chat/slow", async (
            [FromBody] ChatRequest req,
            [FromServices] ILlamaService llama,
            CancellationToken ct) =>
        {
            if (string.IsNullOrWhiteSpace(req.PromptId) || string.IsNullOrWhiteSpace(req.Message))
                return Results.BadRequest(new { error = "Body JSON required: { \"promptId\": \"...\", \"message\": \"...\" }" });

            // message long
            var message = req.Message + "\n\nÉcris une réponse détaillée (au moins 300 mots) et donne 10 exemples.";

            var text = await llama.GenerateFinalLineAsync(req.PromptId, message, ct);

            return Results.Ok(new { response = text });
        });

        return endpoints;
    }
}