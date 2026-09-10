# UI Redesign Report — v1.2.9

## Summary

Full presentation redesign toward a calm, editorial, Claude-like system: warm neutrals, single terracotta accent, Source Serif 4 + Inter, monochrome outlined icons, tone-based surfaces, no gradients/glow/status pills/springs.

**Behavior, navigation, and feature scope unchanged.**

## Tokens

### Color (light / dark)

| Token | Light | Dark |
|-------|-------|------|
| background | #F5F4EF | #1F1E1D |
| surface | #FAF9F5 | #262523 |
| surfaceElevated | #FFFFFF | #2B2A27 |
| surfaceSubtle | #ECEAE3 | #33312E |
| outline | #DDD9CF | #3D3B37 |
| textPrimary | #1F1E1D | #F5F4EF |
| textSecondary | #6B6860 | #A8A49B |
| textTertiary | #9C988E | #77736B |
| accent | #C96442 | #D97757 |
| accentPressed | #B5573A | #E08A6E |
| onAccent | #FFFFFF | #1F1E1D |
| accentSubtle | #F3E4DC | #3A2C26 |
| error | #B3261E | #E5776A |
| success | #4F7A5B | #86B096 |
| warning | #9A6B1B | #D2A24C |

Defined in `app/src/main/java/com/ryzen/ui/theme/Color.kt` and mirrored in `res/values{,-night}/colors.xml`.

### Typography

- Display/headlines: Source Serif 4, weight 400
- UI: Inter 400/500/600
- Mono: JetBrains Mono
- Letter spacing 0
- File: `Type.kt`

### Shape / spacing / motion

- Radii: input 8, button 10, card 12, dialog 16, sheet 24 (`Shape.kt`)
- 8dp grid + 20dp screen horizontal (`Spacing.kt`)
- Motion: 150 / 250 / 300 ms, FastOutSlowIn only; springs removed (`Motion.kt`)

## Components

| Component | Notes |
|-----------|-------|
| AppButton | Flat primary accent; secondary outline; tertiary/destructive = text only |
| AppTextField | surfaceSubtle fill, 8dp radius, label above |
| AppCard | surfaceElevated, 12dp, 1dp outline, no shadow |
| AppTopBar | 56dp, surface, outlined back |
| AppBottomNav | Flat bar, outlined icons, accent when selected, no pill/shadow |
| AppDialog | 16dp, outline, no glow shadow |
| AppBadge | Plain semantic text + 2dp rule (not a capsule) |
| AppProgressBar | 4dp linear accent on surfaceSubtle |

## Screens migrated

1. Splash  
2. Login (+ dialogs)  
3. Main dashboard (Games)  
4. Settings (incl. Reset Guest)  
5. Crash  
6. Permissions / Game-not-installed dialogs  

## Removed / neutralized

- Ambient/hero/button/progress **gradients**
- Accent **glow** and dialog/nav **drop shadows**
- Spring / bounce / press-scale animation
- Colored status **pills** (badges → text + rule)
- Material **Rounded** icons → **Outlined**
- Dual competing purple-ember Compose palette
- XML theme solid system bars → transparent edge-to-edge

## Known issues

- Device/emulator screenshots not captured in this environment (no local SDK). Save pairs under `docs/ui-redesign/screenshots/` after install as `<screen>_light|dark.png`.
- Some UI copy still inline (not every string moved to `strings.xml`); structure ready for follow-up string pass.
- Legacy unused `res/layout/*` and loader/glow drawables remain on disk; safe to delete in a cleanup PR after Lint UnusedResources on CI.
- `pressScale` kept as no-op API stub for compatibility.

## How to add a new screen

1. Wrap root in `AppTheme` / `AppBackground`.
2. Use only `AppTheme.colors.*`, `AppTheme.typography.*`, `AppTheme.spacing.*`, `AppRadii.*`.
3. Prefer shared components: `AppButton`, `AppTextField`, `AppCard`, `AppTopBar`, `AppDialog`.
4. Status = `AppBadge` / plain semantic text — never filled chips.
5. One primary filled button max per screen.
6. Icons: `Icons.Outlined.*`, tint `textSecondary` (or `accent` only if selected nav).
7. Insets: `statusBarsPadding` / `navigationBarsPadding` / `imePadding` as needed.
8. Put user-facing copy in `strings.xml`.

## Version

- versionName **1.2.9** / versionCode **12**
