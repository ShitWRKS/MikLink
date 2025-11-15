# MIKLINK - API VALIDATION CHECKLIST
**Target IP**: 192.168.0.251  
**Data**: 2025-01-15

---

## 🎯 OBIETTIVO

Validare completamente l'integrazione REST API con RouterOS MikroTik su dispositivo fisico (IP 192.168.0.251) prima del rollout in produzione.

---

## 📋 PRE-REQUISITI

### Hardware Setup
- [x] MikroTik configurato e raggiungibile su `192.168.0.251`
- [x] Telefono Android connesso alla stessa rete WiFi
- [x] Cavo Ethernet collegato a porta test (`ether1` o `ether2`)

### Software Setup
- [x] Script `run_mikrotik_commands.ps1` (Windows PowerShell)
- [x] Script `run_mikrotik_commands.sh` (Linux/macOS/WSL)
- [x] App MikLink installata su telefono
- [x] Credenziali RouterOS: username/password

---

## 🔬 TEST MANUALI (Script)

### Test 1: DHCP Client Print

**Comando**:
```powershell
curl -v -u "admin:" -X POST "http://192.168.0.251/rest/ip/dhcp-client/print" `
  -H 'Content-Type: application/json' `
  -d '{"?.interface":"ether1"}'
```

**Variante da testare**:
```powershell
# Test A: ?.interface (con punto)
-d '{"?.interface":"ether1"}'

# Test B: ?interface (senza punto)
-d '{"?interface":"ether1"}'
```

**Expected Response** (se DHCP client esiste):
```json
[
  {
    ".id": "*1",
    "disabled": "false",
    "status": "bound",
    "address": "192.168.1.100/24",
    "gateway": "192.168.1.1",
    "dns": "8.8.8.8"
  }
]
```

**Expected Response** (se non esiste):
```json
[]
```

**Checklist**:
- [ ] Test A (`?.interface`) funziona → usare questo nel DTO
- [ ] Test B (`?interface`) funziona → usare questo nel DTO
- [ ] Entrambi falliscono → investigare RouterOS version/syntax
- [ ] Response JSON è valido e parsabile
- [ ] Campo `status` contiene "bound" se lease attivo

**Azione post-test**: Aggiornare `InterfaceNameRequest` in `MikroTikApiService.kt`:
```kotlin
data class InterfaceNameRequest(
    @Json(name = "?.interface") val interfaceName: String  // o "?interface"
)
```

---

### Test 2: Interface Disable/Enable

**Comando Disable**:
```powershell
curl -v -u "admin:" -X POST "http://192.168.0.251/rest/interface/disable" `
  -H 'Content-Type: application/json' `
  -d '{"numbers":"ether1"}'
```

**Expected Response**: `{}` (empty object) o HTTP 200

**Comando Enable**:
```powershell
curl -v -u "admin:" -X POST "http://192.168.0.251/rest/interface/enable" `
  -H 'Content-Type: application/json' `
  -d '{"numbers":"ether1"}'
```

**Expected Response**: `{}` (empty object) o HTTP 200

**Checklist**:
- [ ] Disable riduce link a "no-link" (verificare con `/rest/interface/print`)
- [ ] Enable ripristina link a "link-ok"
- [ ] Nessun errore 400/401/500
- [ ] Tempo di recovery link < 5 secondi

**Attenzione**: NON eseguire su interfaccia di management o perdi connessione!

---

### Test 3: System Resource Print

**Comando**:
```powershell
curl -v -u "admin:" -X POST "http://192.168.0.251/rest/system/resource/print" `
  -H 'Content-Type: application/json' `
  -d '{".proplist":["board-name"]}'
```

**Expected Response**:
```json
[
  {
    "board-name": "RB4011iGS+RM"
  }
]
```

**Checklist**:
- [ ] Response contiene `board-name`
- [ ] Il valore è non-vuoto (es. "RB4011", "CCR1036")
- [ ] `Compatibility.isTdrSupported(boardName)` ritorna `true` se modello supporta TDR

---

### Test 4: Ethernet Monitor (Link Status)

**Comando**:
```powershell
curl -v -u "admin:" -X POST "http://192.168.0.251/rest/interface/ethernet/monitor" `
  -H 'Content-Type: application/json' `
  -d '{"numbers":"ether1","once":true}'
```

**Expected Response** (link UP):
```json
[
  {
    "status": "link-ok",
    "rate": "1Gbps"
  }
]
```

**Expected Response** (link DOWN):
```json
[
  {
    "status": "no-link",
    "rate": null
  }
]
```

**Checklist**:
- [ ] `status` = "link-ok" quando cavo collegato
- [ ] `status` = "no-link" quando cavo scollegato
- [ ] `rate` è presente e parsabile ("1Gbps", "100Mbps", "10Mbps")
- [ ] `RateParser.parseToMbps(rate)` ritorna valore corretto

---

### Test 5: Cable Test (TDR)

