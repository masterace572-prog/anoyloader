# UI Redesign Report — v1.3.1

## Summary

Production polish of the warm, editorial design system introduced in 1.2.9:
flat surfaces, single terracotta accent, Source Serif 4 + Inter, monochrome
outlined icons, calm motion only. Every screen and shared component re-tuned
for spacing consistency, string resources, and bottom-nav clearance.

**Behavior, navigation destinations, auth, install/launch/OBB, and Reset Guest
are unchanged.**

## Design system

| Layer | Spec |
|-------|------|
| Color | Warm stone neutrals + terracotta accent (light/dark) — `Color.kt` |
| Type | Serif display 400 · Inter UI · Mono code · letterSpacing 0 — `Type.kt` |
| Shape | input 8 · button 10 · card 12 · dialog 16 — `Shape.kt` |
| Space | 8dp grid, 20dp screen horizontal — `Spacing.kt` |
| Motion | 150/250/300 ms FastOutSlowIn only — no springs/scale bounce — `Motion.kt` |

### Hard rules enforced

- No gradients / glow / drop shadows on chrome
- No colored status capsules (status = text + 2dp rule via `AppBadge`)
- Outlined icons only
- One primary filled button emphasis per surface
- No `Color(0x…)` outside token files
- No decorative scale entrance on splash

## Components

| Component | Notes |
|-----------|-------|
| `AppButton` | Flat primary; secondary outline; tertiary/destructive text-only + loading |
| `AppTextField` | surfaceSubtle, 8dp radius, focus border accent |
| `AppCard` | surfaceElevated, 12dp, 1dp outline, no elevation |
| `AppTopBar` | 56dp surface + outline divider |
| `AppBottomNav` | Flat bar, outlined icons, accent when selected |
| `AppDialog` | 16dp radius, outline, left-aligned title |
| `AppBadge` | Semantic text + optional left rule |
| `AppProgressBar` | 4dp linear accent |
| `SectionHeader` | Shared section label |
| `GameNotInstalledDialog` | Uses `AppDialog` shell |

## Screens

1. **Splash** — fade-only entrance, brand + status, update/maintenance dialogs  
2. **Login** — centered auth card, validation error, loading/error/maintenance/announcement/permissions  
3. **Dashboard** — target app card(s), expiry chip, options dialog, progress  
4. **Settings** — license, sandbox tools (clear login / reset guest / clear resources), Telegram  
5. **Crash** — summary + mono stack + copy/restart  

Bottom content padding (72dp) keeps lists clear of the flat bottom nav.

## Version

- versionName **1.3.1** / versionCode **14**
