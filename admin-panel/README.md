# Anoy Loader — Admin Panel

Next.js control panel for license keys, lib/APK OTA, and maintenance.

## Secrets (Vercel Environment Variables)

Set these under **Vercel → Project → Settings → Environment Variables**
(Production + Preview). Do **not** commit real values.

| Variable | Client? | Purpose |
|---|---|---|
| `ADMIN_PIN` | **No** (server only) | Dashboard login PIN. Verified only by `POST /api/admin/auth`. |
| `ADMIN_SESSION_SECRET` | **No** (optional) | HMAC secret for session tokens. Falls back to a derivative of `ADMIN_PIN`. |
| `SUPABASE_SERVICE_ROLE_KEY` | **No** | Privileged server DB writes from API routes. |
| `NEXT_PUBLIC_SUPABASE_URL` | Yes | Supabase project URL. |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Yes | Supabase anon key (RLS applies). |

After changing env vars on Vercel, **redeploy** so serverless functions pick them up.

### Local development

```bash
cp .env.local.example .env.local
# edit .env.local with real values
npm install
npm run dev
```

`.env` / `.env.local` are gitignored.

## Auth flow

1. Browser submits PIN to `POST /api/admin/auth` (PIN never compared in client JS).
2. Server checks `process.env.ADMIN_PIN` with a constant-time compare.
3. On success, server returns a signed session token (12h TTL).
4. Token is stored in `sessionStorage` and sent as `Authorization: Bearer …`
   (and `x-admin-token`) on admin write APIs.
5. Protected write routes: `/api/admin/games` (POST), `/api/client/config` (POST),
   `/api/client/app-update` (POST).

There is **no default PIN** in production code. If `ADMIN_PIN` is missing,
login returns HTTP 503 with a setup hint.

## Deploy

Root of this folder is the Vercel project root (`admin-panel/`), or configure
the Vercel project **Root Directory** to `admin-panel`.
