package com.aquasmart.app

import android.content.Context

/**
 * Guarda la última IP válida del ESP32 y el nombre del proyecto, para que
 * la próxima vez que se abra la app no haya que volver a escanear la red
 * desde cero (se intenta primero esta IP, y si responde, se usa directo).
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("aquasmart_prefs", Context.MODE_PRIVATE)

    var ultimaIp: String?
        get() = sp.getString(KEY_IP, null)
        set(value) = sp.edit().putString(KEY_IP, value).apply()

    var nombreDispositivo: String?
        get() = sp.getString(KEY_NOMBRE, null)
        set(value) = sp.edit().putString(KEY_NOMBRE, value).apply()

    fun limpiar() {
        sp.edit().remove(KEY_IP).remove(KEY_NOMBRE).apply()
    }

    companion object {
        private const val KEY_IP = "ultima_ip"
        private const val KEY_NOMBRE = "nombre_dispositivo"
    }
}