**Prerequisito**: Modello MikroTik deve supportare TDR (es. RB4011, CCR)

**Comando**:
```powershell
curl -v -u "admin:" -X POST "http://192.168.0.251/rest/interface/ethernet/cable-test" `
  -H 'Content-Type: application/json' `
  -d '{"numbers":"ether1"}'
```

**Expected Response** (cavo OK):
```json
[
  {
    "status": "open",
    "cable-pairs": [
      {"pair": "1-2", "status": "open", "length": "45m"},
      {"pair": "3-6", "status": "open", "length": "45m"}
    ]
  }
]
```

**Expected Response** (cavo difettoso):
```json
[
  {
    "status": "short",
    "cable-pairs": [
      {"pair": "1-2", "status": "short", "length": "12m"},
      {"pair": "3-6", "status": "open", "length": "45m"}
    ]
  }
]
```

**Checklist**:
- [ ] Se modello NON supporta TDR → HTTP 500 o `{"error":"..."}` → gestire in app
- [ ] Se supporta → `cable-pairs` è array non vuoto
- [ ] Ogni pair ha `status` e `length`
- [ ] App mostra correttamente "open" (OK) vs "short/open" (difetto)

---

### Test 6: LLDP/CDP Neighbor Print

**Comando**:
```powershell
curl -v -u "admin:" -X POST "http://192.168.0.251/rest/ip/neighbor/print" `
  -H 'Content-Type: application/json' `
  -d '{"?.query":["interface=ether1"],".proplist":["identity","interface-name","system-caps-enabled","discovered-by"]}'
```

**Expected Response** (se switch LLDP presente):
```json
[
  {
    "identity": "SW-Core-01",
    "interface-name": "ge-0/0/24",
    "system-caps-enabled": "bridge,router",
    "discovered-by": "lldp"
  }
]
```

**Expected Response** (nessun neighbor):
```json
[]
```

**Checklist**:
- [ ] Se switch con LLDP/CDP → response non vuota
- [ ] Campi `identity`, `interface-name`, `system-caps-enabled` presenti
- [ ] `discovered-by` = "lldp" o "cdp"
- [ ] App mostra correttamente switch e porta collegata

---

### Test 7: Ping

**Comando** (4 ping):
```powershell
curl -v -u "admin:" -X POST "http://192.168.0.251/rest/ping" `
  -H 'Content-Type: application/json' `
  -d '{"address":"8.8.8.8","count":"4"}'
```

**Expected Response**:
```json
[
  {
    "avg-rtt": "12ms"
  }
]
```

**Variante count**:
```powershell
# Test 10 ping
-d '{"address":"8.8.8.8","count":"10"}'
```

**Checklist**:
- [ ] Ping a target raggiungibile → `avg-rtt` presente (es. "12ms")
- [ ] Ping a target non raggiungibile → `avg-rtt` = null o timeout
- [ ] Parametro `count` modificabile (testare 1, 4, 10, 20)
- [ ] Response time < 5 secondi per 4 ping

---

### Test 8: Traceroute

**Comando**:
```powershell
curl -v -u "admin:" -X POST "http://192.168.0.251/rest/tool/traceroute" `
  -H 'Content-Type: application/json' `
  -d '{"address":"8.8.8.8","max-hops":"10","timeout":"3000ms"}'
```

**Expected Response**:
```json
[
  {"hop": "1", "host": "192.168.1.1", "avg-rtt": "1ms"},
  {"hop": "2", "host": "10.0.0.1", "avg-rtt": "5ms"},
  {"hop": "3", "host": "8.8.8.8", "avg-rtt": "12ms"}
]
```

**Checklist**:
- [ ] Response è array di hop in ordine
- [ ] Ogni hop ha `hop`, `host`, `avg-rtt`
- [ ] Se hop non risponde → `host` = "*" o null
- [ ] Parametri `max-hops` e `timeout` rispettati

---

## 🔧 TEST IN-APP (Pulsante "Verifica Sonda")

### Implementazione in ProbeEditScreen

Aggiungere pulsante "Test Completo" che esegue sequenza:

