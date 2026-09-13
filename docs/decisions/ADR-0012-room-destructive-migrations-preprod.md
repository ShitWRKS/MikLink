# ADR-0012 — Destructive migrations in pre-production

- **Status:** Superseded by ADR-0014
- **Data:** 2025-12-30

## Contesto

MikLink è in fase pre-production con iterazione rapida dello schema DB.
La gestione esplicita delle migrazioni Room aggiunge overhead che rallenta lo sviluppo senza benefici concreti: i tester possono ricaricare i dati di seed rapidamente.

## Decisione

Questa decisione e' stata superata da ADR-0014, che impone migrazioni Room esplicite e testate.

**Source of truth dello schema:**

- Versione corrente: annotazione `@Database(version = X)` in `MikLinkDatabase.kt`
- Schema snapshot: `app/schemas/com.app.miklink.data.local.room.MikLinkDatabase/<version>.json`
- Il file JSON dell'ultima versione rappresenta lo schema corrente

## Conseguenze

**Pro:**

- Bump versione schema senza scrivere `Migration` intermedie
- Iterazione rapida su entità/colonne
- DB ricreato automaticamente con seed di default (callback `onCreate` in `DatabaseModule`)

**Contro:**

- Perdita dati locali ad ogni bump di versione senza migration esplicita
- Utenti alpha/beta devono rifare setup client e profili dopo aggiornamenti con schema change

**Criterio di uscita (non implementato ora):**

Quando MikLink entrerà in produzione stabile, sostituiremo destructive migration con migrazioni incrementali esplicite per preservare i dati utente. Questo richiederà un ADR dedicato.

**Non-goals:**

- Non stiamo introducendo migrazioni programmatiche ora
- Non stiamo definendo il processo di migrazione a production-ready

## Link

- Supersedes: nessuno (integra ADR-0003)
- Vedi: `app/src/main/java/com/app/miklink/di/DatabaseModule.kt` per configurazione
- Vedi: ADR-0003 per baseline iniziale
