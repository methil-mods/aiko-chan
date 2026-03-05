using Microsoft.AspNetCore.Mvc;

namespace backend.Llama;

public static class LlamaEndpoints
{
    public sealed record ChatRequest(string PromptId, string Message, string? Model = null);

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

            var (text, usage) = await llama.GenerateFinalLineAsync(req.PromptId, req.Message, req.Model, ct);

            return Results.Ok(new
            {
                response = text,
                inputTokens = usage.InputTokens,
                outputTokens = usage.OutputTokens,
                tokensConsumed = usage.TokensConsumed,
                tokensPerSecond = usage.TokensPerSecond,
                elapsedMs = usage.ElapsedMs
            });
        });

        // SSE: stream token-by-token with the same metrics as /chat
        // EventSource is GET-only, so we accept params in query string.
        endpoints.MapGet("/chat/stream", async (
            HttpContext http,
            [FromQuery] string promptId,
            [FromQuery] string message,
            [FromQuery] string? model,
            [FromServices] ILlamaService llama,
            CancellationToken ct) =>
        {
            if (string.IsNullOrWhiteSpace(promptId) || string.IsNullOrWhiteSpace(message))
            {
                http.Response.StatusCode = StatusCodes.Status400BadRequest;
                await http.Response.WriteAsync("Missing query params: promptId, message", ct);
                return;
            }

            http.Response.Headers.ContentType = "text/event-stream";
            http.Response.Headers.CacheControl = "no-cache";
            http.Response.Headers.Connection = "keep-alive";
            http.Response.Headers["X-Accel-Buffering"] = "no";
            http.Response.Headers["Access-Control-Allow-Origin"] = "*";

            await foreach (var chunk in llama.StreamFinalAsync(promptId, message, model, ct))
            {
                var payload = System.Text.Json.JsonSerializer.Serialize(new
                {
                    token = chunk.Token,
                    response = chunk.Response,
                    inputTokens = chunk.Usage.InputTokens,
                    outputTokens = chunk.Usage.OutputTokens,
                    tokensConsumed = chunk.Usage.TokensConsumed,
                    tokensPerSecond = chunk.Usage.TokensPerSecond,
                    elapsedMs = chunk.Usage.ElapsedMs,
                    done = chunk.Done
                });

                await http.Response.WriteAsync($"event: token\ndata: {payload}\n\n", ct);
                await http.Response.Body.FlushAsync(ct);

                if (chunk.Done) break;
            }
        });

        endpoints.MapPost("/chat/split", async (
            [FromBody] ChatRequest req,
            [FromServices] ILlamaService llama,
            CancellationToken ct) =>
        {
            if (string.IsNullOrWhiteSpace(req.PromptId) || string.IsNullOrWhiteSpace(req.Message))
                return Results.BadRequest(new { error = "Body JSON required: { \"promptId\": \"...\", \"message\": \"...\" }" });

            var (reasoning, answer, usage) = await llama.GenerateWithReasoningAsync(req.PromptId, req.Message, req.Model, ct);

            return Results.Ok(new
            {
                reasoning,
                answer,
                inputTokens = usage.InputTokens,
                outputTokens = usage.OutputTokens,
                tokensConsumed = usage.TokensConsumed,
                tokensPerSecond = usage.TokensPerSecond,
                elapsedMs = usage.ElapsedMs
            });
        });

        return endpoints;
    }
}