# UI Redesign Audit — Anoy Loader / OG Cheats

**Date:** 2026-09-10  
**Scope:** Presentation only (Compose UI + residual XML). No business-logic changes in this audit.  
**Status:** Part A complete — **awaiting your reply `go` before any migration.**

---

## 1. Stack detection

| Area | Finding |
|------|---------|
| **Primary UI** | **Jetpack Compose** (BOM `2024.02.00`) + **Material 3** |
| **Host activities** | `AppCompatActivity` + `setContent { AppTheme { … } }` |
| **XML Views** | **Legacy only.** Layouts under `res/layout/` (`activity_splash`, `activity_login`, `activity_main`, `dialog_start_options`, `ios_loading`) are **not referenced** by current Kotlin (`setContentView` / `R.layout.*` unused in app UI path). |
| **Theme (XML)** | `Theme.Material3.DayNight.NoActionBar` as `Base.AppTheme` (`values` + `values-night`). Status/nav bar colors solid `@color/bg_dark` in XML (Compose overrides to transparent). |
| **Theme (Compose)** | Custom `AppTheme` wrapping M3 `MaterialTheme` + `LocalExtendedColors` / `LocalSpacing`. |
| **Dynamic color** | **Not used** (good). No `dynamicDarkColorScheme` / Material You. |
| **Design tokens (Compose)** | Present but **out of spec**: purple-tinted neutrals, saturated ember, violet secondary, glow, ambient gradients (`Color.kt`, `Theme.kt`, `Motion.kt`, `Shape.kt`, `Spacing.kt`, `Type.kt`). |
| **Design tokens (XML)** | Closer to Claude warm palette in `values/colors.xml` / `values-night/colors.xml`, but **diverged from Compose tokens** — two competing systems. |
| **Edge-to-edge** | `enableEdgeToEdge()` on Splash / LogAct / MAct / CrashActivity. Compose sets transparent system bars. **No `WindowInsets.safeDrawing`** usage. |
| **Flavors** | `anoy` / `ogcheats` — branding via resources; same Compose UI. |

### Libraries (UI-related)

| Library | Role |
|---------|------|
| `androidx.compose.material3:material3` | Theme / components base |
| `androidx.compose.material:material-icons-extended` | **Icons.Rounded.*** everywhere |
| `androidx.activity:activity-compose` | `setContent`, edge-to-edge |
| `androidx.appcompat:appcompat` | Activity host |
| Fonts in `res/font/` | Source Serif 4, Inter, JetBrains Mono (already bundled; scale/weights wrong vs brief) |

---

## 2. Screens / routes inventory

### Activities (navigation graph)

| Route | Class | Composable | Description |
|-------|-------|------------|-------------|
| Launcher | `SplashActivity` | `SplashScreen` | Boot, security checks, config/update/maintenance gates → login |
| Auth | `LogAct` | `LoginScreen` | License key entry, auth, permissions, maintenance/announcement dialogs → main |
| Main shell | `MAct` | `MainDashboardScreen` **or** `SettingsScreen` via `Crossfade` + `AppBottomNav` | Games tab + Settings tab; launch/install/OBB flows |
| Crash | `CrashActivity` (`:crash`) | `CrashScreen` | Fatal report, copy log, restart |

### Composable screens

| Screen | File | One-line description |
|--------|------|----------------------|
| Splash | `ui/screens/SplashScreen.kt` | Brand mark, version badge, status spinner, update + maintenance dialogs |
| Login | `ui/screens/LoginScreen.kt` | Key field, save toggle, authenticate / get key, loading/error/maintenance/announcement/permissions overlays |
| Dashboard (Games) | `ui/screens/MainDashboardScreen.kt` | Top bar + expiry chip, game tabs, version cards with status badge + primary action, options dialog, not-installed dialog |
| Settings | `ui/screens/SettingsScreen.kt` | License card, sandbox tools (clear login / reset guest BGMI-only / clear resources), Telegram CTA |
| Crash | `ui/screens/CrashScreen.kt` | Error summary + stack scroll, copy / restart |

