# MikLink - Indice Documentazione

**Ultima Revisione**: 2025-11-15  
**Versione App**: 2.0

---

## 📐 Architettura e Design

### [ARCHITECTURE.md](ARCHITECTURE.md)
**Documentazione tecnica completa dell'architettura MikLink**

- Diagramma architetturale (Presentation → Domain → Data Layer)
- Schema Database Room v8 con tutte le entità
- Descrizione dettagliata di `Client`, `ProbeConfig`, `TestProfile`, `Report`
- Business rules e constraint per ogni entità
- Pattern MVVM + Repository

**Target**: Sviluppatori senior, architetti software

---

### [UX_UI_SPEC.md](UX_UI_SPEC.md)
**Design System completo (stile Ubiquiti)**

- Palette colori (primari, semantici, light/dark theme)
- Typography scale (Material 3)
- Design principles: chiarezza, coerenza, feedback
- Target audience: tecnici certificatori, installatori, manager IT

**Target**: Designer, sviluppatori frontend

---

## 🔬 Testing e Validazione

### [API_VALIDATION.md](API_VALIDATION.md)
**Checklist operativa per validazione API REST MikroTik**

- Pre-requisiti hardware/software
- Script PowerShell/Bash per test manuale API
- Endpoint da testare con expected response JSON
- Allineamento DTO Retrofit con sintassi RouterOS

**Target**: QA Engineer, sviluppatori backend

**Uso**: Eseguire prima di ogni release per validare integrazione API MikroTik

---

### [testing/](testing/)
**Storico report di testing e validazione**

#### [testing/API_TESTING_2025-11-15.md](testing/API_TESTING_2025-11-15.md)
- Report finale testing API REST con dispositivo MikroTik (IP 192.168.0.251)
- Riepilogo 6 fix critici (conversione POST→GET, parametri mancanti, timeout)
- Tabella completa 16 endpoint con status test
- Modifiche ai file: `MikroTikApiService.kt`, DTO aggiornati

#### [testing/VALIDATION_REPORT_v2.0.md](testing/VALIDATION_REPORT_v2.0.md)
- Report validazione post-implementazione refactor v2.0
- Executive summary: 6 problemi rilevati e risolti
- Fix critici: Migrazione DB v7→v8, route `probe_list` deprecata
- Build status: ✅ PRONTO

---

## 📦 Changelog e Implementazione

### [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
**Riepilogo completo modifiche refactor v2.0**

- Checklist modifiche per layer: Database, Repository, ViewModel, UI
- File modificati con snippet codice
- Cleanup file obsoleti
- Warning e azioni post-implementazione

**Target**: Sviluppatori, tech lead

**Uso**: Reference per rollback, debugging, onboarding su refactor v2.0

---

## 🗂️ Archivio

### [archive/](archive/)
**Documentazione storica e piani superati**

#### [archive/MASTER_PLAN_v2.0.md](archive/MASTER_PLAN_v2.0.md)
- Piano originale refactor v2.0 (sonda unica, parametri test, PDF HTML)
- Roadmap feature con specifiche modifiche DB/UI/ViewModel
- **Status**: Parzialmente obsoleto (molte feature già implementate)
- **Uso**: Reference storico decisioni architetturali

#### [archive/DUPLICATES_CLEANUP.md](archive/DUPLICATES_CLEANUP.md)
- Inventory duplicati e file obsoleti da eliminare
- **Status**: Parzialmente obsoleto (cleanup già eseguito)
- **Uso**: Reference per pattern anti-duplicazione

---

## 🔗 Quick Links

| Documento | Quando Usarlo |
|-----------|---------------|
| **ARCHITECTURE.md** | Setup progetto, onboarding, migrazioni DB |
| **UX_UI_SPEC.md** | Sviluppo nuove UI, design review |
| **API_VALIDATION.md** | Pre-release testing, debug API MikroTik |
| **IMPLEMENTATION_SUMMARY.md** | Review changelog, debug refactor v2.0 |
| **testing/** | Troubleshooting errori API, reference fix storici |

---

## 📚 Documentazione Correlata

- **[../README.md](../README.md)** - README principale del progetto (overview alto livello)
- **[../PROJECT_STATE_DOCUMENTATION.md](../PROJECT_STATE_DOCUMENTATION.md)** - Stato completo del progetto

---

**Manutenzione**: Aggiornare questo index ad ogni aggiunta di nuova documentazione.

