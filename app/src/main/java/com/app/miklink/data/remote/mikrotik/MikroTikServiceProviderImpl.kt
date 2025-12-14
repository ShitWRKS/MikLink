package com.app.miklink.data.remote.mikrotik

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.data.remote.mikrotik.infra.MikroTikServiceFactory
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikApiService
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikServiceProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Implementazione di MikroTikServiceProvider.
 * 
 * Centralizza la creazione del service MikroTik, inclusa la gestione del WiFi network binding
 * per garantire che le chiamate API utilizzino la rete WiFi corretta.
 */
class MikroTikServiceProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serviceFactory: MikroTikServiceFactory
) : MikroTikServiceProvider {

    @Suppress("DEPRECATION")
    private fun findWifiNetwork(): Network? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.allNetworks.firstOrNull { network ->
            val caps = connectivityManager.getNetworkCapabilities(network)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    override fun build(probe: ProbeConfig): MikroTikApiService {
        val wifiNetwork = findWifiNetwork()
        return serviceFactory.createService(probe, wifiNetwork?.socketFactory)
    }
}

