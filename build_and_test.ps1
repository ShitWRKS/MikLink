#!/usr/bin/env pwsh
# Build script per validare tutte le modifiche

Write-Host "🚀 Build MikLink - Validazione Modifiche" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "📝 Modifiche applicate:" -ForegroundColor Yellow
Write-Host "  ✅ Eliminato Traceroute completamente (8 file modificati)" -ForegroundColor Green
Write-Host "  ✅ Consolidata logica test (rimosso buildSectionsFromResults)" -ForegroundColor Green
Write-Host "  ✅ Risolto problema inconsistenza Ping PASS/FAIL" -ForegroundColor Green
Write-Host ""

Write-Host "🔨 Avvio build..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug --no-daemon

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ BUILD COMPLETATA CON SUCCESSO!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 Prossimi passi:" -ForegroundColor Cyan
    Write-Host "  1. Disinstalla l'app dal device (per pulizia database)" -ForegroundColor White
    Write-Host "  2. Installa la nuova build" -ForegroundColor White
    Write-Host "  3. Crea un profilo test con Ping abilitato" -ForegroundColor White
    Write-Host "  4. Esegui un test e verifica:" -ForegroundColor White
    Write-Host "     - Durante test: card Ping mostra 'In corso...'" -ForegroundColor Gray
    Write-Host "     - A completamento: card Ping mostra PASS con dettagli" -ForegroundColor Gray
    Write-Host "     - Schermata risultati: card Ping IDENTICA" -ForegroundColor Gray
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ BUILD FALLITA!" -ForegroundColor Red
    Write-Host "Controlla gli errori sopra." -ForegroundColor Red
    Write-Host ""
    exit 1
}

