package com.example.projectohola

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.LinkedList

class MainActivity : AppCompatActivity() {

    private var puntos = 0
    private var maxCps = 0
    private var felicidad = 100

    private val clickTimestamps = LinkedList<Long>()

    private lateinit var tvPuntos: TextView
    private lateinit var tvCps: TextView
    private lateinit var tvMaxCps: TextView
    private lateinit var tvFelicidadEtiqueta: TextView
    private lateinit var pbFelicidad: ProgressBar
    private lateinit var ivMascota: ImageView
    private lateinit var tvMensaje: TextView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateCpsRunnable: Runnable
    private lateinit var decayFelicidadRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlazar los componentes con el XML
        tvPuntos = findViewById(R.id.tvPuntos)
        tvCps = findViewById(R.id.tvCps)
        tvMaxCps = findViewById(R.id.tvMaxCps)
        tvFelicidadEtiqueta = findViewById(R.id.tvFelicidadEtiqueta)
        pbFelicidad = findViewById(R.id.pbFelicidad)
        ivMascota = findViewById(R.id.ivMascota)
        tvMensaje = findViewById(R.id.tvMensaje)

        // Evento al tocar la mascota
        ivMascota.setOnClickListener {
            val ahora = SystemClock.uptimeMillis()
            clickTimestamps.add(ahora)

            puntos++
            tvPuntos.text = "Puntos: $puntos"

            // Recuperar felicidad al tocar
            if (felicidad < 100) {
                felicidad = (felicidad + 10).coerceAtMost(100)
                actualizarFelicidadUI()
            }

            if (felicidad > 50) {
                tvMensaje.text = "¡Eso se sintió genial! (+1)"
            } else {
                tvMensaje.text = "¡Gracias por prestarme atención! (+1)"
            }

            actualizarCps()
        }

        // Bucle para el CPS
        updateCpsRunnable = object : Runnable {
            override fun run() {
                actualizarCps()
                handler.postDelayed(this, 100)
            }
        }
        handler.post(updateCpsRunnable)

        // Bucle para la baja de felicidad (cada 1 segundo / 1000 ms)
        decayFelicidadRunnable = object : Runnable {
            override fun run() {
                if (felicidad > 0) {
                    felicidad -= 5
                    if (felicidad < 0) felicidad = 0
                    actualizarFelicidadUI()
                    evaluarEstadoMascota()
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(decayFelicidadRunnable, 1000)
    }

    private fun actualizarCps() {
        val ahora = SystemClock.uptimeMillis()

        while (clickTimestamps.isNotEmpty() && ahora - clickTimestamps.first > 1000) {
            clickTimestamps.removeFirst()
        }

        val cpsActual = clickTimestamps.size

        if (cpsActual > maxCps) {
            maxCps = cpsActual
            tvMaxCps.text = "Máx CPS: $maxCps"
        }

        tvCps.text = "CPS: $cpsActual"
    }

    private fun actualizarFelicidadUI() {
        pbFelicidad.progress = felicidad
        tvFelicidadEtiqueta.text = "Felicidad: $felicidad%"
    }

    private fun evaluarEstadoMascota() {
        when {
            felicidad == 0 -> {
                tvMensaje.text = "😴 Me dormí de aburrimiento..."
            }
            felicidad <= 25 -> {
                tvMensaje.text = "😭 ¡Tengo mucha hambre y estoy solo!"
            }
            felicidad <= 50 -> {
                tvMensaje.text = "🙁 Me estoy aburriendo... ¡Tócame!"
            }
            felicidad <= 75 -> {
                tvMensaje.text = "😐 ¿Seguimos jugando?"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::updateCpsRunnable.isInitialized) {
            handler.removeCallbacks(updateCpsRunnable)
        }
        if (::decayFelicidadRunnable.isInitialized) {
            handler.removeCallbacks(decayFelicidadRunnable)
        }
    }
}