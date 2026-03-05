FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS base
WORKDIR /app
EXPOSE 8080
EXPOSE 8081

# Dépendances natives courantes pour llama.cpp CPU
RUN apt-get update && apt-get install -y --no-install-recommends \
    libstdc++6 libgcc-s1 libgomp1 \
 && rm -rf /var/lib/apt/lists/*

FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
ARG BUILD_CONFIGURATION=Release
WORKDIR /src

COPY ["backend/backend.csproj", "backend/"]
# restore "rid-aware" pour prendre les assets linux
RUN dotnet restore "backend/backend.csproj" -r linux-x64

COPY . .
WORKDIR "/src/backend"

FROM build AS publish
ARG BUILD_CONFIGURATION=Release
RUN dotnet publish "./backend.csproj" -c $BUILD_CONFIGURATION -o /app/publish \
    -r linux-x64 --self-contained false /p:UseAppHost=false

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "backend.dll"]