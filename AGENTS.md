# Repository Guidelines

## Project Structure & Module Organization
The entry point lives in `src/main/java/org/newdawn/spaceinvaders/SpaceInvadersApp.java`, with feature-specific packages such as `app`, `gameplay`, `login`, `mainmenu`, `multyplay`, `room`, `server`, `shop`, and `database`. Shared assets sit under `src/main/resources` (sprites, backgrounds, fonts), while `DB.json` provides sample runtime data. Tests mirror the package layout in `src/test/java/org/newdawn/spaceinvaders`, and build artifacts land in `target/`. Use `run.sh` or `run.bat` for a scripted launch.

## Build, Test, and Development Commands
- `mvn clean package` — compiles, runs tests, and produces `target/space_invaders-1.0-SNAPSHOT.jar`.
- `mvn test` — executes the JUnit test suite without creating a JAR.
- `mvn exec:java -Dexec.mainClass=org.newdawn.spaceinvaders.SpaceInvadersApp` — runs the game straight from sources.
- `./run.sh` (or `run.bat` on Windows) — ensures the shaded JAR exists, then launches the game with packaged assets.

## Coding Style & Naming Conventions
Follow the existing Java style: four-space indentation, braces on the same line as declarations, and concise inline comments. Classes use `PascalCase`, fields and methods use `camelCase`, constants stay in `UPPER_SNAKE_CASE`. Keep packages under `org.newdawn.spaceinvaders` and group new code by feature module (e.g., place new lobby screens under `room`). Favor `Logger` or existing managers over `System.out` for gameplay feedback.

## Testing Guidelines
Tests rely on JUnit 4 (`org.junit.Test`). Name new files `*Test.java`, colocated with the code under test inside `src/test/java`. Cover gameplay state changes, room lifecycle, and multiplayer networking with deterministic scenarios; mock or stub network adapters where feasible. Run `mvn test` before pushing, and aim to include at least one positive and one failure-path assertion for each public entry point you touch.

## Commit & Pull Request Guidelines
The history follows Conventional Commit prefixes (`feat:`, `fix:`, `refactor:`). Keep messages in the imperative, reference issues (`feat: add lobby countdown (#42)`) when available, and limit body text to the rationale plus notable side effects. For pull requests, provide:
- A short summary of behavior changes and affected modules.
- Evidence of local testing (`mvn test`, manual gameplay checks).
- Screenshots or clips for UI-facing changes (menus, sprites).
- Call out configuration steps if `DB.json` or new resources need updating.

## Configuration Tips
Avoid committing real account data; `DB.json` is meant for mock credentials. If you add new assets, store them beneath `src/main/resources/sprites` (create subfolders as needed) and reference them via the resource classpath to keep distribution self-contained.*** End Patch
