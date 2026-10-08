package com.example.projectohola

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.LinkedList

class MainActivity : AppCompatActivity() {

    private var puntos = 0
    private var maxCps = 0
    private var felicidad = 100
    private var contadorToquesQte = 0

    private var qteActivo = false
    private var qteCompletado = false

    private val clickTimestamps = LinkedList<Long>()

    private lateinit var mainLayout: ConstraintLayout
    private lateinit var tvPuntos: TextView
    private lateinit var tvCps: TextView
    private lateinit var tvMaxCps: TextView
    private lateinit var tvFelicidadEtiqueta: TextView
    private lateinit var pbFelicidad: ProgressBar
    private lateinit var tvQteBanner: TextView
    private lateinit var ivMascota: ImageView
    private lateinit var tvMensaje: TextView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateCpsRunnable: Runnable
    private lateinit var decayFelicidadRunnable: Runnable
    private lateinit var alertaCincoMinutosRunnable: Runnable
    private lateinit var qteTimerRunnable: Runnable

    private var alertaActiva = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainLayout = findViewById(R.id.mainLayout)
        tvPuntos = findViewById(R.id.tvPuntos)
        tvCps = findViewById(R.id.tvCps)
        tvMaxCps = findViewById(R.id.tvMaxCps)
        tvFelicidadEtiqueta = findViewById(R.id.tvFelicidadEtiqueta)
        pbFelicidad = findViewById(R.id.pbFelicidad)
        tvQteBanner = findViewById(R.id.tvQteBanner)
        ivMascota = findViewById(R.id.ivMascota)
        tvMensaje = findViewById(R.id.tvMensaje)

        cargarDatos()

