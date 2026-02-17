# hytale-sourcequery

A2S query protocol plugin for Hytale servers. Lets game server browsers query your server for info, player list, and rules.

You can find this plugin on CurseForge - https://www.curseforge.com/hytale/mods/source-query-a2s

## Building

```bash
mvn clean package
```

The jar will be in `target/`.

## Installation

Drop the jar into your Hytale server's `plugins/` folder.

## Configuration

Set the `QUERY_PORT` environment variable to change the UDP port. If not set, it defaults to your game port + 1.

```bash
QUERY_PORT=27015
```

This plugin automatically checks for updates using the github API. To disable this feature, set the `SOURCEQUERY_UPDATE_CHECK`
environment variable to `false`.

```bash
SOURCEQUERY_UPDATE_CHECK=false
```

## What it exposes

- **A2S_INFO** - Server name, map, player count, max players, game version
- **A2S_PLAYER** - List of connected players
- **A2S_RULES** - Server rules (version, protocol, world info, plugin count, etc.)

## Testing

You can test with any Source query tool:

```bash
npx gamedig --type protocol-valve 127.0.0.1:27016
```
