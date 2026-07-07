package com.aquasmart.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aquasmart.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Pantalla única de la app. NO es un dashboard: solo detecta el ESP32,
 * recuerda su IP y da acceso al panel web real del dispositivo mediante
 * WebView. Todo el contenido (gráficas, historial, configuración) vive
 * en la página web del ESP32, no aquí.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    private var ipActual: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        binding.btnAbrirPanel.setOnClickListener { abrirPanel(ipActual) }
        binding.btnBuscarNuevamente.setOnClickListener { buscarDispositivo() }
        binding.btnReconfigurarWifi.setOnClickListener { abrirReconfiguracionWifi() }

        buscarDispositivo()
    }

    private fun buscarDispositivo() {
        mostrarEstadoBuscando()

        lifecycleScope.launch {
            val encontrado = DeviceFinder.buscar(this@MainActivity, prefs.ultimaIp)

            if (encontrado != null) {
                prefs.ultimaIp = encontrado.ip
                prefs.nombreDispositivo = encontrado.nombre
                ipActual = encontrado.ip
                mostrarEstadoConectado(encontrado.nombre, encontrado.ip)
            } else {
                ipActual = null
                mostrarEstadoNoEncontrado()
            }
        }
    }

    private fun abrirPanel(ip: String?) {
        if (ip.isNullOrBlank()) {
            Toast.makeText(this, R.string.estado_no_encontrado, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra(WebViewActivity.EXTRA_URL, "http://$ip/")
        startActivity(intent)
    }

    private fun abrirReconfiguracionWifi() {
        Toast.makeText(
            this,
            R.string.ayuda_reconfigurar,
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra(WebViewActivity.EXTRA_URL, "http://$IP_MODO_AP/")
        startActivity(intent)
    }

    // ---------------------------------------------------------------
    // Estados visuales
    // ---------------------------------------------------------------

    private fun mostrarEstadoBuscando() {
        binding.progresoBuscando.visibility = android.view.View.VISIBLE
        binding.puntoEstado.visibility = android.view.View.GONE
        binding.txtEstado.text = getString(R.string.estado_buscando)
        binding.txtAyuda.visibility = android.view.View.GONE
        binding.txtDispositivoNombre.text = prefs.nombreDispositivo ?: getString(R.string.dispositivo_sin_datos)
        binding.btnAbrirPanel.isEnabled = false
    }

    private fun mostrarEstadoConectado(nombre: String, ip: String) {
        binding.progresoBuscando.visibility = android.view.View.GONE
        binding.puntoEstado.visibility = android.view.View.VISIBLE
        binding.puntoEstado.background.setTint(ContextCompat.getColor(this, R.color.verde_ok))
        binding.txtEstado.text = getString(R.string.estado_conectado)
        binding.txtAyuda.visibility = android.view.View.GONE
        binding.txtDispositivoNombre.text = "$nombre  ·  $ip"
        binding.btnAbrirPanel.isEnabled = true
    }

    private fun mostrarEstadoNoEncontrado() {
        binding.progresoBuscando.visibility = android.view.View.GONE
        binding.puntoEstado.visibility = android.view.View.VISIBLE
        binding.puntoEstado.background.setTint(ContextCompat.getColor(this, R.color.rojo_error))
        binding.txtEstado.text = getString(R.string.estado_no_encontrado)
        binding.txtAyuda.visibility = android.view.View.VISIBLE
        binding.txtDispositivoNombre.text = getString(R.string.dispositivo_sin_datos)
        binding.btnAbrirPanel.isEnabled = false
    }
}
