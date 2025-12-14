# MikLink — Context Pack (handoff)

> Scopo: consentire a un nuovo LLM/agent di riprendere il refactor **senza inventare nulla**, mantenendo il canone SOLID/layering, riducendo drift e arrivando a una versione **minimamente funzionante** da portare su `develop`.

---

## 0) Regole operative anti‑drift (obbligatorie)

1. **Non assumere nulla.** Qualsiasi affermazione sullo stato del codice deve essere verificata nel repo attuale (`test-refactor`) con grep, lettura file o build/test.
2. **Se trovi discrepanze tra doc/epic e codice, STOP e chiedi prima.**  
   - Non “riparare” a intuito.
   - Apri un breve report: cosa hai trovato, dove, e quali alternative proponi.
3. **Non introdurre debito tecnico.** Niente “quick fix” che violino il canone (import, dipendenze, layering).
4. **Una epic alla volta.** Ogni epic deve terminare con un “pavimento stabile” (build/test verdi o motivazione esplicita concordata).
5. **After every response**, verifica di poter ancora leggere/ricordare questo Context Pack; se inizi a perdere contesto, fermati e chiedi di aprire una nuova sessione.

---

## 1) Obiettivo del progetto

MikLink è un’app Android (Kotlin/Compose) per eseguire test su dispositivi MikroTik via REST e generare report (anche PDF).  
Il refactor è orientato a:
- Architettura SOLID/layered stabile, manutenibile, “a prova di agent”.
- Eliminazione legacy (multi‑probe, campi obsoleti, placeholder).
- Suite test anti‑regressione come bussola.
- Versione “minimamente funzionante” da portare su `develop` senza debiti tecnici.

**Vincolo importante:** il progetto è in sviluppo; **si possono fare cambi distruttivi** (nessuna compatibilità richiesta, nessuna installazione attiva da preservare).

---

## 2) Stato Git / flusso lavoro

- Branch di lavoro: **`test-refactor`**.
- EPIC‑0001 è stata avviata/eseguita su `test-refactor`.
- Target: portare su **`develop`** una versione almeno minimamente funzionante e pulita.

**Policy consigliata:**
- 1 commit per epic (o piccoli commit coerenti).
- Prima di merge in `develop`: build/test verdi + checklist architetturale.

---

## 3) Canone definitivo (A) + regole import “senza ambiguità”

### Struttura
- `core/domain/**` = modelli + regole + use case
- `core/data/**` = **solo** ports/contratti (repository/provider/gateway) + tipi neutrali se indispensabili
- `data/**` = implementazioni (Room/Retrofit/OkHttp/Moshi/iText) + mapper/parser + repository impl
- `ui/**` = Compose + ViewModel + UiState + mapper domain→ui
- `di/**` = wiring/binding, **zero logica**
- `feature/**` = non canonico, da eliminare se presente/inutile
- `domain/**` top-level = non canonico, da migrare in `core/domain/**`

### Regole import (chiuse)
- `core/domain/model/**` **NON** importa `core/data/**` né Android/Room/Retrofit/OkHttp/Moshi/iText/UI
- `core/domain/usecase/**` **PUÒ** importare `core/data/**` (solo ports) + `core/domain/model/**`
- `core/data/**` **NON** importa `data/**` `ui/**` `di/**` né infra concreta
- `data/**` può importare `core/data/**` + `core/domain/**` (mapper/impl)
- `ui/**` può importare `core/domain/**` (+ opzionale `core/data/**` solo se serve) **ma NON** `data/**` concreti
- `di/**` può importare tutto, ma **niente logica**

---

## 4) Decisioni “chiuse” (non negoziabili senza nuova ADR)

### 4.1 Single probe (sonda unica)
- L’app gestisce **una sola sonda** in tutta l’app.
- Qualsiasi uso di `probeId` (route, model, repository, db, backup) è **legacy** e va rimosso.

> Nota tecnica: se si usa Room, una PK interna può esistere per necessità tecnica, ma **non deve mai esistere** nel domain/UI e non deve chiamarsi `probeId` né esporre concetto di “multi‑probe”.

### 4.2 HTTP/HTTPS toggle + trust-all consapevole
- L’utente sceglie HTTP o HTTPS.
- In HTTPS si deve poter comunicare **senza verifica certificato** (trust-all consapevole).
- Implementazione consigliata: factory con client “safe” per HTTP e “unsafe” per HTTPS.