### Shared components (`ui/components/`)

| Component | Role | Spec risk |
|-----------|------|-----------|
| `AppButton` | Primary / secondary / tertiary / destructive | Gradient fills, error as fill, press scale spring |
| `AppCard` | Bordered surface | Optional tonal elevation / shadow path |
| `AppTextField` | Custom basic field | Mostly aligned; hardcoded heights |
| `AppTopBar` | Title + optional back/trailing | OK structure; divider always on |
| `AppBottomNav` | 2-tab floating pill dock | Shadow, accent pill fill, springs, swipe |
| `AppDialog` | Modal shell | Shadow + accent glow handle |
| `AppBadge` | Status / category pills | **Colored capsule** — Part C violation |
| `AppProgressBar` | Linear progress | Accent **gradient** fill |
| `AppCountdownTimer` | Expiry digits | Present; limited use |
| `PermissionsDialog` | Permission checklist | Custom layout, not AppDialog actions pattern |
| `GameNotInstalledDialog` | Play Store prompt | Custom |

### Dead / residual UI assets

- `res/layout/*.xml` — unused by Compose path  
- Many `drawable/loader_*`, `bg_button_gradient`, `*_pill`, `loader_glow` — legacy XML era  
- Duplicate XML color system vs Compose “Obsidian + Ember”

---

## 3. Hardcoded colors, dimensions, fonts, strings

### 3.1 Colors (`Color(0x…)` / raw hex)

**Compose token file (all must be rewritten to Part B.1):**

| Location | Examples |
|----------|----------|
| `ui/theme/Color.kt:15–77` | Full palette: purple greys (`#12111A`…), saturated ember (`#E85D3B`, `#FF7A55`), **violet** (`#7B5CFF`), bright success/warning/error/info, glow alphas |
| `ui/theme/Color.kt:117–140` | `ambientBrush` / `accentGradient` / `heroGlowBrush` — **gradients** |
| `ui/theme/Theme.kt:27–64` | Inline `Color(0xFF1A0F0C)`, `Color.White` on schemes |
| `ui/components/AppButton.kt` | `Color.White`, `Color.Transparent` for on-accent / gradient host |
| Screens | `Color.Transparent` for unselected tabs; Login checkbox `Color.White` |

**XML (must converge to same tokens; remove unused):**

| File | Notes |
|------|-------|
| `res/values/colors.xml` | Claude-like warm set (`#F5F4EF`, `#C96442`) — **closer to brief** but not full token list |
| `res/values-night/colors.xml` | Warm charcoal dark |
| `res/values/themes.xml` | Hardcoded `#FFFFFF`, `#F5F4EF` splash items |
| `res/values-night/themes.xml` | Hardcoded `#1F1E1D` |
| Drawables | Multiple solid/gradient XML colors (`bg_button_gradient`, `loader_glow`, pills) |

**Delta vs Part B.1 (Compose live palette):** wrong neutrals (cool purple vs warm stone), accent too hot/saturated, extra violet + info + filled semantic tints, glow tokens.

### 3.2 Dimensions (hardcoded `.dp` / `.sp` outside pure token defs)

**~200+** literal `N.dp` / `N.sp` hits under `ui/` (components + screens). Samples:

