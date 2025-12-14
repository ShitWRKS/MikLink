# ADR-0001 — SOLID Canone A

- **Status:** Accepted
- **Data:** 2025-12-13

## Contesto

Serve un perimetro chiaro per evitare drift tra UI, domain e data layer mentre si completa l'epic SOLID Canone A. Le regole devono impedire leak di tecnologie (Android, Room, Retrofit, Moshi, iText) nei layer core e normalizzare i modelli condivisi (report, backup, PDF/IO).

## Decisione

- Canone A (layering):
  - `core/domain/**` rimane puro: nessuna dipendenza da Android SDK, Room, Retrofit/OkHttp, Moshi, iText.
  - `core/data/**` contiene solo contratti e tipi neutrali; nessun adapter o infrastruttura.
  - `data/**` ospita tutte le implementazioni tecnologiche (Room, Retrofit/Moshi, OkHttp, iText, SAF/ContentResolver, DataStore, ecc.).
  - `ui/**` non importa `data/**`; dialoga solo con `core/domain` (use case) e `core/data` (ports) esposti via use case.
  - I use case possono dipendere dai port definiti in `core/data/**`, mai dalle impl in `data/**`.
- ReportData unificato: modello normale di report in `core/domain/model/report/ReportData` (riusato da UI, storia, PDF, backup/export) con codec dedicato.
- PDF/IO neutro: `core/data/pdf/PdfGenerator.generate(...)` restituisce `ByteArray`; l'IO avviene tramite `DocumentWriter.writeBytes(dest: DocumentDestination, ...)` dove `DocumentDestination` incapsula solo `uriString`. SAF rimane confinato all'impl Android in `data/**`.
- Sicurezza HTTPS: la policy "trust-all" vive solo in `data/remote` (vedi ADR-0002); i port restano neutri.

## Conseguenze

- Qualsiasi dipendenza Android/Moshi/Room/iText nei layer core va rimossa o spostata in `data/**`.
- Le nuove feature devono usare `ReportData` e i codec/port dedicati; niente DTO/ParsedResults in UI/domain.
- Nuovi adattatori infrastrutturali richiedono port in `core/data/**` e binding DI in `data/**`.
- Le review devono verificare le regole di import per evitare regressioni di layering.