```kotlin
suspend fun runFullProbeTest(probe: ProbeConfig): ProbeTestReport {
    val results = mutableMapOf<String, TestResult>()
    
    // 1. System Resource
    results["system_resource"] = try {
        val api = buildServiceFor(probe)
        val res = api.getSystemResource(ProplistRequest(listOf("board-name")))
        TestResult.Success("Board: ${res.firstOrNull()?.boardName}")
    } catch (e: Exception) {
        TestResult.Failure(e.message ?: "Unknown error")
    }
    
    // 2. DHCP Client Print
    results["dhcp_client"] = try {
        val api = buildServiceFor(probe)
        val dhcp = api.getDhcpClientStatus(InterfaceNameRequest(probe.testInterface))
        TestResult.Success("DHCP: ${dhcp.firstOrNull()?.status ?: "no client"}")
    } catch (e: Exception) {
        TestResult.Failure(e.message ?: "Unknown error")
    }
    
    // 3. Interface Disable/Enable
    results["interface_toggle"] = try {
        val api = buildServiceFor(probe)
        api.disableInterface(NumbersRequest(probe.testInterface))
        delay(1000)
        api.enableInterface(NumbersRequest(probe.testInterface))
        delay(2000)
        TestResult.Success("Toggle: OK")
    } catch (e: Exception) {
        TestResult.Failure(e.message ?: "Unknown error")
    }
    
    // 4. Ethernet Monitor
    results["link_status"] = try {
        val api = buildServiceFor(probe)
        val link = api.getLinkStatus(MonitorRequest(probe.testInterface, true))
        val status = link.firstOrNull()
        TestResult.Success("Link: ${status?.status} @ ${status?.rate}")
    } catch (e: Exception) {
        TestResult.Failure(e.message ?: "Unknown error")
    }
    
    // 5. Ping
    results["ping"] = try {
        val api = buildServiceFor(probe)
        val ping = api.runPing(PingRequest("8.8.8.8", "4"))
        TestResult.Success("Ping: ${ping.firstOrNull()?.avgRtt}")
    } catch (e: Exception) {
        TestResult.Failure(e.message ?: "Unknown error")
    }
    
    // 6. Traceroute
    results["traceroute"] = try {
        val api = buildServiceFor(probe)
        val tr = api.runTraceroute(TracerouteRequest("8.8.8.8", "5", "2000ms"))
        TestResult.Success("Traceroute: ${tr.size} hops")
    } catch (e: Exception) {
        TestResult.Failure(e.message ?: "Unknown error")
    }
    
    return ProbeTestReport(
        success = results.values.all { it is TestResult.Success },
        results = results
    )
}

sealed class TestResult {
    data class Success(val message: String) : TestResult()
    data class Failure(val error: String) : TestResult()
}

data class ProbeTestReport(
    val success: Boolean,
    val results: Map<String, TestResult>
)
```

**UI Display**:
```kotlin
@Composable
fun ProbeTestReportCard(report: ProbeTestReport) {
    Card {
        Column {
            Text(
                if (report.success) "✓ Tutti i test superati" else "✗ Alcuni test falliti",
                color = if (report.success) Color.Green else Color.Red
            )
            
            report.results.forEach { (test, result) ->
                Row {
                    Icon(
                        if (result is TestResult.Success) Icons.Default.Check else Icons.Default.Close,
                        tint = if (result is TestResult.Success) Color.Green else Color.Red
                    )
                    Text(test)
                    when (result) {
                        is TestResult.Success -> Text(result.message)
                        is TestResult.Failure -> Text(result.error, color = Color.Red)
                    }
                }
            }
        }
    }
}
```

---

## 📊 CHECKLIST FINALE

### Script Tests (Manuale)
- [ ] Test 1: DHCP Client Print (verificare `?.interface` vs `?interface`)
- [ ] Test 2: Interface Disable/Enable
- [ ] Test 3: System Resource Print
- [ ] Test 4: Ethernet Monitor (Link Status)
- [ ] Test 5: Cable Test (TDR) - se supportato
- [ ] Test 6: LLDP/CDP Neighbor Print
- [ ] Test 7: Ping (count variabile)
- [ ] Test 8: Traceroute

### In-App Tests
- [ ] "Verifica Sonda" button in ProbeEditScreen implementato
- [ ] Tutti i 6 test eseguiti correttamente
- [ ] Report dettagliato mostrato in UI
- [ ] Errori gestiti con messaggi chiari

### DTO Alignment
- [ ] `InterfaceNameRequest` aggiornato con sintassi corretta
- [ ] Tutti i DTO allineati con response MikroTik
- [ ] Moshi parsing funziona senza eccezioni

### Error Handling
- [ ] HTTP 401 (auth failed) → messaggio "Credenziali errate"
- [ ] HTTP 404 (endpoint not found) → messaggio "API non supportata"
- [ ] HTTP 500 (server error) → messaggio "Errore sonda"
- [ ] Timeout → messaggio "Sonda non raggiungibile"

---

## 🚨 TROUBLESHOOTING

### Problema: DHCP Client Print fallisce con 400 Bad Request
**Causa probabile**: Sintassi query filtro errata  
**Fix**: Verificare `?.interface` vs `?interface` e aggiornare DTO

### Problema: Cable Test ritorna 500
**Causa probabile**: Modello non supporta TDR  
**Fix**: Verificare `tdrSupported` flag e disabilitare test in UI

### Problema: Traceroute non completa
**Causa probabile**: Timeout troppo basso o max-hops insufficiente  
**Fix**: Aumentare `timeout` a 5000ms e `max-hops` a 30

### Problema: Ping/Traceroute fallisce con "no route to host"
**Causa probabile**: Interfaccia test non ha gateway configurato  
**Fix**: Verificare DHCP lease o config statica corretta

---

**Fine API_VALIDATION.md**

