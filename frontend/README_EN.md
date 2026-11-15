# FastSearch Persons — Frontend

Web interface for **FastSearch Persons**, an on-premise person matching engine.
The application never performs automatic identification: it displays
**candidates** with a score, a confidence level and a field-by-field
explanation.

**Version 0.1.0 — first public release.** See the [changelog](CHANGELOG.md).

> All data displayed by the demo project is **synthetic**. No real person is
> represented.

## Features

- Structured search (last name, first name, date, place) and free-text search.
- Bilingual **French / Arabic** interface with automatic RTL layout.
- Persisted language choice and `Noto Naskh Arabic` support.
- Result cards with score, confidence and field-level explanations.
- Server-side pagination with numbered navigation and page-size selection.
- Detailed candidate panel.
- Index administration: statistics, reindexing and synchronization.
- Audit log and synthetic evaluation panel.
- HTTP Basic authentication and backend role handling.
- Real API client: no demo mode or frontend mock data.

## Requirements

- Node.js **20.19+** or **22.12+**
- npm
- FastSearch Persons backend running at `http://localhost:8080`

## Getting started

```bash
npm install
npm run dev
```

The application is available at `http://localhost:5173`.

Vite proxies `/api` and `/actuator` to the backend. To use another server:

```bash
FASTSEARCH_BACKEND_URL=http://localhost:18080 npm run dev
```

PowerShell:

```powershell
$env:FASTSEARCH_BACKEND_URL = "http://localhost:18080"
npm run dev
```

## Scripts

| Command | Purpose |
|---|---|
| `npm run dev` | Vite development server |
| `npm run build` | Production build in `dist/` |
| `npm run preview` | Preview the production build |
| `npm run typecheck` | TypeScript check |
| `npm run lint` | ESLint analysis |
| `npm test` | Vitest unit tests |
| `npm run test:watch` | Tests in watch mode |

## Quality and CI

- Unit tests for i18n, display labels and confidence levels.
- `typecheck`, `lint`, `test` and `build` checks in GitHub Actions.
- Production dependency audit: `npm audit --omit=dev`.
- No mock data or demo mode in the API client.

## Docker

The frontend can be served by Nginx with a same-origin proxy to the backend:

```bash
docker build -t fastsearch-persons-frontend .
docker run --rm -p 8080:80 \
  -e BACKEND_UPSTREAM=http://host.docker.internal:8080 \
  fastsearch-persons-frontend
```

The final image contains only the static build and Nginx. It adds security
headers, compression and SPA fallback. On Linux, add
`--add-host=host.docker.internal:host-gateway` when needed.

## Configuration

The backend endpoint can be configured from the login screen and the settings
menu. An empty value uses the same-origin Vite proxy.

The selected language is stored in `localStorage` under
`fastsearch_language`. It is applied to `document.documentElement.lang` and
`dir` (`ltr` or `rtl`).

## Structure

```text
src/
├── components/       screens and UI components
├── lib/              API client, authentication, i18n, display helpers
├── App.tsx           main application
├── main.tsx          React entry point
└── types.ts          shared types
```

## Related documentation

- [README.md](README.md) — version française.
- [CHANGELOG.md](CHANGELOG.md) — release history.
- [SECURITY.md](SECURITY.md) — security policy.
- [CONTRIBUTING.md](CONTRIBUTING.md) — contribution guide.
- [LICENSE](LICENSE) — MIT license.

## License

Released under the **MIT** license. See [LICENSE](LICENSE).

Copyright (c) 2026 FastSearch Persons contributors.
