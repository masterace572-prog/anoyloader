# Anoy Control

Public introduction site, email/password accounts, approved-admin dashboard, and scoped license-management API. The Android app continues to use its existing **license-key login**, not the site's administrator credentials.

## Deploy / upgrade (required)

1. Back up your database. Apply your existing backend setup first if this is a new installation.
2. Run `../supabase/migration_account_auth.sql` in the Supabase SQL Editor **after all older setup scripts**. It adds account approval and API-key tables, removes permissive anonymous table/storage writes, hides license records, and revokes legacy management RPC access. Existing app configuration reads and `verify_and_activate_key` remain available. Do not rerun old setup scripts afterward: they recreate insecure policies. The migration is repeatable.
3. Configure these environment variables on the Next.js server:

   | Variable | Visibility | Purpose |
   |---|---|---|
   | `NEXT_PUBLIC_SUPABASE_URL` | Public | Supabase project URL |
   | `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Public | Supabase public/anon key (protected by RLS) |
   | `SUPABASE_SERVICE_ROLE_KEY` | **Server only** | Authorized database operations; no anon fallback |

   `ADMIN_PIN` and `ADMIN_SESSION_SECRET` are no longer used; old sessions stop working.
4. Enable email/password signups and email confirmation in Supabase Auth. Set the **Site URL** to your deployed site and allow its `/dashboard` redirect URL (including a trusted preview host if needed). Configure production SMTP and Auth rate limits/CAPTCHA as appropriate. Use HTTPS.
5. Deploy with `admin-panel/` as the project root. Create and confirm your first account at `/signup`.
6. Approve the account using trusted SQL Editor access, replacing the email:

   ```sql
   INSERT INTO public.admin_accounts (user_id, is_approved)
   SELECT id, true FROM auth.users WHERE lower(email) = lower('YOUR_ADMIN_EMAIL')
   ON CONFLICT (user_id) DO UPDATE SET is_approved = true;
   ```

   Signup alone grants **no** management access. Approval is not read from user-editable profile metadata. Approvals are currently managed through SQL, not a dashboard user-management screen.
7. Open `/dashboard` → **API access** to create credentials. Store the returned secret immediately; it is shown only once.

To revoke an account and its API access, set its `admin_accounts.is_approved` to `false`. Revoke individual credentials in **API access**. All approved admins share the existing license catalog; each admin can list/revoke only their own API credentials. Never distribute the service-role key.

## Pages / API

- `/`: public introduction
- `/login`, `/signup`: account authentication via Supabase Auth
- `/dashboard`: approved-admin key, release, maintenance, and API access tools
- `/docs`: public usage documentation and curl examples
- `/api/v1/licenses`: GET list; POST `create`, `bulk`, `extend`, `reset_hwid`, `toggle_ban`, `delete`
- `/api/admin/api-keys`: session-only GET/POST and DELETE `?id=…` (revocation)

API credentials use `Authorization: Bearer anoy_api_…`. Read operations require `licenses:read`; writes require `licenses:write`. API keys expire after 1–365 days, are stored as SHA-256 hashes of 256-bit random secrets, and cannot create other API keys or access system/update settings. Usage counts authenticated, approved requests, including invalid operation inputs; this is not billing.

## Local development / checks

```bash
cp .env.local.example .env.local
# Configure your own development Supabase project; never commit secrets.
npm ci
npm run dev -- --hostname 0.0.0.0
npm run typecheck
npm test
npm run build
npm run start -- --hostname 0.0.0.0
```

The public pages render without Supabase credentials; private operations correctly fail closed. Account signup/login and live data require configured, reachable Supabase services. Tests use a mocked Supabase HTTP boundary plus real PostgreSQL permissions/DDL in PGlite; they do not contact your production project.

## Operational limits

- Configure deployment/WAF rate limits before public automation. Application-level throttling is not implemented.
- License list is unpaginated and subject to the Supabase project's configured row limit; large catalogs need pagination before scaling.
- Create/bulk/extend/toggle are not idempotent; do not blindly retry writes. Concurrent extension/toggle requests should be serialized by the caller in this version.
- Browser sessions use Supabase's standard persisted access/refresh lifecycle. Use a strict deployment CSP and avoid untrusted scripts. Credentials never enter URLs.
- Storage upload policies are intentionally closed. Current UI uses hosted download URLs; any future upload API must enforce approval server-side.
- Next.js updated to 15.5.27; PostCSS/source-map overrides address transitive audit findings. System font stacks keep builds independent of external font downloads.

### Verification in this workspace

- Production build and TypeScript check pass; 18 API/auth/SQL permission tests pass.
- `npm audit --omit=dev` reports no vulnerabilities with the lockfile in this change.
- Full `npm audit` still reports findings in the existing Tailwind 3 build-time dependency chain (notably `braces`, with no patched release available). A Tailwind major-version migration is outside this change; do not process untrusted build inputs and reassess before upgrading build dependencies.
- Live Supabase email delivery/login and Android compilation/gameplay were not exercised here. See `../docs/STABILITY.md` for the device verification checklist.
