# DISCREPANCIES (Docs/ADR vs Codice)

Questo file contiene **solo evidenze** (path/line).  
Niente soluzioni, niente proposte.

---

## D-001 — `probeId` ancora presente (hard rule = 0 occorrenze)

- `app/src/main/java/com/app/miklink/core/domain/test/model/TestPlan.kt:7`
  - contiene la stringa `probeId` (anche se solo in commento)

---

## D-002 — HTTPS trust-all applicato anche quando si usa HTTP

Vincolo: trust-all **solo** quando `isHttps = true` (ADR-0002).

Evidenza:

- `app/src/main/java/com/app/miklink/di/NetworkModule.kt:86-91`
  - `sslSocketFactory(...)` + `hostnameVerifier { _, _ -> true }` applicati **sempre** nel client di default

---

## D-003 — Violazioni Canone A: implementazioni tecnologiche in `core/data/**`

Vincolo: `core/data/**` = solo ports/contratti (no Retrofit/Room/iText/Android).

Stato: parzialmente risolto.

- Retrofit/Moshi in core/data (remote): **RISOLTO (EPIC-0003)** — implementazioni spostate in `app/src/main/java/com/app/miklink/data/remote/mikrotik/**`.
- iText + Android in core/data (pdf impl):
  - `app/src/main/java/com/app/miklink/core/data/pdf/impl/PdfGeneratorIText.kt:3,15-21`
    - import `android.content.Context`
    - import `com.itextpdf.*`

---

## D-004 — Logs ancora presenti (fuori scope)

Stato: **RISOLTO** (EPIC-0001.2).

Evidenze precedenti rimosse:

- UI: toggle + pannello "raw logs" (TestExecutionScreen)
- Domain: evento log (TestEvent.LogLine)
- String resources dedicate ai log
