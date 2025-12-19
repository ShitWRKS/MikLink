# UI architecture

This project follows a Clean Architecture boundary: UI depends on domain and ports, never on data implementations.

## Goals

- Keep screens resilient to refactors in repositories/DB/remote.
- Minimize UI churn when domain evolves.
- Make ViewModels unit-testable without Android framework.

## Structure

Recommended packages:

- `ui/navigation/` — routes and navigation helpers
- `ui/feature/<feature>/` — feature-first slices
  - `XxxScreen.kt`
  - `XxxViewModel.kt`
  - `XxxUiState.kt`
  - `XxxEvent.kt` (optional)
  - `components/` (feature-scoped composables)

Shared:
- `ui/components/` for truly global composables (buttons, dialogs, etc.)
- `ui/common/` for shared state primitives (UiState, Result mapping, formatters)

### Current vs Target
- **Target:** feature-first slices under `ui/feature/<feature>/` with co-located screen/VM/state/components.
- **Current:** legacy structure still uses folders like `ui/test/*`, `ui/client/*`, `ui/dashboard/*`; treated as in-migration while keeping boundaries (no data imports, use cases/ports only).

## Rules

1) **No data-layer imports**
- UI must not import `com.app.miklink.data.*` (Room, Retrofit DTOs, iText).
- UI should talk only to **use cases** and/or **ports** (`core/data/*`).

2) **Prefer use cases over repositories**
- Repositories are persistence/network ports.
- UI should depend on use cases for business flows:
  - `GeneratePdfUseCase`
  - `ExportBackupUseCase`
  - `SaveTestReportUseCase`
  - etc.

3) **UI models are allowed**
- If domain models churn frequently, map them to UI models in VM.
- This reduces UI breakage when domain refactors.

4) **Navigation must be typed**
- Avoid raw strings scattered across screens.
- Keep a single source of truth:
  - `sealed interface Route` with `pattern` and `build(...)`
  - encode/decode helpers for arguments (URI encoding for `socketName`)

5) **State is a single stream**
- Each VM exposes:
  - `val uiState: StateFlow<UiState>`
  - plus small derived flows when necessary.
- Avoid multiple mutable flows that can desync.

6) **One-off events**
- Use `SharedFlow` (or Channel) for one-off UI events:
  - snackbars
  - navigation triggers

## Testing

- ViewModel tests in `app/src/test` using:
  - `kotlinx-coroutines-test`
  - fakes for ports/use cases
- UI instrumented tests in `app/src/androidTest` for navigation & rendering.

### Test execution rendering (attuato)
- Renderer unico (`ui/feature/test_details/*`) alimentato da `TestRunSnapshot` tipizzati.
- Live e History riusano lo stesso renderer; vietato il parsing di mappe stringa o chiavi libere.
- I report v1 vengono mappati a `TestRunSnapshot` per mantenere la compatibilità.

## Legacy cleanup policy

Any UI that is not reachable from the navigation graph is considered **legacy** and must be removed (screen + VM + routes + tests).
