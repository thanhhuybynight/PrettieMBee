# Bug audit — 2026-09-13

This document separates reproducible defects from architectural observations found during the repository audit.

## Confirmed defects fixed on `fix`

| Area | Confirmed failure | Resolution / regression evidence |
|---|---|---|
| Reset | A successful root reset left the persisted “applied theme” status visible. | Clear applied metadata only after reset succeeds; ViewModel-owned operation survives Activity recreation. |
| Refresh | Losing root/app availability could leave stale installed themes on screen; refresh exceptions could escape. | Clear target-derived state immediately and contain non-cancellation failures. |
| Target ownership | Activity and repository both mutated the package; package and selected UUID were lost on recreation. | Immutable package-scoped `RootRepository`, one ViewModel owner, and per-package persisted target preferences. |
| Lifecycle | Activity-owned coroutines could be cancelled during a configuration change while downloading or injecting. | Move orchestration and operation state to `MainViewModel.viewModelScope`. |
| Auto launch | Disabling the deeplink option still launched the app's main Activity. | Do not invoke launch at all when the option is disabled; launch now reports failure. |
| Root result handling | Force-stop, preparation, ownership, permissions, SELinux restore and reset could report success after command failure. | Validate command results and postconditions; fail closed on unsafe/missing data directories and UID/GID. |
| Root input safety | Configurable package names and UUIDs entered shell commands and target paths without strict validation. | Strict package/UUID validation before repository construction or root work; only allow known package data-directory forms. |
| Source layout | A directory with any file under `images/` could be treated as downloaded even though injection requires PNG files. | Require a real lowercase `.png` plus `theme/token.json`; import/download tests cover invalid layouts. |
| Download/import cleanup | Failed and cancelled ZIP operations could leave temporary files or partial theme directories. | Cancellable copy/extraction and `finally` cleanup, covered by failure and cancellation tests. |
| Replacement safety | A failed re-download deleted the previously working local theme. | Validate in a staging directory, publish only after success, retain the old copy on failure. |
| Catalog cache | Malformed remote JSON could overwrite a valid cache; `forceRefresh` had no effect. | Parse before cache write; normal reads prefer validated cache and forced refresh attempts network with fallback. |
| HTTP redirects | Redirect handling missed relative URLs/statuses and lacked a reliable bound/cleanup path. | Resolve relative redirects, support 301/302/303/307/308, reject downgrade and cap redirect count. |
| Theme IDs/deletion | Remote/custom IDs could escape the themes directory; deletion failure was hidden. | Validate IDs, enforce canonical containment/symlink checks and return `Result`. |
| Background images | Selecting an unreadable replacement deleted the prior background and still showed success. | Copy to a unique file first, persist/swap only on success, retain previous state and report failure to UI. |
| Rendering | `FlashScreen` rendered the same global wallpaper/dim layer a second time. | Keep wallpaper ownership in `PrettieMBeeTheme` only. |
| Store catalog | Two aliases share one MB Store UUID and produced duplicate target rows/keys. | Preserve source data but deduplicate by UUID at the repository read boundary. |

## Not defects / deferred design observations

- `MainActivity` previously had too many responsibilities. This was a maintainability risk, not independently a runtime bug; extracting `MainViewModel` was justified by the confirmed lifecycle/state defects above.
- `ThemeConfig` still combines appearance, pin and applied-theme preferences. This is design debt, but splitting it without a feature need would be a broad migration rather than a bug fix.
- Dependency-update and icon/style lint notices are warnings, not proof of broken behavior. This patch deliberately avoids an unrelated Android/Compose migration.

## Verification boundary

Robolectric tests exercise catalog, redirect, ZIP, cancellation, path validation, persistence, and transactional background behavior. Android lint and debug APK compilation run locally and in CI. End-to-end root injection still requires a rooted Android device with a compatible MB Bank installation; no such device was attached during this audit.
