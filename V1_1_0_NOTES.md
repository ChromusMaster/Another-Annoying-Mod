# Annoying Mod v1.1.0 - Embedded Custom Sounds

This package adds embedded OGG custom sounds and a server-side custom sound controller.

## New audio groups

- Death: random sound every 4 player deaths.
- Night: random night sound every 5 in-game nights, at night start. If the player tries to sleep before that night's sound fires, sleep is blocked and the sound plays.
- Morning: random morning sound every 5 in-game days, scheduled 5-10 real minutes after dawn.
- Item/drop: `spongebob-boowomp` plays every 50 valuable item drops.
- Item/found: `magic-fairy` plays when valuable item count increases while a non-crafting container is open.
- KillMob: random sound every 30 passive/non-hostile mob kills based on vanilla kill statistics.
- Enderman: random sound every 20 times an Enderman targets the player.
- Misc/EvilLaugh: up to 5 sounds on an active misc day, no immediate repeats, every 3 in-game days.

## Audio notes

Original uploaded MP3 files were converted to OGG Vorbis, mono, 44.1 kHz, with a maximum duration cap of 60 seconds.

## Commands

Server-side custom sound test:

```mcfunction
/annoyingmod test custom_sound
```

Vanilla client sound test:

```mcfunction
/annoyingmodclient test sound
```

## Configuration

New options:

```json
"customSoundsEnabled": true,
"customSoundIntervalMinSeconds": 180,
"customSoundIntervalMaxSeconds": 300
```

These intervals are used by the misc custom sound pool. Event-driven custom sounds use internal counters and cooldowns.
