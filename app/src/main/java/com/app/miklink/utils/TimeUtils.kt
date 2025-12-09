package com.app.miklink.utils

/**
 * Normalizza valori di tempo ricevuti dalle API MikroTik.
 * 
 * MikroTik restituisce valori temporali in formato verboso (es. "21ms30us", "1s500ms").
 * Questa funzione estrae solo la parte più significativa per la visualizzazione.
 * 
 * Esempi:
 * - "21ms30us" → "21ms" (rimuove microsecondi)
 * - "17ms341us" → "17ms"
 * - "1s" → "1000ms" (converte secondi in millisecondi)
 * - "1s500ms" → "1500ms"
 * - "2m30s" → "150000ms" (converte minuti in millisecondi)
 * - null → "N/A"
 * 
 * @param rawTime Valore temporale grezzo dalla API MikroTik
 * @return Valore normalizzato (solo millisecondi) o "N/A" se non parsabile
 */
fun normalizeTime(rawTime: String?): String {
    if (rawTime.isNullOrBlank()) return "N/A"
    
    try {
        var totalMs = 0.0
        val input = rawTime.trim()
        
        // Regex per estrarre componenti temporali: numero seguito da unità (m, s, ms, us, ns)
        // Ordine importante: ms prima di m, us prima di s
        val timeRegex = Regex("""(\d+(?:\.\d+)?)(ms|us|ns|m|s)""")
        val matches = timeRegex.findAll(input)
        
        if (!matches.any()) {
            // Se non ci sono match, ritorna il valore originale
            return rawTime
        }
        
        var hasTimeUnits = false
        for (match in matches) {
            val value = match.groupValues[1].toDoubleOrNull() ?: continue
            val unit = match.groupValues[2]
            hasTimeUnits = true
            
            totalMs += when (unit) {
                "m" -> value * 60 * 1000 // minuti
                "s" -> value * 1000 // secondi
                "ms" -> value // millisecondi
                "us" -> 0.0 // microsecondi - ignoriamo per semplicità
                "ns" -> 0.0 // nanosecondi - ignoriamo
                else -> 0.0
            }
        }
        
        // Arrotonda a intero e formatta
        val roundedMs = totalMs.toInt()
        
        // Se abbiamo trovato unità temporali ma il risultato è 0ms (es. "12us", "0ms12us"),
        // restituiamo "0ms" invece del valore grezzo
        return if (roundedMs > 0) {
            "${roundedMs}ms"
        } else if (hasTimeUnits) {
            "0ms"  // Sub-millisecond values (es. "12us", "0ms12us")
        } else {
            rawTime  // Fallback per formati non riconosciuti
        }
        
    } catch (e: Exception) {
        // In caso di errore nel parsing, ritorna il valore originale
        return rawTime
    }
}
