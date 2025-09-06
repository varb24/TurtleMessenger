# TurtleMessenger Frontend + WebSocket Report

## Overview
This project pairs a Spring Boot backend with a React (Vite + TypeScript) SPA for realtime chat. REST handles history; STOMP over WebSocket delivers live messages.

## Backend Flow
- Enable STOMP: `WebSocketConfig` registers `/ws` and configures `/app` (inbound) and `/topic` (outbound).
- Send path: FE publishes to `/app/rooms.{id}.send`.
- Controller: `ChatMessageController.send` receives a `MessageDTO`, stamps time, and broadcasts to `/topic/rooms.{id}` via `SimpMessagingTemplate`.
- REST: `ChatRestController` exposes `GET /api/rooms/{id}/messages` and `POST /api/rooms/{id}/messages` (simple in‑memory history to illustrate).

## Frontend Flow
- STOMP client connects to `ws://localhost:8080/ws` with auto‑reconnect.
- Subscribes to `/topic/rooms.1` and appends incoming messages to state.
- Sends messages by publishing to `/app/rooms.1.send` with `{ roomId, senderId, content, ts }`.
- Dev proxy in `vite.config.ts` forwards `'/api'` and `'/ws'` to `http://localhost:8080` for local development.

## Run Locally
- Backend: `./gradlew bootRun` (port 8080).
- Frontend: `cd frontend && npm i && npm run dev` (port 5173). Open `http://localhost:5173` in two tabs to see realtime updates.

## Concepts to Learn
- STOMP Destinations: application prefix (`/app`) routes to controllers; broker prefix (`/topic`) fans out to subscribers.
- Backpressure/ordering: simple broker is in‑memory; scale with an external broker if needed.
- CORS/Origins: allow FE origin during dev. For prod, serve `frontend/dist` from `src/main/resources/static` to use same origin.

## Next Steps
- Add auth (JWT) for REST and WS headers.
- Persist messages (JPA + SQLite) and page history.
- Rooms, typing indicators, and delivery receipts.
