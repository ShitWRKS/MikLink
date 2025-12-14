package com.app.miklink.testsupport

import com.app.miklink.data.remote.mikrotik.infra.NeighborDetailListAdapter
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object TestMoshiProvider {
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(NeighborDetailListAdapter())
            .add(object {
                @FromJson
                fun fromJson(reader: com.squareup.moshi.JsonReader): Boolean {
                    return if (reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                        reader.nextString().equals("true", ignoreCase = true)
                    } else {
                        reader.nextBoolean()
                    }
                }

                @ToJson
                fun toJson(writer: com.squareup.moshi.JsonWriter, value: Boolean) {
                    writer.value(value)
                }
            })
            .add(object {
                @FromJson
                fun fromJson(reader: com.squareup.moshi.JsonReader): Int {
                    return if (reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                        reader.nextString().toIntOrNull() ?: 0
                    } else {
                        reader.nextInt()
                    }
                }

                @ToJson
                fun toJson(writer: com.squareup.moshi.JsonWriter, value: Int) {
                    writer.value(value)
                }
            })
            .add(KotlinJsonAdapterFactory())
            .build()
    }
}
