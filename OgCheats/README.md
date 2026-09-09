# OG Cheats

Branded twin of **Anoy Loader**, built as the `ogcheats` product flavor of `:app`.

| | Anoy Loader (`anoy` flavor) | OG Cheats (`ogcheats` flavor) |
|---|---|---|
| applicationId | `com.ryzen` | `com.ogcheats` |
| Display name | Anoy Loader | OG Cheats |
| Telegram | @libAkAudioVisiual | @CrimeCell |
| Icon | `app/src/main/res/drawable/ic_launcher.png` | `OgCheats/src/main/res/drawable/ic_launcher.png` |
| Resources | `app/src/main/res` | + overrides in this folder |

## Build

```bash
./gradlew :app:assembleAnoyRelease :app:assembleOgcheatsRelease
```

APKs:
- `app/build/outputs/apk/anoy/release/`
- `app/build/outputs/apk/ogcheats/release/`
