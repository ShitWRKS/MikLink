package com.app.miklink.utils

/**
 * Utility object for validating network targets (IP addresses and hostnames)
 * compatible with MikroTik REST API requirements.
 */
object NetworkValidator {
    
    /**
     * Validates if the input is a valid IPv4 address.
     * Examples: "8.8.8.8", "192.168.1.1"
     */
    fun isValidIpAddress(input: String): Boolean {
        if (input.isBlank()) return false
        
        val parts = input.split(".")
        if (parts.size != 4) return false
        
        return parts.all { part ->
            part.isNotEmpty() && part.all { it in '0'..'9' } &&
                (part.toIntOrNull()?.let { it in 0..255 } ?: false)
        }
    }

    /**
     * Validates an IPv4 address with CIDR prefix.
     * Example: "10.0.3.100/24"
     */
    fun isValidIpv4Cidr(value: String): Boolean {
        if (value.isBlank()) return false

        val parts = value.split("/")
        if (parts.size != 2) return false

        val prefixPart = parts[1]
        if (prefixPart.isEmpty() || prefixPart.any { it !in '0'..'9' }) return false

        val prefix = prefixPart.toIntOrNull() ?: return false
        return isValidIpAddress(parts[0]) && prefix in 0..32
    }

    /**
     * Validates a plain IPv4 address and rejects CIDR notation.
     * Example: "10.0.3.1"
     */
    fun isValidIpv4WithoutCidr(value: String): Boolean {
        if (value.contains("/")) return false
        return isValidIpAddress(value)
    }
    
    /**
     * Validates if the input is a valid hostname WITHOUT schema.
     * MikroTik API requires plain hostnames without http://, https://, or www.
     * 
     * Valid: "google.it", "dns.google", "cloudflare.com"
     * Invalid: "https://google.it", "http://example.com", "www.google.it"
     */
    fun isValidHostname(input: String): Boolean {
        if (input.isBlank()) return false
        
        // Reject if contains schema
        if (input.contains("://")) return false
        
        // Reject if starts with www.
        if (input.startsWith("www.", ignoreCase = true)) return false
        
        // Basic hostname validation: alphanumeric, dots, hyphens
        val hostnameRegex = Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$")
        return hostnameRegex.matches(input)
    }
    
    /**
     * Validates if the input is a valid ping target.
     * Accepts:
     * - Valid IPv4 addresses
     * - Valid hostnames (without schema)
     * - Special keyword "DHCP_GATEWAY"
     */
    fun isValidTarget(input: String): Boolean {
        if (input.isBlank()) return false
        
        // Accept special keyword
        if (input.equals("DHCP_GATEWAY", ignoreCase = true)) return true
        
        // Check if it's a valid IP or hostname
        return isValidIpAddress(input) || isValidHostname(input)
    }
}
