/*
 * Purpose: Hilt qualifiers for the two Moshi instances.
 * Inputs: none.
 * Outputs: @AppMoshi (strict, for backup/report/app codecs) and @RouterOsMoshi (RouterOS Retrofit only).
 * Notes: No global Boolean/Int adapters; no global coercion.
 */
package com.app.miklink.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppMoshi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RouterOsMoshi
