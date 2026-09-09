# OG Cheats

Branded twin of **Anoy Loader** in this monorepo.

| | Anoy Loader (`:app`) | OG Cheats (`:OgCheats`) |
|---|---|---|
| applicationId | `com.ryzen` | `com.ogcheats` |
| Display name | Anoy Loader | OG Cheats |
| Telegram | @libAkAudioVisiual | @CrimeCell |
| Icon | `app/.../ic_launcher.png` | `OgCheats/.../ic_launcher.png` (from `Helper/`) |
| Sources | `app/src/main` | shared `app/src/main` + local res overrides |

Build:

```bash
./gradlew :app:assembleRelease :OgCheats:assembleRelease
```