| File | Examples |
|------|----------|
| `AppButton.kt` | `42.dp` / `50.dp` height, `20.dp` pad, `18.dp` spinner (spec: 48dp button, 18dp icon in button OK if tokenized) |
| `AppBottomNav.kt` | `56.dp` dock, `12.dp` shadow, `24.dp` outer pad |
| `AppTextField.kt` | `52.dp` height (OK target), `14.dp` horizontal pad |
| `AppBadge.kt` | `9.dp` / `4.dp` pad, `6.dp` dot, `13.dp` icon |
| `SplashScreen.kt` | `88.dp` logo, `220.dp` glow, `11.sp` footer |
| `MainDashboardScreen.kt` | `42.dp` tabs, `52.dp` game icon, `11.sp` section labels |
| `SettingsScreen.kt` | `40.dp` tabs, `11.sp` / `12.sp` / `14.sp` overrides |
| `Shape.kt` | Radii `8/12/16/22/28` — **not** brief `10/8/12/24/16` |
| `Spacing.kt` | Has 4–48 grid + `screenHorizontal=20` (good base); screens still bypass with raw dp |

### 3.3 Typography / fonts

| Item | Current | Spec |
|------|---------|------|
| Families | Source Serif 4, Inter, JetBrains Mono in `res/font/` | Same families (keep) |
| Display weight | Often **SemiBold/Bold** | Display/headlines **400** |
| `titleLarge` | **Serif** SemiBold 18 | **Inter 600** 20/28 |
| Letter spacing | Negative on display; **0.1–0.5** on labels/mono | **0 everywhere** |
| Scale | displayLarge 32/38, etc. | displayLarge **36/44**, headlineLarge **28/36**, … |
| Runtime overrides | Widespread `.copy(fontSize = 11.sp, fontWeight = Bold, letterSpacing = 0.2.sp)` | Forbidden drift |

### 3.4 User-facing strings hardcoded in UI

`strings.xml` has ~40 brand/action keys, but **majority of UI copy is inline**:

| Area | Approx. `text = "…"` count |
|------|----------------------------|
| SplashScreen | 9 |
| LoginScreen | 13 |
| SettingsScreen | 17 |
| MainDashboardScreen | 5 (+ many dynamic action labels) |
| CrashScreen | 6 |
| PermissionsDialog | 7 |
| GameNotInstalledDialog | 4 |
| Toasts in `MAct` / activities | Many English strings outside `strings.xml` |

Examples of inline copy that already exist as (or should be) resources: `"Secure Sandbox Authentication"`, `"Authenticate"`, `"License key"`, `"Target application"`, `"Crash Report"`, dialog titles, etc.

---

## 4. Part C prohibition violations

| Prohibition | Status | Evidence |
|-------------|--------|----------|
| **Emojis** | Clean in UI strings | No emoji glyphs found in Compose UI. Bullet `•` in footers (Splash/Settings) — replace with plain text or middot via strings. Mask `••••` OK. |
| **Gradients** | **Heavy** | `Color.ambientBrush`, `accentGradient`, `heroGlowBrush`; Splash/Login radial glow; `AppButton` horizontal gradient; `AppProgressBar` gradient; `drawable/bg_button_gradient.xml`, `loader_glow.xml` |
| **Neon / glow / saturated** | **Yes** | `accentGlow`, violet secondary, bright `#FF7A55` / `#3FCB88` / `#FF6B6B`; dialog/nav **shadow with accentGlow** |
| **Colored text pills** | **Yes — core pattern** | `AppBadge` filled tint + border + colored text (Online, Latest, Fatal, Lifetime, version chip). Expiry chip on dashboard uses **successTint fill**. Nav active pill = **accentTint fill** |
| **Uppercase / tracking** | Partial | Server tags uppercased in model (`LATEST`); labels use positive letterSpacing; no ALL_CAPS buttons observed |
| **>1 accent-filled control / screen** | **Yes** | Login: Authenticate primary + accent paste icon + Online badge. Settings: Reset Guest primary + Contact Admin primary (+ destructive filled). Maintenance dialogs: primary + destructive fill |
| **Filled / mixed icons** | **Yes** | Entire set is **`Icons.Rounded`** (filled rounded), not Outlined/Symbols grade-0. Status icons tinted accent/error/warning/success |
| **Material purple / dynamic color** | Partial | No dynamic color. Compose neutrals are **purple-grey** (not M3 default purple, still wrong). XML theme closer to terracotta |
| **Drop shadows** | **Yes** | `AppBottomNav.shadow(12.dp)`, `AppDialog.shadow(16.dp)`; cards may use tonal elevation |
| **Bouncy / spring / decorative motion** | **Yes** | `AppMotion.SpringSnappy/Soft`, `pressScale` spring, bottom-nav spring pill, splash scale entrance, icon scale on nav |
| **Hardcoded colors/dimens/fonts/strings in UI** | **Yes** | See §3 |
| **Placeholder / coming soon / TODO in UI** | Partial | `"Coming soon"` as badge + disabled CTA is **product state** (server-driven), not lorem — restyle as plain status text, keep behavior |
| **Semantic color as fills** | **Yes** | error/warning/success **Container** backgrounds on icon wells; destructive **button fill**; badge tints |

