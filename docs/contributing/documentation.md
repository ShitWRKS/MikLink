# Contributing alla documentazione

Obiettivo: avere doc che resta valida nel tempo e non diventa “diario”.

## Metodo adottato

- **Diátaxis** per separare i tipi di contenuto:
  - *Tutorial* (imparare) — percorso guidato e lineare
  - *How-to* (fare) — ricette brevi per obiettivi specifici
  - *Reference* (consultare) — API, formati, schema DB, comandi build
  - *Explanation* (capire) — architettura, trade-off, motivazioni
- **ADR** per decisioni architetturali e invarianti
- **Docs-as-code**: la doc vive nel repo, passa review, e viene aggiornata nello stesso PR del codice

## Regole pratiche

- La **fonte di verità** è il codice. La doc deve:
  - puntare ai file “owner” e non duplicare dettagli fragili
  - descrivere le invarianti e i confini (cosa *non* deve succedere)
- Se rinomini/sposti file o package → aggiorna i path in doc nello stesso PR.
- Evita file “onnivori” tipo `Utils.kt`, `Models.kt` quando diventano contenitori di cose non correlate.

## Quando scrivere un ADR

Scrivi un ADR se:
- introduci un trade-off (es. security vs usabilità)
- imposti un'invariante (es. single probe)
- scegli un formato persistente o compatibilità (DB schema, backup JSON, report JSON)
- cambi una regola di confine (es. cosa può importare `core/domain`)

## Template ADR

Crea un file in `docs/decisions/ADR-XXXX-<slug>.md`.

```md
# ADR-XXXX: Titolo

## Contesto

## Decisione

## Conseguenze

## Alternative considerate (opzionale)
```

## Checklist PR (doc)

- [ ] Ho aggiornato l'indice in `docs/README.md` se ho aggiunto/rimosso pagine importanti
- [ ] Ho aggiornato i riferimenti a file/percorsi rinominati
- [ ] Ho aggiornato DB schema reference se cambia Room schema
- [ ] Ho aggiornato ADR se cambia un'invariante o un confine architetturale
- [ ] Ho aggiunto/aggiornato test “bussola” se tocco parsing/mapping/formati
