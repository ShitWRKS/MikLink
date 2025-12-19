/*
 * Purpose: Serialize/deserialize TestThresholds to JSON for Room persistence without schema explosion.
 * Inputs: Raw JSON string from the entity or a TestThresholds instance from the domain.
 * Outputs: Parsed TestThresholds or JSON string ready for storage.
 * Notes: Uses Moshi with default adapter; failures fall back to defaults to keep profiles usable.
 */
package com.app.miklink.data.local.room.mapper

import com.app.miklink.core.domain.model.TestThresholds
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val thresholdsAdapter by lazy {
    runCatching {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter(TestThresholds::class.java)
    }.getOrNull()
}

fun String?.toThresholdsOrDefault(): TestThresholds {
    return this?.let { json ->
        runCatching { thresholdsAdapter?.fromJson(json) }.getOrNull()
    } ?: TestThresholds.defaults()
}

fun TestThresholds.toJsonOrNull(): String? {
    val adapter = thresholdsAdapter ?: return null
    return runCatching { adapter.toJson(this) }.getOrNull()
}