---

## 5. Insets / edge-to-edge / keyboard

| Surface | Handling | Issues |
|---------|----------|--------|
| Theme | Transparent status + nav; light/dark icon contrast | XML theme still paints solid bar colors (flash risk before Compose) |
| Splash | `statusBarsPadding` + `navigationBarsPadding` | No `ime` (N/A). No `safeDrawing` |
| Login | `statusBars` + `navigationBars` + **`imePadding`** on root | Good baseline; glow can draw under status without safeDrawing |
| MAct shell | `AppBackground` full bleed; dashboard/settings rely on `AppTopBar.statusBarsPadding`; bottom nav `navigationBarsPadding` | Content can sit under gesture bar if scroll padding insufficient; **no `safeDrawing`** |
| Crash | padding via screen | OK-ish; verify cutout |
| Dialogs | Platform default width false + horizontal pad | Keyboard over dialog fields not specially handled (login field is behind dialog rarely) |
| Font scale 1.3 | Untested | Labels with fixed `11.sp` / fixed heights (`40.dp` tabs, `50.dp` buttons) likely clip |

---

## 6. Fonts, icons, UI libraries (current)

### Fonts (`res/font/`)

- `source_serif_4_{regular,semibold,bold}.ttf`  
- `inter_{regular,medium,semibold,bold}.ttf`  
- `jetbrains_mono_{regular,medium,semibold}.ttf`  
- Orphans: `bold.ttf`, `font.ttf` (legacy — audit for deletion)

### Icons

- **Only** Material Icons **Rounded** via `material-icons-extended`  
- Custom drawables: launcher, telegram, game art (`india`, `ic_pubg_gl`), paste/visibility XML leftovers  
- Launcher icon: **leave unchanged** (per brief)

### Motion library

- Compose animation only (no Lottie). Springs + decorative scale must go.

---

## 7. Component-level gaps vs Part B.6

| Spec component | Current | Work |
|----------------|---------|------|
| Primary button | Gradient, ~50dp, radius sm 12 | Flat accent, 48dp, 10dp radius, onAccent token, no spring scale |
| Secondary | Outline OK-ish | Match 1dp outline + textPrimary |
| Text | Tertiary exists | Confirm no container |
| Destructive | **Filled error gradient** | Text button, **error text only** |
| Text field | Custom 52dp | surfaceSubtle fill, 8dp radius, label above — retoken |
| Top bar | Exists | surface bg, titleLarge Inter, scroll divider optional |
| Bottom nav | Floating shadowed pill, 2 tabs | Flat bar, transparent indicator, outlined icons, accent only when selected |
| Lists | Card-heavy dashboard | Prefer list rows unless rich game row keeps one card |
| Cards | Outline + optional elevation | surfaceElevated, 12dp, 1dp outline, **no shadow** |
| Chips | Missing as filter chips; badges misuse chip role | Add filter chip; kill status capsules |
| Status | AppBadge | Plain semantic text ± 2dp left rule |
| Dialogs | Centered card + glow shadow | 16dp radius, text actions right-aligned |
| Snackbars | System Toast only | Optional later; restyle if custom added |
| Empty states | Implicit (“Not Installed” as primary CTA) | Serif headline + one sentence + one button |
| Progress | Gradient bar / accent circular | 4dp linear or 20dp mono circular |
| Tabs | Filled segment control | Text + 2dp accent underline |

