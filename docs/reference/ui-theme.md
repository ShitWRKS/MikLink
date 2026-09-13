# UI theme, fonts, and status tokens

Scopo: documentare font, semantic status colors e soft glow della UI MikLink, includendo dove vivere nel codice e come usarli senza hardcode.

## Palette v1.1 (gray accent, single source of truth)
- Neutrali (dark-first): background `#0E0E10`, surface1 `#15181D`, surface2 `#1C2128`, outline `#2A313A`, textHigh `#E9EDF2`, textMedium `#B7C0CB`, textDisabled `#7B8794`.
- Primary dark: `#707372`, primaryContainer `#4B4F56`, onPrimary `#F4F4F4` (RAL 9003 insp.), onPrimaryContainer `#E9EDF2`.
- Primary light: `#4B4F56`, primaryContainer `#D1D0CE` (Cool Gray 2 C insp.), onPrimary `#F4F4F4`, onPrimaryContainer `#0E0E10`.
- Semantic test states: success `#2F6F4E`, successContainer `#173A2A`, successGlow `#44DE95`; failure `#B14A4A`, failureContainer `#3A1B1B`, failureGlow `#FF6B6B`; running = primary per scheme (containers as sopra), runningGlow `#D1D0CE`.
- Light neutrali: `#F4F6F8/#FFFFFF/#E4E9F0`, outline `#CBD3DE`, textHigh `#0E0E10`.

## Font
- Base UI: **Manrope** variable (pesi consumati: Regular 400, Medium 500, SemiBold 600, Bold 700) in `app/src/main/res/font/manrope_variable.ttf`. `UiFontFamily` imposta esplicitamente l'asse `wght` per ogni peso e alimenta l'intera scala `MaterialTheme.typography`: titoli, sezioni, body, label, azioni, form, dialog e navigazione restano proporzionali.
- Monospace tecnico: **JetBrains Mono** (Regular/Medium) in `app/src/main/res/font/jetbrains_mono_*.ttf`. `TechnicalFontFamily` è usata soltanto dagli stili semantici `MonoBody` e `MonoLabel`, per log/output raw e campioni diagnostici nei quali il monospace migliora scansione e allineamento (per esempio `RawLogsPane` e `PingSectionRenderer`). Valori di controllo compatti come intervalli e percentuali nelle impostazioni restano normale testo UI.
- Regola anti-regressione: non assegnare `TechnicalFontFamily`/JetBrains Mono alla normale scala `MaterialTheme.typography` e non aggiungere override `fontFamily` nelle schermate. Un nuovo uso monospace deve rappresentare un dato tecnico e consumare `MonoBody` o `MonoLabel`.
- Font e licenze: Manrope proviene dalla [distribuzione Google Fonts](https://github.com/google/fonts/tree/main/ofl/manrope); JetBrains Mono dal [repository ufficiale JetBrains](https://github.com/JetBrains/JetBrainsMono). Entrambi sono redistribuiti sotto SIL Open Font License 1.1; copyright e testo della licenza sono in `THIRD_PARTY_NOTICES.md`.
- Valutazione JetBrains Sans (2026-08-11): le [linee guida ufficiali JetBrains](https://www.jetbrains.com/company/brand/) ne attestano il nome, ma non espongono un pacchetto font Android/TTF né una licenza specifica che autorizzi il bundling e la redistribuzione. I [termini ufficiali del sito](https://www.jetbrains.com/legal/docs/company/useterms/) non concedono la redistribuzione dei contenuti del sito. Non essendo dimostrabile la redistribuibilità da una fonte ufficiale JetBrains, JetBrains Sans non è inclusa e non va estratta o convertita dagli asset web.

## Execution logs
- Execution logs are displayed using JetBrains Mono for readability.
- Logs are sanitized through `LogSanitizer` (redaction of credentials/tokens and truncation of long lines).
- Logs are kept in a bounded in-memory buffer (oldest entries trimmed) and are not persisted to DB/disk (pre‑prod).
- A `Show logs` toggle is visible during running tests and after completion, exposing **sanitized** logs only.

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
  - `rg "JetBrainsMono|FontFamily\\.(Monospace|Mono)|fontFamily\\s*=|MonoBody|MonoLabel" app/src/main/java/com/app/miklink/ui` (solo configurazione centrale e helper tecnici motivati)
  - `rg "#015EA4|#012D4E|#4FA2DB|violet|purple|cyan"` (evitare reintroduzione blu/violet)
- Verifica manuale light/dark:
  - PASS/FAIL/RUNNING con palette v1 + glow soft.
  - History filter/badge coerenti (niente neon).
  - Settings slider Material3 standard.
  - Log/valori tecnici in JetBrains Mono.