### 4.3 Report: `resultsJson` unico
- `Report.resultsJson` resta **una sola colonna JSON**.  
- Analytics/splitting schema rimandati ad una epic futura.

### 4.4 Socket ID — versione LITE
Target “lite” per il refactor:
- Socket ID = `prefix + counter + suffix` (con separatore/padding se previsto)
- Stato contatore salvato su **Client** (opzione A)
- Incremento contatore **solo** su test SUCCESS
- Niente JSON template avanzato in lite (estensioni future possibili)

### 4.5 Logs
- Logs fuori scope: rimuovere UI/feature/logiche logs.

### 4.6 Backup import/export
- Backup **rimane** in scope.
- Deve essere coerente con single-probe: niente `List<ProbeConfig>`, ma singola `ProbeConfig` (o nullabile gestita).

### 4.7 DB rebase (previsto in EPIC‑0002)
- DB può essere ricreato da zero.  
- Obiettivo: eliminare naming v1/v2, migrazioni legacy, e fissare una baseline pulita.

---

## 5) Policy test / build

- Durante una epic è tollerato che alcuni test falliscano temporaneamente.
- **Alla fine di ogni epic:**  
  - minimo: `./gradlew test` deve essere **verde**  
  - se l’epic tocca Room/UI: valutare `connectedAndroidTest` (se disponibile in CI/local)
- Se un test fallisce per cambio intenzionale: **STOP e chiedi**; non “aggiustare” alla cieca.

---

## 6) Cosa fare all’inizio (checklist oggettiva)

Eseguire su `test-refactor`:

1. Identità repo/commit:
   - `git rev-parse --abbrev-ref HEAD`
   - `git rev-parse HEAD`
   - `git status`
2. Stato single-probe:
   - `git grep -n "probeId" app/src/main app/src/test`
   - `git grep -n "getAllProbes|getProbe\(" app/src/main app/src/test`
3. Stato layering:
   - `git grep -n "core\.data\.local\.room" app/src/main/java/com/app/miklink/ui`
   - `git grep -n "android\." app/src/main/java/com/app/miklink/core/domain`
4. Build/test:
   - `./gradlew test` (salvare output)
5. Report iniziale:
   - creare un breve “Refactor Status Report” con:
     - residui `probeId` (path/linea)
     - violazioni canone (path/linea)
     - test falliti (nome test + errore sintetico)

> Solo dopo questo report è consentito iniziare nuove modifiche.

---

## 7) Roadmap refactor (alto livello, da validare col codice reale)

- EPIC‑0001: Single probe end‑to‑end + Backup single probe (**già avviata su test-refactor**)
- EPIC‑0002: DB rebase distruttivo baseline (Room version=1, nome `miklink`, rimozione migrations/v1/v2)
- EPIC‑0003: UI non importa Room/DAO/Entity; UI parla via use case/ports
- EPIC‑0004: Risultati canonici in domain; PDF e UI non dipendono da DTO/UI model
- EPIC‑0005: HTTPS toggle con trust-all solo quando HTTPS
- EPIC‑0006: Socket ID LITE + incremento su SUCCESS
- EPIC‑0007: Pulizia residuale (feature/**, domain top-level, placeholders) + quality gates

> Nota: l’ordine può cambiare solo se il report iniziale dimostra dipendenze/compilazione bloccanti.

---

## 8) Prompt “starter” per nuova sessione LLM (incollare come primo messaggio)

**Istruzioni per LLM/agent:**

- Leggi questo Context Pack.
- Non inventare né assumere nulla.
- Prima di cambiare codice: produci un “Refactor Status Report” basato su grep e build/test.
- Lavora per epic: 1 epic alla volta, con gate finali.
- Se un file/linea non coincide con quanto atteso o trovi discrepanze: STOP e fai domande.
- Mantieni il canone A (struttura + regole import).
- Target finale: branch `develop` con build/test verdi e zero debiti tecnici.

---

## 9) Nota: contesto umano
Il committente non è uno sviluppatore full-time ma può definire chiaramente comportamento desiderato.  
Il lavoro deve essere guidato da evidenze e da decisioni chiuse (ADR), non da “intuizioni” dell’agent.