---

## 8. Proposed migration order

Per brief: foundation → components → screens by frequency.

### Phase 0 — Prep (no visual ship required)

1. Freeze behavior; note dialogs/toasts that stay API-identical.  
2. Inventory drawable/layout deletion candidates (legacy XML UI).  
3. Expand `strings.xml` catalog (do not rewrite copy until screen pass).

### Phase 1 — Design system foundation

1. Rewrite `Color.kt` to **exact** Part B.1 tokens (light/dark); remove violet, glow, info fills, ambient brushes.  
2. Align XML `colors.xml` / night / themes to the same names; transparent system bars in XML too.  
3. Rewrite `Type.kt` scale + letterSpacing 0; map titleLarge → Inter 600; headlines serif 400.  
4. Rewrite `Shape.kt` / `Spacing.kt` / `Motion.kt` (150/250/300, FastOutSlowIn only; **delete springs**, pressScale bounce, entrance rise).  
5. `AppTheme`: flat background (no gradient canvas); ripple = textPrimary 8%; disable any dynamic color leftover.  
6. Optional: introduce `ui/designsystem/` package and deprecate ad-hoc paths.

### Phase 2 — Shared components

Order:

1. `AppButton` (primary/secondary/text/destructive-as-text)  
2. `AppTextField`  
3. `AppTopBar`  
4. `AppCard`  
5. Replace `AppBadge` → `AppStatusText` (or delete)  
6. `AppDialog` + migrate Permissions / GameNotInstalled to same shell  
7. `AppBottomNav` (flat, outlined icons)  
8. `AppProgressBar`  
9. Filter `AppChip` if needed  
10. Icon pass: **Outlined** (or Material Symbols Outlined), monochrome rules  

### Phase 3 — Screens (user frequency)

1. **Splash** (`SplashActivity` / `SplashScreen`) — first paint  
2. **Login** (`LogAct` / `LoginScreen`) — including nested dialogs  
3. **Main shell + Dashboard** (`MAct` + `MainDashboardScreen`)  
4. **Settings** (`SettingsScreen` + Reset Guest confirm)  
5. **Crash** (`CrashActivity` / `CrashScreen`)  
6. Empty / error polish pass (not-installed, maintenance, permissions, no-key)  
7. Toast copy → strings; optional snackbar component  

### Phase 4 — Cleanup & gates

1. Delete unused layouts, loader/glow/gradient drawables, orphan fonts.  
2. Grep gates: no `Brush.`, no `Color(0x` outside token file, no `Icons.Rounded` / Filled, no `.shadow(` on cards/nav, no spring motion.  
3. Lint UnusedResources; font_scale 1.3; light/dark screenshots → `docs/ui-redesign/screenshots/`.  
4. `REPORT.md` with before/after.

---

## 9. Risk notes (behavior-preserving)

- **Reset Guest**, clear login/resources, install/launch/OBB, auth, permissions: **callbacks stay**; only chrome changes.  
- **“Coming soon” / version tags**: keep branching; change presentation from pill → text status.  
- **Bottom nav is 2 destinations** (Games, Settings): stays 2; visual only.  
- **Dual brand** (Anoy / OG): strings/icons from resources; design system must not hardcode brand name colors.  
- **No emulator in this environment for screenshots** until CI/device path agreed; document in REPORT if capture is deferred.

---

## 10. Stop gate

**No code has been changed for this redesign.**  

Audit artifact: `docs/ui-redesign/AUDIT.md`  

**Reply `go` to start Phase 1 (foundation tokens + theme).**  
If you want a different screen order or to keep any current pattern (e.g. floating dock), say so before `go`.
