package com.app.miklink.data.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Adapter Moshi per gestire risposte MikroTik che possono essere
 * sia un singolo oggetto che un array.
 *
 * Esempio:
 * - 1 neighbor: MikroTik restituisce { "identity": "...", ... }
 * - 2+ neighbor: MikroTik restituisce [{ "identity": "..." }, { ... }]
 *
 * Questo adapter normalizza sempre a List<NeighborDetail>
 */
class NeighborDetailListAdapter {

    @FromJson
    fun fromJson(reader: JsonReader): List<NeighborDetail> {
        val result = mutableListOf<NeighborDetail>()

        // Peek per vedere se è un oggetto o un array
        val token = reader.peek()

        when (token) {
            JsonReader.Token.BEGIN_ARRAY -> {
                // È un array: parsing normale
                reader.beginArray()
                while (reader.hasNext()) {
                    result.add(readNeighborDetail(reader))
                }
                reader.endArray()
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                // È un singolo oggetto: wrappa in lista
                result.add(readNeighborDetail(reader))
            }
            else -> {
                // Formato sconosciuto: salta
                reader.skipValue()
            }
        }

        return result
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: List<NeighborDetail>?) {
        // Serializzazione standard (non dovrebbe essere necessaria)
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginArray()
        for (item in value) {
            writeNeighborDetail(writer, item)
        }
        writer.endArray()
    }

    private fun readNeighborDetail(reader: JsonReader): NeighborDetail {
        var identity: String? = null
        var interfaceName: String? = null
        var systemCaps: String? = null
        var discoveredBy: String? = null
        var vlanId: String? = null
        var voiceVlanId: String? = null
        var poeClass: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "identity" -> identity = reader.nextString()
                "interface-name" -> interfaceName = reader.nextString()
                "system-caps-enabled" -> systemCaps = reader.nextString()
                "discovered-by" -> discoveredBy = reader.nextString()
                "vlan-id" -> vlanId = if (reader.peek() == JsonReader.Token.NULL) { reader.nextNull<String?>(); null } else reader.nextString()
                "voice-vlan-id" -> voiceVlanId = if (reader.peek() == JsonReader.Token.NULL) { reader.nextNull<String?>(); null } else reader.nextString()
                "poe-class" -> poeClass = if (reader.peek() == JsonReader.Token.NULL) { reader.nextNull<String?>(); null } else reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return NeighborDetail(
            identity = identity,
            interfaceName = interfaceName,
            systemCaps = systemCaps,
            discoveredBy = discoveredBy,
            vlanId = vlanId,
            voiceVlanId = voiceVlanId,
            poeClass = poeClass
        )
    }

    private fun writeNeighborDetail(writer: JsonWriter, neighbor: NeighborDetail) {
        writer.beginObject()
        writer.name("identity").value(neighbor.identity)
        writer.name("interface-name").value(neighbor.interfaceName)
        writer.name("system-caps-enabled").value(neighbor.systemCaps)
        writer.name("discovered-by").value(neighbor.discoveredBy)
        writer.name("vlan-id").value(neighbor.vlanId)
        writer.name("voice-vlan-id").value(neighbor.voiceVlanId)
        writer.name("poe-class").value(neighbor.poeClass)
        writer.endObject()
    }
}

