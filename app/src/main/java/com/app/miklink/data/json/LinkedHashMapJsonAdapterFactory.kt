package com.app.miklink.data.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.LinkedHashMap

/**
 * Moshi supports Map interfaces by default, but release shrinking/optimization can surface
 * concrete LinkedHashMap field types in obfuscated classes.
 * This factory maps LinkedHashMap<TK, TV> to Moshi's standard Map adapter.
 */
object LinkedHashMapJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi
    ): JsonAdapter<*>? {
        if (annotations.isNotEmpty()) return null
        if (Types.getRawType(type) != LinkedHashMap::class.java) return null

        val (keyType, valueType) = if (type is ParameterizedType) {
            type.actualTypeArguments[0] to type.actualTypeArguments[1]
        } else {
            Any::class.java to Any::class.java
        }

        val mapType = Types.newParameterizedType(Map::class.java, keyType, valueType)
        val delegate = moshi.adapter<Map<Any?, Any?>>(mapType)

        return object : JsonAdapter<LinkedHashMap<Any?, Any?>>() {
            override fun fromJson(reader: JsonReader): LinkedHashMap<Any?, Any?>? {
                val parsed = delegate.fromJson(reader) ?: return null
                return LinkedHashMap(parsed)
            }

            override fun toJson(writer: JsonWriter, value: LinkedHashMap<Any?, Any?>?) {
                delegate.toJson(writer, value)
            }
        }.nullSafe()
    }
}
