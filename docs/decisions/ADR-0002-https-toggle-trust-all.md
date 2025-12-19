# ADR-0002 — HTTP/HTTPS toggle con trust-all in HTTPS

- **Status:** Accepted
- **Data:** 2025-12-13

## Contesto

La sonda (MikroTik) è spesso un dispositivo “solo strumento” in cantiere.
Quando si usa HTTPS, nella maggior parte dei casi non c'è un certificato valido (CA pubblica / hostname).
L'utente deve poter scegliere **esplicitamente** tra HTTP e HTTPS.

## Decisione

Nella UI di configurazione sonda esiste un toggle:

- **OFF** → usa `http://<ip>`
- **ON** → usa `https://<ip>` con:
  - **nessuna verifica** del certificato
  - **nessuna verifica** dell'hostname

Questa è una scelta consapevole (trade-off accettato per il contesto d'uso).

### Precisazione TLS

- Il trust-all copre solo certificato/hostname. **Non** risolve incompatibilità di protocollo/cipher: se RouterOS espone HTTPS con cipher suite non supportate da OkHttp/JVM, la stretta di mano TLS fallirà comunque.
- In caso di handshake fallito, l'app prova un fallback automatico su HTTP; se fallisce anche il fallback, mostra un messaggio esplicito che suggerisce di:
  - Installare un certificato valido su RouterOS (www-ssl), **oppure**
  - Usare HTTP se il contesto lo consente.
- Non aggiungere cipher suite obsolete o workaround insicuri per “farlo funzionare”.
- Gli errori di trasporto vengono aggregati: quando il fallback HTTP fallisce dopo un handshake TLS, `CallOutcome.Failure` conserva entrambe le eccezioni (https + http) così la UI può mostrare un hint TLS senza perdere il contesto del secondo errore.

## Conseguenze

- Implementare trust-all **solo** quando `isHttps = true`.
- Tenere la scelta nel model di configurazione sonda (`ProbeConfig.isHttps` o equivalente dominio futuro).
- Documentare chiaramente la scelta (questo ADR) per evitare regressioni di security-hardening non desiderate.
