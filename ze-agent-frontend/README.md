# ZE AI Agent Frontend

Vue3 + Vite frontend for the ZE AI Agent Spring Boot backend.

## Pages

- `/`: application switcher
- `/love`: AI 恋爱大师聊天室
- `/manus`: AI 超级智能体聊天室

## API

The dev server proxies `/api` to `http://localhost:8080`.

- `GET /api/ai/Love_app/Chat/sse?message=...&chatId=...`
- `GET /api/ai/manus/chat?message=...`

## Run

```bash
npm install
npm run dev
```

Open the URL printed by Vite, usually `http://localhost:5173`.
