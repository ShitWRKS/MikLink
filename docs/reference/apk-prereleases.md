# APK prerelease automatiche

Il workflow `.github/workflows/develop-apk-release.yml` viene eseguito:

- a ogni push sul branch `develop`;
- manualmente tramite **Actions > Develop APK prerelease > Run workflow**.

La pipeline esegue i test, compila un APK `release` firmato e crea una GitHub prerelease associata al commit. Se lo stesso workflow viene rieseguito, aggiorna l'asset della release esistente invece di crearne una duplicata.

## Secrets richiesti

Configurare in **Settings > Secrets and variables > Actions > Repository secrets**:

| Secret | Contenuto |
|---|---|
| `MIKLINK_KEYSTORE_BASE64` | Keystore JKS codificato integralmente in Base64 |
| `MIKLINK_KEYSTORE_PASSWORD` | Password del keystore |
| `MIKLINK_KEY_ALIAS` | Alias della chiave di firma |
| `MIKLINK_KEY_PASSWORD` | Password della chiave |

La pipeline interrompe esplicitamente l'esecuzione se manca uno dei quattro secret.

## Codifica del keystore

### Linux

```bash
base64 -w 0 miklink-release.jks > miklink-release.jks.base64
```

### PowerShell

```powershell
[Convert]::ToBase64String(
    [IO.File]::ReadAllBytes("miklink-release.jks")
) | Set-Content -NoNewline "miklink-release.jks.base64"
```

Copiare l'intero contenuto di `miklink-release.jks.base64` nel secret `MIKLINK_KEYSTORE_BASE64`. Il file Base64 e il keystore non devono essere aggiunti al repository.

## Versionamento delle build develop

Per ogni esecuzione:

```text
versionCode = GITHUB_RUN_NUMBER
versionName = 1.0.0-dev.<run-number>+<short-sha>
tag         = develop-<run-number>-<short-sha>
APK         = MikLink-develop-<run-number>-<short-sha>.apk
```

Esempio:

```text
versionName = 1.0.0-dev.184+a31fd82
APK         = MikLink-develop-184-a31fd82.apk
```

## Conservazione della chiave

La stessa chiave di firma deve essere conservata per tutte le versioni che devono aggiornare installazioni esistenti. Cambiare chiave rende l'APK incompatibile come aggiornamento dell'app già installata.

Conservare il keystore e le password anche fuori da GitHub, in un archivio sicuro e sottoposto a backup.
