# Repository Guidelines

Space Invaders is a Java 8 + Maven game with Firebase login and an optional multiplayer server. Use this reference to stay aligned.

## Project Structure & Module Organization
- `src/main/java/org/newdawn/spaceinvaders` holds all modules: `app` (screens), `gameplay` (loop, entities), `login/database` (Firebase), `multyplay` & `room` (matchmaking), `server` (GameServer) and `shop`.
- Runtime assets live in `src/main/resources/{sprites,fonts}`; keep filenames lowercase-hyphenated.
- Tests mirror production packages under `src/test/java/org/newdawn/spaceinvaders` (e.g., `GameTest.java`).
- Helper scripts `run.sh`, `run.bat`, and `run-server.sh` provide the supported launch paths.

## Build, Test, and Development Commands
- `./run.sh` – launch the desktop client using the packaged LWJGL natives for a fast smoke test.
- `mvn exec:java -Dexec.mainClass=org.newdawn.spaceinvaders.SpaceInvadersApp` – run directly from sources while debugging.
- `mvn clean package` – build the fat JAR in `target/space_invaders-1.0-SNAPSHOT.jar`; add `-DskipTests` only after a clean `mvn test`.
- `./run-server.sh 9000` or `java -cp target/space_invaders-1.0-SNAPSHOT.jar org.newdawn.spaceinvaders.server.GameServer 9000` – start the multiplayer relay on a chosen port.
- `mvn test` – execute the full test suite before pushing; narrow to one class with `-Dtest=RoomManagerTest` if needed.

## Coding Style & Naming Conventions
- Use 4 spaces, same-line braces, and import blocks ordered by package depth.
- Packages stay lowercase; classes/interfaces PascalCase, methods and fields camelCase, constants UPPER_SNAKE_CASE.
- Keep UI code in `app`/`mainmenu`, gameplay rules in `gameplay`/`shop`, and networking in `multyplay`, `room`, or `server`; shared helpers should live in dedicated utility classes.

## Testing Guidelines
- Add JUnit tests beside the relevant package path and name them `*Test`.
- Prioritize coverage for collision logic, leaderboard persistence, matchmaking flows, and Firebase exception handling.
- Run `mvn test` before packaging, and document any manual checks (screens, logs, videos) when automation is not practical.

## Commit & Pull Request Guidelines
- Follow the `<type>: <summary>` pattern seen in history (`feat:`, `docs:`, `refactor:`) or the occasional `[Edit]`; keep subjects imperative and concise.
- Reference issues or tasks in the commit body and briefly describe gameplay or networking impact.
- PRs should list affected modules, commands/tests executed, and include screenshots or updated `docs/` assets for UI or protocol changes.

## Security & Configuration Notes
- `database/FirebaseConfig.java` ships placeholder keys; supply real credentials via environment variables or encrypted config files and never commit secrets.
- Avoid logging tokens or passwords when touching `UserManager`, networking, or server diagnostics.
- `GameServer` defaults to port 7777; coordinate alternate ports for local multiplayer and ensure firewalls open the selected port before distributing builds.
