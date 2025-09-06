# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/io/turtlemessenger`: Spring Boot app code (layers: `controller`, `service`, `repository`, `model`, `dto`, `config`, `util`).
- `src/main/resources`: configuration and assets (e.g., `application.properties`).
- `src/test/java/io/turtlemessenger`: JUnit 5 tests mirroring main package structure.
- Build files: `build.gradle.kts` (primary), `pom.xml` (optional Maven support).
- Artifacts: `build/libs/` (e.g., `TurtleMessenger-1.0.0.jar`).

## Build, Test, and Development Commands
- Build: `./gradlew clean build` (Windows: `gradlew.bat clean build`). Produces jar under `build/libs`.
- Run (dev): `./gradlew bootRun` to start the Spring Boot app.
- Run (jar): `java -jar build/libs/TurtleMessenger-1.0.0.jar`.
- Tests: `./gradlew test` (filter: `./gradlew test --tests "*Service*"`).
- Maven (alternative): `mvn clean package` and `mvn test` if you prefer Maven.

## Coding Style & Naming Conventions
- Language: Java 17, Spring Boot 3.x.
- Indentation: 4 spaces; one class per file.
- Packages: lowercase (e.g., `io.turtlemessenger.controller`).
- Classes: PascalCase (e.g., `MessageService`). Methods/fields: camelCase.
- Keep controllers thin; prefer `service` for business logic and `repository` for persistence.

## Testing Guidelines
- Frameworks: JUnit 5 with Spring Boot Test (`@SpringBootTest`, slices where applicable).
- Location: mirror package under `src/test/java`.
- Naming: `XxxTests` for classes; method names describe behavior.
- Run locally with `./gradlew test`. Aim to cover services and repository interactions; use lightweight context tests for controllers.

## Commit & Pull Request Guidelines
- Commits: clear, imperative subject (e.g., "Add message repository"). Group related changes.
- Conventional Commits are welcome (feat, fix, refactor, test), though not required.
- PRs: include purpose, summary of changes, how to test, and any linked issues. Add screenshots or sample requests if changing HTTP endpoints.

## Security & Configuration Tips
- Default DB: SQLite via `database.db` (override with `SPRING_DATASOURCE_URL`).
- Local overrides: use environment vars or `application.properties` in `src/main/resources`.
- Do not commit secrets; prefer environment-based configuration for credentials.

## Next Steps & Roadmap
- Persistence: add `ChatRoom` and `Message` entities with JPA, link to `UserAccount`; replace in-memory history; add repositories and service layer.
- History API: paginate `GET /api/rooms/{id}/messages` (`page`, `size`, `before`), and create `POST /api/rooms` to manage rooms/membership.
- WebSocket UX: typing indicators, delivery receipts, presence; use `/user/queue/**` for direct messages and `/topic/presence` for online status.
- Auth hardening: registration validation, password reset flow, JWT refresh, rate limiting for `/api/auth/**`, roles for admin endpoints.
- Frontend: rooms list UI, infinite scroll for history (React Query), message virtualization, error toasts, and form validation.
- Testing: unit tests for `AuthService`, security filter, and controllers; WebSocket integration tests; basic E2E for login + chat.
- DevOps: Dockerfile + docker-compose (backend + SQLite volume), GitHub Actions CI (build/test), and production CORS tightening.
- Production serving: build SPA to `frontend/dist` and serve from `src/main/resources/static`; version API under `/api/v1`.
- Migrations & scale: introduce Flyway/Liquibase for schema; consider RabbitMQ as external STOMP broker if load increases.

Note: Keep this section updated as a living checklist. Use commit messages like `docs(roadmap): update next steps` when refining.
