# Astro Loop

A top-down roguelike shooter for Android. Survive waves of asteroids and enemies, collect upgrades, and evolve your weapons.

## Gameplay

- Fly your ship and destroy incoming asteroids before they reach you
- Defeat enemies and bosses to earn upgrade power-ups
- Choose from a pool of weapons and passives each upgrade round
- Weapons level up and can evolve into stronger variants at max level
- Difficulty scales continuously — how long can you survive?

## Weapons

12 base weapons, each with 5 levels and a unique evolution:

| Weapon | Description |
|--------|-------------|
| Pulse Cannon | Auto-aiming energy bolts |
| Energy Saw | Spinning disc that shreds on contact |
| Scatter Shot | Wide pellet spread |
| Homing Missiles | Lock-on projectiles |
| Ion Orbiters | Orbiting energy spheres |
| Railgun | Slow piercing sniper |
| Space Mines | Dropped explosives |
| Solar Storm | Random piercing strikes |
| Nova Blast | Periodic AoE burst |
| Needle Gun | Rapid piercing needles |
| Cluster Bomb | Splits into smaller blasts |
| Flak Cannon | Exploding shells |

Each weapon evolves into a stronger variant at max level when paired with the right passive.

## Enemies

- **Asteroids** — the core threat, splitting on destruction
- **Enemy Ships** — AI-driven attackers with smooth pursuit behavior
- **Rangers** — ranged enemies that maintain distance and burst-fire when cornered
- **Bosses** — large, fast targets that launch drones and accelerate over time

## Building

Requires Android SDK 36 and JDK 17.

```bash
# Debug build
./gradlew assembleDebug

# Output location
app/build/outputs/apk/debug/app-debug.apk
```

## Installing on a Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Tech Stack

- **Language:** Kotlin
- **Platform:** Android (API 24–34)
- **Build:** Gradle 8.2
- **Rendering:** Custom vector renderer (no game engine dependency)

## License

Astro Loop is free software licensed under the **GNU General Public License v3.0**
(`GPL-3.0-only`) — see [`LICENSE`](LICENSE).

Copyright (C) 2026 PubDeer

The bundled fonts (Exo 2, Orbitron) are licensed separately under the SIL Open
Font License 1.1; see [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
