# App and website UI refresh

## Design

Shared slate surfaces and blue accents, Inter/system sans-serif headings, consistent spacing and rounded cards. The app continues to follow Android's light/dark system theme. The website adds a persisted light/dark switch, a flash-free theme initialization, opacity-compatible color tokens, and reduced-motion support.

## Website usability

- Public navigation, distinct account screens, password visibility controls, visible success/error messages, and a clear approval explanation.
- Responsive workspace navigation, license counts/search/empty states, named icon controls, and documentation links.
- Dialog keyboard focus containment, Escape dismissal, scroll locking, and focus restoration; stateful filters/switches and live toast announcements.
- API key creation and revocation use consistent feedback/confirmation rather than browser confirmation dialogs.

## App usability

- Guided setup based on actual host-install, virtual-copy, and OBB state. It does not claim unverified network/engine health.
- License countdown moved to its own card with explicit day/hour/minute/second units. Game actions display their existing next-step explanation.
- Larger minimum-height buttons and text field, separate named paste action, disabled empty-key submissions, and screen-reader labels.
- Standard Material navigation with selection indicators. Settings explains maintenance actions and confirms login/cache/guest cleanup.

## Validation

Website production build, TypeScript checks, existing auth/API/PostgreSQL tests and UI component tests. Browser smoke checks at 390px and 1440px: public pages and mocked-auth dashboard views have no horizontal overflow or JavaScript errors; password reveal, persisted theme, dialog focus, Escape, and focus restoration work.

Android strings parse as XML. Android compilation/device accessibility checks have not been run for this UI patch in the sandbox (no JDK/SDK). Before shipping an updated APK, use the Anoy-only GitHub Actions workflow and check a small-screen device with large font/display scaling, TalkBack, keyboard open, and both system themes. The existing game/runtime behavior and account/license authorization model are unchanged.
