# Changelog

All notable changes to this project will be documented in this file.

Format: Keep a top header for the unreleased section, then list versions in reverse chronological order.

## [Unreleased]

## [v1.0.0] - 2026-05-27
- Added: Efficient bitwise move counting methods `countPossibleMoves` and `countPossibleMovesForUnicorn` in `EscampeBoard`.
- Changed: Made opening libraries public in `Opening.java` for integration.
- Changed: Configured `build.gradle` and `mainClass` files to point to `io.ClientJeu` and `game.EscampeAIPlayer`.
- Fixed: Swapped incorrect sign in unicorn escapability formula in `Heuristic.java`.
- Fixed: Eliminated redundant board scan in evaluation loop.

<!--
Template example:
## [vX.Y.Z] - 2026-05-12
- Added: feature
- Changed: something
- Fixed: bug
-->
