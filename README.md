<div align="center">

# San Essentials

**Teleport, homes and everyday utility commands.**

[![CurseForge](https://img.shields.io/badge/CurseForge-San%20Essentials-F16436?style=flat&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/sanessentials)
[![Modrinth](https://img.shields.io/badge/Modrinth-San%20Essentials-00AF5C?style=flat&logo=modrinth&logoColor=white)](https://modrinth.com/mod/sanessentials)

</div>

San Essentials is a lightweight essentials mod for Fabric and NeoForge. It adds the commands players actually ask for — `/tpa`, `/home`, `/warp`, `/back`, `/fly`, `/heal`, `/repeat` — and lets you switch each of them on or off, set the permission level, and tune cooldowns and distances from a single TOML file.

## Features

- **Server-side only.** Players don't need to install anything; install it on the client too if you want it in singleplayer.
- **Every command is configurable.** Toggle commands, set permission levels, adjust RTP distance and cooldowns.
- **Block griefing protection** against creeper explosions, endermen picking up blocks, and ravagers trampling crops.
- **Polished feedback.** Clickable accept/deny buttons, titles, sounds, and hover hints instead of plain chat text.
- **Player picker menus.** `/tpa` and `/tpahere` without a target open a paginated chest menu of online players.
- **Multilingual.** Messages follow the client's language, with a configurable `fallbackLanguage` (9 languages included).
- **Persistent player data.** Homes, warps, back locations, fly and god mode survive restarts.
- **Multiple named homes.** Each player can save several homes and pick one with `/home <name>`; the limit is configurable (`maxHomes`, 1-10, default 5).
- **Light dependencies.** Fabric builds require Fabric API and nothing else.

## Supported Versions

| Loader | Minecraft |
| --- | --- |
| Fabric | 1.21.1, 26.1.2 |
| NeoForge | 1.21.1, 26.1.2 |

## Commands

### Teleport

| Command | Description |
| --- | --- |
| `/tpa [player]` | Request to teleport to another player, or open the player menu when omitted. |
| `/tpaa` | Accept a pending TPA request. |
| `/tpad` | Deny a pending TPA request. |
| `/tpaauto` | Toggle automatic TPA acceptance. |
| `/tpahere [player]` | Request another player to teleport to you, or open the player menu when omitted. |
| `/tpaherea` | Accept a pending TPAhere request. |
| `/tpahered` | Deny a pending TPAhere request. |
| `/rtp` | Randomly teleport nearby. |
| `/spawn` | Teleport to world spawn. |
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
| `/delwarp <name>` | Delete a server warp. |

Home names are lowercased and limited to letters, numbers and underscores (32 characters max). `/home` without a name falls back to listing your homes if the default one is missing, and tab-completion offers the homes you own.

### Utility And Admin

| Command | Description |
| --- | --- |
| `/trash` | Open a temporary trash inventory that clears on close. |
| `/suicide` | Kill yourself. |
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

Unknown options left over from an older version are dropped on load and the original file is kept as a `.pre-config-migration.bak` backup.

Player data is stored inside the world folder — `data/san_essentials_player_data.dat` on Minecraft 1.21.1 and `data/san_essentials/player_data.dat` on 26.1.2. A schema upgrade writes a `.dat.bak` copy of the previous file before converting it, so an interrupted migration can always be rolled back by hand.

## Installation

Download the jar matching your loader and Minecraft version, then drop it into `mods`. Fabric builds also require Fabric API.

## Building

Requires JDK 25.

```powershell
.\build-all-versions.ps1
```

To build a single target:

```powershell
.\gradlew.bat buildNeoForge --no-daemon --no-configuration-cache
.\gradlew.bat buildFabric "-Ptarget_minecraft_version=26.1.2" --no-daemon --no-configuration-cache
```

Jars land in `build/libs/` as `san_essentials-<loader>-<minecraft>-<version>.jar`.

## Upgrading From PPE Essentials

San Essentials runs under the new `san_essentials` mod id, so it does **not** read the old `ppe_essentials` config or the stored player data (homes, warps, back, fly, god). Expect a clean slate: re-copy your settings into `config/san_essentials-common.toml` and let players set their homes again, or run both mods once and move data by hand.

## Credits & License

San Essentials is a fork and rebrand of [PPE Essentials](https://github.com/PurpleEastZQ/ppe-essentials) by Purple_East, used under the MIT License, which also covers this project. Original copyright is retained in [LICENSE](LICENSE).
