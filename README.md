<div align="center">

# San Essentials

**Teleport, homes and everyday utility commands for NeoForge 1.21.1.**

[![CurseForge](https://img.shields.io/badge/CurseForge-San%20Essentials-F16436?style=flat&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/sanessentials)
[![Modrinth](https://img.shields.io/badge/Modrinth-San%20Essentials-00AF5C?style=flat&logo=modrinth&logoColor=white)](https://modrinth.com/mod/sanessentials)

</div>

San Essentials is a lightweight essentials mod for **NeoForge 1.21.1**. It adds the commands players actually ask for — `/tpa`, `/home`, `/warp`, `/back`, `/fly`, `/heal`, `/repeat` — and lets you switch each of them on or off, set the permission level, and tune cooldowns and distances from a single TOML file.

Starting with **2.0.0**, development is focused on NeoForge 1.21.1. Older Fabric and Minecraft 26.1.2 files remain available on CurseForge for legacy users.

## Features

- **Server-side friendly.** No client mod is required in multiplayer; installing it on the client is only needed for singleplayer.
- **Every command is configurable.** Toggle commands, set permission levels, and tune RTP distances and cooldowns.
- **Safe random teleport.** `/rtp` stays inside the active world border and only teleports to validated ground on the surface.
- **Polished feedback.** Clickable accept/deny buttons, titles, sounds, and hover hints instead of plain chat text.
- **Player picker menus.** `/tpa` and `/tpahere` without a target open a paginated chest menu of online players.
- **Warp menu.** `/warps` opens a paginated chest menu of every server warp.
- **Protection** against creeper, enderman, and ravager block damage.
- **TPA aliases.** `/tpaccept`, `/tpyes`, `/tpdeny`, and `/tpno` work alongside the native commands.
- **Multilingual.** Messages follow the client's language, with a configurable `fallbackLanguage`.
- **Persistent player data.** Homes, warps, back locations, fly and god mode survive restarts.
- **Multiple named homes.** Each player can save several homes and pick one with `/home <name>`; the limit is configurable (`maxHomes`, 1-10, default 5).

## Supported Versions

| Loader | Minecraft | Java |
| --- | --- | --- |
| NeoForge | 1.21.1 | 21+ |

Older Fabric and Minecraft 26.1.2 builds are legacy and no longer receive updates.

## Commands

### Teleport

| Command | Description |
| --- | --- |
| `/tpa [player]` | Request to teleport to another player, or open the player menu when omitted. |
| `/tpaa` | Accept a pending TPA request. |
| `/tpad` | Deny a pending TPA request. |
| `/tpaccept`, `/tpyes` | Aliases for `/tpaa`. |
| `/tpdeny`, `/tpno` | Aliases for `/tpad`. |
| `/tpaauto` | Toggle automatic TPA acceptance. |
| `/tpahere [player]` | Request another player to teleport to you, or open the player menu when omitted. |
| `/tpaherea` | Accept a pending TPAhere request. |
| `/tpahered` | Deny a pending TPAhere request. |
| `/rtp` | Randomly teleport to a safe location inside the world border. |
| `/spawn` | Teleport to the overworld spawn. |
| `/back` or `/dback` | Return to your last death location. |
| `/tback` | Return to your previous teleport location. |

### Homes And Warps

| Command | Description |
| --- | --- |
| `/sethome [name]` | Set a home. Omitting the name sets the default home used by `/home`. |
| `/home [name]` | Teleport to a named home, or the default home when the name is omitted. |
| `/homes` | List your saved homes and how many slots you are using. |
| `/delhome [name]` | Delete a named home, or the default home when the name is omitted. |
| `/setwarp <name>` | Create a server warp. |
| `/warp <name>` | Teleport to a server warp. |
| `/warps` | Open a paginated menu of every server warp. |
| `/delwarp <name>` | Delete a server warp. |

Home names are lowercased and limited to letters, numbers and underscores (32 characters max). `/home` without a name falls back to listing your homes if the default one is missing, and tab-completion offers the homes you own.

### Utility And Admin

| Command | Description |
| --- | --- |
| `/trash` | Open a temporary trash inventory that clears on close. |
| `/suicide` | Kill yourself. Works even while god mode is enabled. |
| `/heal [player]` | Heal yourself or another player. |
| `/fly [player]` | Toggle persistent flight for yourself or another player. |
| `/god [player]` | Toggle god mode for yourself or another player. |
| `/repeat <times> [command]` | Repeat your last command or a specified command. |
| `/san-ess help` | Show the command list. Disabled or unavailable commands are shown in red. |
| `/san-ess reset all` | Clear all player data. |
| `/san-ess reset notice` | Clear notice trigger data. |

## Configuration

Everything lives in `config/san_essentials-common.toml`, written with a comment above each option. Commands are grouped under `[commands.<name>]` with `enabled` and `permissionLevel`:

```toml
messagePrefixEnabled = false
preventCreeperBlockDamage = true
maxHomes = 5
teleportRequestTimeoutSeconds = 60
fallbackLanguage = "en_us"

[commands.tpa]
enabled = true
permissionLevel = 0
```

`messagePrefixEnabled` controls the `[SE]` tag in front of mod messages; it is off by default.

RTP options:

```toml
rtpCooldownSeconds = 30
rtpMinDistance = 2000
rtpMaxDistance = 5000
rtpNetherMinDistance = 600
rtpNetherMaxDistance = 1500
rtpEndMinDistance = -1
rtpEndMaxDistance = -1
```

`rtpEndMinDistance` and `rtpEndMaxDistance` control RTP in the End. Leave them at `-1` to reuse the overworld values.

Existing values are preserved across updates; new options are added with their defaults. Unknown options left over from an older version are dropped on load and the original file is kept as a `.pre-config-migration.bak` backup.

Player data is stored inside the world folder at `data/san_essentials_player_data.dat`. A schema upgrade writes a `.dat.bak` copy of the previous file before converting it.

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Download the matching San Essentials jar.
3. Drop it into the server's `mods` folder.
4. Restart the server.

No client mod is required for multiplayer.

## Building

Requires JDK 25.

```powershell
.\build-all-versions.ps1
```

Or build directly:

```powershell
.\gradlew.bat buildNeoForge --no-daemon --no-configuration-cache
```

Jars land in `build/libs/` as:

```text
san_essentials-neoforge-<version>.jar
```

The build script automatically increments the patch version when a jar with the same or a lower version already exists. For example, after `san_essentials-neoforge-2.0.1.jar` exists, the next build produces `san_essentials-neoforge-2.0.2.jar`. The new version is written back to `mod_version` in `gradle.properties` after a successful build.

## Legacy Versions

Fabric and Minecraft 26.1.2 builds were published before 2.0.0 and are no longer updated. They remain available on CurseForge for users who still need them.

## Credits & License

San Essentials is a fork and rebrand of [PPE Essentials](https://github.com/PurpleEastZQ/ppe-essentials) by Purple_East, used under the MIT License, which also covers this project. Original copyright is retained in [LICENSE](LICENSE).
