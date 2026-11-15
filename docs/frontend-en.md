# Frontend guide (English)

The **FastSearch Persons** web interface is a React 18 / TypeScript / Vite /
Tailwind CSS single-page application. It is bilingual **French / Arabic** with
automatic RTL layout and talks only to the real API: **no demo mode, no mocks**.

> All data displayed by the demo project is **synthetic**. No real person is
> represented.

## Features

- Structured search (last name, first name, date, place) and free-text search.
- Bilingual **French / Arabic** interface with automatic RTL layout.
- Persisted language choice and `Noto Naskh Arabic` support.
- Result cards with score, confidence and field-level explanations.
- Server-side pagination with numbered navigation and page-size selection.
- Local confidence filter for the current page.
- JSON export of the displayed results.
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
cd frontend
npm ci
npm run dev
```

The application is available at `http://localhost:5173`. If the port is already
in use:

```bash
npm run dev -- --port 5174
```

> Vite binds to `localhost` / `::1`. Use `http://localhost:5174/` rather than
> `http://127.0.0.1:5174/` when the server only listens on IPv6.

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

## Configuration

The backend endpoint can be configured from the login screen and the settings
menu. An empty value uses the same-origin Vite proxy.

| Item | Storage | Key |
|---|---|---|
| Backend URL | `localStorage` | `fastsearch_config` |
| Language | `localStorage` | `fastsearch_language` |
| Authenticated session | `sessionStorage` | `fastsearch_session` |

Changing the backend URL clears the session so Basic credentials are never
reused against another server.

## Internationalization and RTL

- French and Arabic dictionaries live in `src/lib/i18n.ts`.
- Every visible string must be added to both dictionaries.
- The language is applied to `document.documentElement.lang` and `dir` (`ltr`
  or `rtl`).
- Tailwind logical utilities (`ms`, `me`, `ps`, `pe`, `start`, `end`) keep the
  layout RTL-safe.
- Directional icons use `rtl:rotate-180`.
- `Noto Naskh Arabic` is loaded for Arabic text.

## Pagination

- The backend returns a 0-based page; the UI displays it as 1-based.
- Page sizes: **10, 20, 50, 100**.
- Numbered navigation with ellipsis, previous/next buttons and a
  “Page X of Y” label.
- Global rank is preserved across pages (`11`, `12`, …).
- The pagination bar is visible whenever there are results, even on a single
  page.
- The confidence filter is reset on each new search.
- An out-of-range page is clamped by the server to the last available page.

`Pagination` relies on `src/lib/pagination.ts` (`buildPageEntries`) to compute
the page window. Unit tests cover bounds, ellipses and duplicate-free output.

## Components

| Component | Purpose |
|---|---|
| `LoginScreen` | HTTP Basic login and credential validation |
| `SearchForm` | Structured or free-text search |
| `ResultCard` | Score, confidence, language, group, dates, parents, explanations |
| `PersonDrawer` | Full candidate detail panel |
| `Pagination` | Navigation and page-size selection |
| `AdminPanel` | Index statistics, reindexing, synchronization |
| `AuditLogPanel` | Paginated audit log with hash, duration and outcome |
| `EvalPanel` | Metrics, categories, baselines, suggestions |
| `SettingsBar` | Backend URL, role, logout |
| `CandidateBanner` | “Candidate, not identification” reminder |
| `EmptyState` | Empty and no-result states |
| `LoadingError` | Loading and error states |

## Authentication and session

- HTTP Basic sent by the API client.
- Credentials kept in `sessionStorage` for the current tab only.
- No server-side session cookie.
- Logout clears the local session.
- Administration and evaluation screens are shown according to roles returned
  by `/api/v1/auth/me`, but the server remains the final authority.

## Accessibility and UX

- Arabic names displayed in automatic RTL.
- Scores out of 100 with a progress bar.
- Colored confidence badges (`FORT`, `MOYEN`, `FAIBLE`).
- Visible warnings when a result is unreliable.
- Keyboard navigation on result cards.
- Accessible drawer (`role="dialog"`, `aria-modal`).
- ARIA labels on pagination buttons.
- Responsive Tailwind layout.
- No real personal data displayed.

## Quality and CI

```bash
npm run typecheck
npm run lint
npm test
npm run build
```

- Vitest unit tests for i18n, display, confidence levels and pagination.
- `typecheck`, `lint`, `test` and `build` checks in GitHub Actions.
- Production dependency audit: `npm audit --omit=dev`.
- No mock data or demo mode in the API client.

**Verified state for version 0.1.0: 25 tests, 0 failures.**

## Docker and Nginx

The frontend can be served by Nginx with a same-origin proxy to the backend:

```bash
cd frontend
docker build -t fastsearch-persons-frontend .
docker run --rm -p 8081:80 \
  -e BACKEND_UPSTREAM=http://host.docker.internal:8080 \
  fastsearch-persons-frontend
```

The final image contains only the static build and Nginx. It adds security
headers, compression and SPA fallback. On Linux, add
`--add-host=host.docker.internal:host-gateway` when needed.

From the monorepo root, `docker compose up --build` starts PostgreSQL, the
backend and the frontend. See [Deployment](deployment.md).

## Structure

```text
frontend/
├── public/                 favicon and static assets
├── src/
│   ├── components/         screens and UI components
│   ├── lib/                API client, authentication, i18n, display helpers
│   ├── App.tsx             main application
│   ├── main.tsx            React entry point
│   └── types.ts            shared types
├── Dockerfile
├── nginx.conf
├── package.json
└── vite.config.ts
```

## Related documentation

- [Version française](frontend.md)
- [Architecture](architecture.md)
- [API reference](api.md)
- [Deployment](deployment.md)
- [Contributing](../CONTRIBUTING.md)
- [Security](../SECURITY.md)
