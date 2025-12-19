# UI theme, fonts, and status tokens

Scopo: documentare font, semantic status colors e soft glow della UI MikLink, includendo dove vivere nel codice e come usarli senza hardcode.

## Palette v1.1 (gray accent, single source of truth)
- Neutrali (dark-first): background `#0E0E10`, surface1 `#15181D`, surface2 `#1C2128`, outline `#2A313A`, textHigh `#E9EDF2`, textMedium `#B7C0CB`, textDisabled `#7B8794`.
- Primary dark: `#707372`, primaryContainer `#4B4F56`, onPrimary `#F4F4F4` (RAL 9003 insp.), onPrimaryContainer `#E9EDF2`.
- Primary light: `#4B4F56`, primaryContainer `#D1D0CE` (Cool Gray 2 C insp.), onPrimary `#F4F4F4`, onPrimaryContainer `#0E0E10`.
- Semantic test states: success `#2F6F4E`, successContainer `#173A2A`, successGlow `#44DE95`; failure `#B14A4A`, failureContainer `#3A1B1B`, failureGlow `#FF6B6B`; running = primary per scheme (containers as sopra), runningGlow `#D1D0CE`.
- Light neutrali: `#F4F6F8/#FFFFFF/#E4E9F0`, outline `#CBD3DE`, textHigh `#0E0E10`.

## Font
- Base UI: **Manrope** (Regular/Medium/SemiBold) in `app/src/main/res/font/manrope_*.ttf`, referenziati in `app/src/main/java/com/app/miklink/ui/theme/Type.kt` e applicati come `MaterialTheme.typography`.
- Monospace tecnico: **JetBrains Mono** (Regular/Medium) in `app/src/main/res/font/jetbrains_mono_*.ttf`, esposti in `Type.kt` via `JetBrainsMono`, `MonoBody`, `MonoLabel`. Usare per log, valori tecnici, ping samples (es. `RawLogsPane`, `PingSectionRenderer`).
- Licenze: Manrope (SIL OFL 1.1) e JetBrains Mono (Apache 2.0). Binari da repo ufficiali; mantenere i file sotto `res/font` e documentare se si aggiungono pesi.

## Semantic status colors (PASS/FAIL/RUNNING)
- Definiti in `app/src/main/java/com/app/miklink/ui/theme/Color.kt` e esposti via `MikLinkThemeTokens.semantic` in `Theme.kt`:
  - `success`, `successContainer`, `onSuccess`, `onSuccessContainer`, `successGlow`
  - `failure`, `failureContainer`, `onFailure`, `onFailureContainer`, `failureGlow`
  - `running`, `runningContainer`, `onRunning`, `onRunningContainer`, `runningGlow`
- Uso:
  - Esiti test in execution/history/result cards/report hero. Badge/chip: background = `*Container`, testo/icon = `on*Container`.
  - `colorScheme.error*` resta per errori/validation generici, non per FAIL test.
  - Evitare blu/violet/cyan e corsivo nelle UI copy.

## Soft glow
- Implementazione: `Modifier.softGlow(...)` in `app/src/main/java/com/app/miklink/ui/theme/GlowModifiers.kt` (radial gradient + breathing alpha opzionale).
- Default: raggio ~28–32dp, `maxAlpha` ~0.18–0.24, `breathe=true`.
- Uso: hero/indicatori principali di stato (execution/report). Non usarlo su liste/chip ripetuti o warning generici.

## Checklist anti-regressione
- Ripgrep:
  - `rg "Color\\.Red|Color\\.Green|0xFF4CAF50|0xFFF44336|0xFF2E7D32"`
  - `rg "errorContainer" app/src/main/java/com/app/miklink/ui` (non deve servire per FAIL test)
  - `rg "FontStyle\\.Italic"` (non usare corsivo)
  - `rg "FontFamily\\.Monospace|monospace"` (solo in `Type.kt` helpers)
  - `rg "#015EA4|#012D4E|#4FA2DB|violet|purple|cyan"` (evitare reintroduzione blu/violet)
- Verifica manuale light/dark:
  - PASS/FAIL/RUNNING con palette v1 + glow soft.
  - History filter/badge coerenti (niente neon).
  - Settings slider Material3 standard.
  - Log/valori tecnici in JetBrains Mono.
