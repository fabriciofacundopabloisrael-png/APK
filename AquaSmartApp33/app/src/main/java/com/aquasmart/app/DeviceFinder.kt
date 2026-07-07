package com.aquasmart.app

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL

/** IP fija del ESP32 cuando actúa como Access Point (modo configuración). */
const val IP_MODO_AP = "192.168.4.1"

data class DispositivoEncontrado(val ip: String, val nombre: String)

/**
 * Encuentra el ESP32 de AquaSmart en la red, sin necesitar mDNS ni
 * configuración manual. Orden de búsqueda:
 *   1) Última IP guardada (caso normal, el más rápido).
 *   2) IP fija del modo Access Point (192.168.4.1), por si el teléfono
 *      está conectado a la red WiFi propia del dispositivo.
 *   3) Escaneo de la subred WiFi actual del teléfono, en paralelo,
 *      preguntando por /info y verificando que la respuesta sea
 *      realmente del firmware de AquaSmart.
 */
object DeviceFinder {

    private const val TIMEOUT_RAPIDO_MS = 900
    private const val TIMEOUT_ESCANEO_MS = 350

    suspend fun buscar(context: Context, ipGuardada: String?): DispositivoEncontrado? {
        // 1) Última IP conocida
        if (!ipGuardada.isNullOrBlank()) {
            consultarInfo(ipGuardada, TIMEOUT_RAPIDO_MS)?.let { return it }
        }

        // 2) Modo Access Point del ESP32
        if (ipGuardada != IP_MODO_AP) {
            consultarInfo(IP_MODO_AP, TIMEOUT_RAPIDO_MS)?.let { return it }
        }

        // 3) Escaneo de la subred local (red de casa)
        return escanearSubred(context)
    }

    /** Hace GET a http://ip/info y valida que sea el firmware de AquaSmart. */
    private suspend fun consultarInfo(ip: String, timeoutMs: Int): DispositivoEncontrado? =
        withContext(Dispatchers.IO) {
            var conexion: HttpURLConnection? = null
            try {
                val url = URL("http://$ip/info")
                conexion = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                    requestMethod = "GET"
                }
                if (conexion.responseCode != 200) return@withContext null

                val cuerpo = conexion.inputStream.bufferedReader().readText()
                // Verificación mínima de identidad: el JSON del firmware
                // de AquaSmart siempre incluye "ssidAP" y "nombre".
                if (!cuerpo.contains("ssidAP") || !cuerpo.contains("nombre")) return@withContext null

                val nombre = extraerCampoJson(cuerpo, "nombre") ?: "AquaSmart"
                DispositivoEncontrado(ip, nombre)
            } catch (e: Exception) {
                null
            } finally {
                conexion?.disconnect()
            }
        }

    /** Extrae el valor de un campo string simple de un JSON plano (sin librerías extra). */
    private fun extraerCampoJson(json: String, campo: String): String? {
        val regex = "\"$campo\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    /** Escanea todas las IPs de la subred /24 actual, en paralelo, buscando el ESP32. */
    private suspend fun escanearSubred(context: Context): DispositivoEncontrado? {
        val prefijo = obtenerPrefijoSubred(context) ?: return null

        return withTimeoutOrNull(6000) {
            withContext(Dispatchers.IO) {
                val trabajos = (1..254).map { host ->
                    async {
                        consultarInfo("$prefijo.$host", TIMEOUT_ESCANEO_MS)
                    }
                }
                // Devuelve el primero que encuentre algo válido; cancela el resto.
                var encontrado: DispositivoEncontrado? = null
                for (trabajo in trabajos) {
                    val resultado = trabajo.await()
                    if (resultado != null && encontrado == null) {
                        encontrado = resultado
                    }
                }
                encontrado
            }
        }
    }

    /** Obtiene "192.168.1" a partir de la IP actual del teléfono en WiFi. */
    private fun obtenerPrefijoSubred(context: Context): String? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null

        val ipEntera = wifiManager.connectionInfo?.ipAddress ?: return null
        if (ipEntera == 0) return null

        val ipTexto = Formatter.formatIpAddress(ipEntera)
        val partes = ipTexto.split(".")
        if (partes.size != 4) return null

        return "${partes[0]}.${partes[1]}.${partes[2]}"
    }
}
