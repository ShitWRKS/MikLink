# ADR-0011: Typed test execution contract and unified renderer

- **Status:** Accepted
- **Date:** 2025-12-18

## Contesto

Lo stream di esecuzione test usava chiavi stringa (`Map<String, String>`, `TestSectionResult`, messaggi liberi) e renderer differenti per Live e History. Il parsing fragile di stringhe ("Target …", "Ping #") portava a regressioni e a UI divergenti, mentre i report v1 devono restare leggibili.

## Decisione

- Introdurre modelli tipizzati di snapshot (`TestRunSnapshot`, `TestSectionId`, `TestSectionPayload`, `TestProgressKey`) come unico contratto engine→UI.
- Emettere `TestEvent.SnapshotUpdated(TestRunSnapshot)` dal runner e mappare i report (resultFormatVersion=1) allo stesso snapshot per History/PDF.
- Mantenere compatibilità v1 dei report con estensioni additive al payload; nessuna rottura del decoder.
- Un solo renderer "test_details" viene riusato da Live e History; vietato il parsing di chiavi stringa.
- Trasporto unificato: i repository MikroTik usano solo `MikroTikCallExecutor` con `CallOutcome` che conserva errori https+http.

## Conseguenze

- UI e History condividono un renderer tipizzato, riducendo il drift.
- I test devono fallire se riemergono `Map<String,String>` nel contratto (scan guardrail).
- I report v1 restano leggibili; eventuali nuovi campi devono essere opzionali con default.
- Nuove feature sui test vanno modellate estendendo i payload tipizzati, non aggiungendo chiavi di testo.
