package com.example.projectohola // <- NO BORRES TU PACKAGE ORIGINAL, DÉJALO TAL CUAL ESTÁ EN TU ARCHIVO

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.LinkedList

class MainActivity : AppCompatActivity() {

    private var puntos = 0
    private var maxCps = 0

    private val clickTimestamps = LinkedList<Long>()

    private lateinit var tvPuntos: TextView
    private lateinit var tvCps: TextView
    private lateinit var tvMaxCps: TextView
    private lateinit var ivMascota: ImageView
    private lateinit var tvMensaje: TextView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateCpsRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlazar los elementos de la interfaz por su ID
        tvPuntos = findViewById(R.id.tvPuntos)
        tvCps = findViewById(R.id.tvCps)
        tvMaxCps = findViewById(R.id.tvMaxCps)
        ivMascota = findViewById(R.id.ivMascota)
        tvMensaje = findViewById(R.id.tvMensaje)

        // Acción al hacer clic en la mascota
        ivMascota.setOnClickListener {
            val ahora = SystemClock.uptimeMillis()
            clickTimestamps.add(ahora)

            puntos++
            tvPuntos.text = "Puntos: $puntos"
            tvMensaje.text = "¡Eso estuvo super guay! (+1)"

            actualizarCps()
        }

        // Definir el bucle en segundo plano para actualizar el CPS
        updateCpsRunnable = object : Runnable {
            override fun run() {
                actualizarCps()
                handler.postDelayed(this, 100) // Se ejecuta cada 100 milisegundos
            }
        }

        handler.post(updateCpsRunnable)
    }

    private fun actualizarCps() {
        val ahora = SystemClock.uptimeMillis()

        // Eliminar los clicks que ocurrieron hace más de 1 segundo (1000 ms)
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

    override fun onDestroy() {
        super.onDestroy()
        // Detener el handler cuando la app se cierre para evitar fugas de memoria
        if (::updateCpsRunnable.isInitialized) {
            handler.removeCallbacks(updateCpsRunnable)
        }
    }
}