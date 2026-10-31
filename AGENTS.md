# Soulbound Companions — Codex Development Instructions

## Project

Soulbound Companions is a Minecraft Java Edition mod that introduces player-bound evolutionary companions.

Technical stack:
- Minecraft Java Edition 1.21.11
- NeoForge 21.11.48, using the exact version selected by the generated project
- Java 21
- Gradle with ModDevGradle
- Windows 11 and VS Code
- Mod ID: `soulbound`
- Base Java package: `br.com.soulbound`

## Core Principles

1. Always inspect existing source code and Gradle configuration before editing.
2. Use NeoForge APIs compatible with Minecraft 1.21.11.
3. Do not use Fabric or legacy Forge APIs.
4. Do not change dependency versions without approval.
5. Preserve existing Gradle wrapper and project configuration.
6. Implement small, independently testable features.
7. Run `.\gradlew.bat build` after Java or resource changes.
8. Explain compilation failures and fix them before proceeding.
9. Never claim that a feature works without validating it.
10. Ask before deleting files, resetting Git history or changing dependencies.

## Architecture

Keep these responsibilities separate:

- registry: entity, item, block and other registrations
- entity: companion entity implementation
- entity.ai: navigation, goals and targeting
- companion: evolution, owner data and behavioral states
- item: companion egg and future command items
- ritual: evolution, resurrection and altar mechanics
- network: client-server communication
- client: renderers, models, animations and interfaces

Avoid unnecessary abstractions and duplicate state.

## Companion Design

Permanent evolution stages:
- BABY
- JUNIOR
- PLENO

Temporary transformation:
- GOD

Behavior modes:
- FOLLOW_PASSIVE
- STAY
- DEFEND_OWNER
- BERSERKER
- ATTACK_TARGET

The GOD transformation lasts 60 seconds and has a 15-minute cooldown. These mechanics belong to future phases.

A companion has one owner, identified by player UUID.

The server must be authoritative for ownership, state changes, combat and progression.

## Phase 1 — Baby MVP

Implement only:
- One Lupus companion entity
- A companion spawn egg
- Ownership assigned when the player spawns the companion
- FOLLOW_PASSIVE behavior
- STAY behavior
- Interaction to switch between Follow and Stay
- No hostile targeting or attacks
- Persistent owner UUID and behavior mode
- Safe multiplayer ownership validation
- Basic renderer using an existing vanilla-compatible model as a temporary placeholder

A companion must not attack mobs or players in Baby mode.

Do not implement evolution, combat, ore scanning, rituals or resurrection yet.

## Acceptance Criteria

1. The mod compiles successfully.
2. Minecraft starts with the mod loaded.
3. The egg spawns a Baby companion.
4. The spawning player becomes its owner.
5. The companion follows its owner.
6. The owner can toggle Follow and Stay.
7. Other players cannot control the companion.
8. State persists after saving and reopening the world.
9. Baby never initiates combat.
10. The implementation does not reference client-only classes on a dedicated server.

## Workflow

Before implementing:
- Inspect the relevant code.
- Explain the proposed changes briefly.
- Identify the APIs and registrations required.

After implementing:
- Run the Gradle build.
- Fix compilation failures.
- Summarize modified files.
- Provide manual test instructions.
- Report any untested behavior explicitly.

Use Brazilian Portuguese for explanations and English for Java class names, methods and identifiers.
