package com.app.miklink.data.repositoryimpl.mikrotik

import android.content.Context
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.data.remote.mikrotik.service.MikroTikServiceProvider
import com.app.miklink.core.data.repository.ProbeCheckResult
import com.app.miklink.core.data.repository.probe.ProbeConnectivityRepository
import com.app.miklink.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementazione di ProbeConnectivityRepository.
 *
 * Usa MikroTikServiceProvider per costruire il service e chiama l'API MikroTik
 * per verificare la connessione e ottenere informazioni hardware.
 */
class ProbeConnectivityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serviceProvider: MikroTikServiceProvider
) : ProbeConnectivityRepository {

    override suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult =
        withContext(Dispatchers.IO) {
            try {
                val api = serviceProvider.build(probe)
                val boardName = api.getSystemResource(com.app.miklink.core.data.remote.mikrotik.dto.ProplistRequest(listOf("board-name")))
                    .firstOrNull()?.boardName ?: "Unknown Board"
                val interfacesRaw = api.getEthernetInterfaces()
                android.util.Log.d("ProbeConnectivityRepository", "checkProbeConnection: Ricevute ${interfacesRaw.size} interfacce dall'API")
                val interfaces = interfacesRaw.map {
                    android.util.Log.d("ProbeConnectivityRepository", "checkProbeConnection: Interface name = '${it.name}'")
                    it.name
                }
                android.util.Log.d("ProbeConnectivityRepository", "checkProbeConnection: Interfacce mappate: $interfaces")
                ProbeCheckResult.Success(boardName, interfaces)
            } catch (e: Exception) {
                android.util.Log.e("ProbeConnectivityRepository", "checkProbeConnection: Errore durante la verifica", e)
                ProbeCheckResult.Error(e.message ?: context.getString(R.string.error_probe_connection_unknown))
            }
        }
}