        ivMascota.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            // Animación de escala rápida al presionar
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).withEndAction {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50)
            }

            val ahora = SystemClock.uptimeMillis()
            clickTimestamps.add(ahora)

            puntos++
            contadorToquesQte++

            if (contadorToquesQte >= 50 && !qteActivo) {
                iniciarQTE()
            }

            tvPuntos.text = "Puntos: $puntos"

            if (felicidad < 100) {
                felicidad = (felicidad + 10).coerceAtMost(100)
                actualizarFelicidadUI()
            }

            if (felicidad > 0 && alertaActiva) {
                detenerAlertasInternas()
            }

            if (!qteActivo) {
                if (felicidad > 50) {
                    tvMensaje.text = "¡Eso se sintió genial! (+1)"
                } else {
                    tvMensaje.text = "¡Gracias por prestarme atención! (+1)"
                }
            }

            actualizarCps()
            guardarDatos()
        }

        updateCpsRunnable = object : Runnable {
            override fun run() {
                actualizarCps()
                handler.postDelayed(this, 100)
            }
        }
        handler.post(updateCpsRunnable)

        decayFelicidadRunnable = object : Runnable {
            override fun run() {
                if (felicidad > 0) {
                    felicidad -= 5
                    if (felicidad < 0) felicidad = 0
                    actualizarFelicidadUI()
                    if (!qteActivo) evaluarEstadoMascota()
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(decayFelicidadRunnable, 1000)

        alertaCincoMinutosRunnable = object : Runnable {
            override fun run() {
                if (felicidad == 0) {
                    Toast.makeText(
                        this@MainActivity,
                        "⚠️ ¡Atención! Tu mascota lleva mucho tiempo desatendida.",
                        Toast.LENGTH_LONG
                    ).show()

                    handler.postDelayed(this, 5 * 60 * 1000L)
                }
            }
        }

        qteTimerRunnable = Runnable {
            if (qteActivo) {
                finalizarQTE(exito = false)
            }
        }
    }

    private fun iniciarQTE() {
        contadorToquesQte = 0
        qteActivo = true
        qteCompletado = false

        mainLayout.setBackgroundColor(Color.parseColor("#FFEBEE")) // Fondo rojo suave
        tvQteBanner.visibility = View.VISIBLE
        tvMensaje.text = "⚡ ¡DALE RÁPIDO! LLEGA A 12 CPS ⚡"

        handler.postDelayed(qteTimerRunnable, 3000L)
    }

    private fun finalizarQTE(exito: Boolean) {
        qteActivo = false
        handler.removeCallbacks(qteTimerRunnable)

        mainLayout.setBackgroundColor(Color.WHITE)
        tvQteBanner.visibility = View.GONE

        if (exito) {
            puntos += 200
            tvPuntos.text = "Puntos: $puntos"
            tvMensaje.text = "🎉 ¡DESAFÍO COMPLETADO! +200 Puntos Extra"
            Toast.makeText(this, "🏆 ¡DESAFÍO COMPLETADO! +200 Puntos", Toast.LENGTH_SHORT).show()
            guardarDatos()
        } else {
            tvMensaje.text = "❌ ¡Tiempo agotado! No alcanzaste 12 CPS."
            Toast.makeText(this, "QTE Fallado 😅", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarCps() {
        val ahora = SystemClock.uptimeMillis()

        while (clickTimestamps.isNotEmpty() && ahora - clickTimestamps.first > 1000) {
            clickTimestamps.removeFirst()
        }

        val cpsActual = clickTimestamps.size

        if (qteActivo && !qteCompletado && cpsActual >= 12) {
            qteCompletado = true
            finalizarQTE(exito = true)
        }

        if (cpsActual > maxCps) {
            maxCps = cpsActual
            tvMaxCps.text = "Máx CPS: $maxCps"
            guardarDatos()
        }

        tvCps.text = "CPS: $cpsActual"
    }

    private fun actualizarFelicidadUI() {
        pbFelicidad.progress = felicidad
        tvFelicidadEtiqueta.text = "Felicidad: $felicidad%"
        actualizarExpresionMascota()
    }

    private fun actualizarExpresionMascota() {
        when {
            felicidad == 0 -> {
                ivMascota.setImageResource(R.drawable.mascota_dormida)
            }
            felicidad <= 25 -> {
                ivMascota.setImageResource(R.drawable.mascota_triste)
            }
            felicidad <= 75 -> {
                ivMascota.setImageResource(R.drawable.mascota_normal)
            }
            else -> {
                ivMascota.setImageResource(R.drawable.mascota_feliz)
            }
        }
    }

    private fun evaluarEstadoMascota() {
        when {
            felicidad == 0 -> {
                tvMensaje.text = "😴 Me dormí de aburrimiento..."
                iniciarAlertasInternas()
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

    private fun iniciarAlertasInternas() {
        if (!alertaActiva) {
            alertaActiva = true
            handler.post(alertaCincoMinutosRunnable)
        }
    }

    private fun detenerAlertasInternas() {
        alertaActiva = false
        handler.removeCallbacks(alertaCincoMinutosRunnable)
    }

    private fun guardarDatos() {
        val prefs = getSharedPreferences("MascotaPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("puntos", puntos)
        editor.putInt("maxCps", maxCps)
        editor.apply()
    }

    private fun cargarDatos() {
        val prefs = getSharedPreferences("MascotaPrefs", Context.MODE_PRIVATE)
        puntos = prefs.getInt("puntos", 0)
        maxCps = prefs.getInt("maxCps", 0)

        tvPuntos.text = "Puntos: $puntos"
        tvMaxCps.text = "Máx CPS: $maxCps"
    }

    override fun onDestroy() {
        super.onDestroy()
        guardarDatos()
        if (::updateCpsRunnable.isInitialized) {
            handler.removeCallbacks(updateCpsRunnable)
        }
        if (::decayFelicidadRunnable.isInitialized) {
            handler.removeCallbacks(decayFelicidadRunnable)
        }
        if (::alertaCincoMinutosRunnable.isInitialized) {
            handler.removeCallbacks(alertaCincoMinutosRunnable)
        }
        if (::qteTimerRunnable.isInitialized) {
            handler.removeCallbacks(qteTimerRunnable)
        }
    }
}