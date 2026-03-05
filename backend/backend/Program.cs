using backend.Llama;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddOpenApi();

// Options (appsettings + env vars)
builder.Services
    .AddOptions<LlamaOptions>()
    .Bind(builder.Configuration.GetSection(LlamaOptions.SectionName))
    .Validate(o => !string.IsNullOrWhiteSpace(o.ModelPath), "Llama:ModelPath is required.")
    .ValidateOnStart();

// DI
builder.Services.AddMemoryCache();
builder.Services.AddSingleton<LlamaModelStore>(); // weights singleton
builder.Services.AddSingleton<ILlamaService, LlamaService>();

// Charge au démarrage
builder.Services.AddHostedService<LlamaWarmupHostedService>();

var app = builder.Build();

// Serve wwwroot/index.html for local SSE testing (same-origin -> no CORS)
app.UseDefaultFiles();
app.UseStaticFiles();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}
else
{
    app.UseHttpsRedirection();
}

app.MapLlamaEndpoints();

app.Run();