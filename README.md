# Annoying Mod (Fabric)

A small server-side Fabric mod. Vanilla content only. No custom items/blocks/sounds/textures.

## Target
- Minecraft **1.21.1** (Java edition)
- Fabric Loader **0.16.x**
- Fabric API matching MC 1.21.1
- Java **21**

> Note: there is no Minecraft "26.1.2". This project targets the closest real
> stable release. Bump `gradle.properties` if you want a different version.

## Build
```
./gradlew build
```
The mod jar appears in `build/libs/annoyingmod-1.0.0.jar`.
Drop it into your server (or client) `mods/` folder along with Fabric API.

## Run dev
```
./gradlew runServer
./gradlew runClient
```

## Configuration

Config file: `<game-or-server-dir>/config/annoyingmod.json`
It is auto-generated on first run with sensible defaults and inline comments
mirrored in this README.

```jsonc
{
  // ===== Chat messages =====
  // Up to 50 messages. Picked at random. Edit freely; no code changes needed.
  // A random message is broadcast every chatIntervalMinMinutes..chatIntervalMaxMinutes.
  "chatMessages": [
    "The forest whispers tonight...",
    "Did you hear that?",
    "Something moved in the dark."
  ],
  "chatIntervalMinMinutes": 15,
  "chatIntervalMaxMinutes": 45,

  // ===== Random sound events =====
  // Plays a vanilla block-place or footstep sound to random nearby players.
  // Scheduled independently from chat; the scheduler also guarantees it never
  // fires on the exact same server tick as a chat message.
  "soundsEnabled": true,
  "soundIntervalMinSeconds": 60,
  "soundIntervalMaxSeconds": 600,

  // ===== Acacia cross structures =====
  // Builds a vanilla-acacia-fence cross at ground level near a random player.
  // Shape: 3 vertical (base) + 1 left arm + 1 right arm at the top of the post.
  // Skipped if the spot would float or overwrite non-replaceable blocks.
  "crossesEnabled": true,
  "crossIntervalMinMinutes": 20,
  "crossIntervalMaxMinutes": 90,
  "crossSearchRadius": 32,

  // ===== Day/Night ratio =====
  // Night lasts exactly 2x the day. Implemented via gamerule + tick adjust.
  // Set false to leave the vanilla cycle untouched.
  "dayNightRatioEnabled": true,

  // ===== Random inventory drop =====
  // At a random interval, removes one randomly-picked item stack slot from
  // a random online player's main inventory and drops it at their feet.
  // Safe on empty inventories.
  "inventoryDropEnabled": true,
  "inventoryDropIntervalMinMinutes": 10,
  "inventoryDropIntervalMaxMinutes": 40
}
```

### Where to edit messages
Open `config/annoyingmod.json` and edit the `chatMessages` array. Save and
either restart the server or run `/annoyingmod reload` (op-only).

## Features
1. **Chat** — random message every 15–45 min (configurable). Up to 50.
2. **Sounds** — vanilla `block.*.place` or `block.*.step` to nearby players.
3. **Crosses** — acacia-fence cross at ground level, terrain-aware.
4. **Day/Night** — night = 2× day, stable across reloads.
5. **Inventory drop** — drops one random item stack from a random player.

All randomness uses `java.util.Random` seeded per-event.
All world mutation runs on the server thread for multiplayer safety.
