package com.example.medicina


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.medicina.data.AppDatabase
import com.example.medicina.data.Medicamento
import com.example.medicina.data.RegistroToma
import com.example.medicina.ui.DialogAgregarMedicamento
import com.example.medicina.ui.MedicamentoAdapter
import com.example.medicina.util.NotificacionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MedicamentoAdapter
    private lateinit var notifHelper: NotificacionHelper

    private val dao by lazy { AppDatabase.obtenerInstancia(this).medicamentoDao() }
    private val registroDao by lazy { AppDatabase.obtenerInstancia(this).registroTomaDao() }

    // Registro del permiso de notificación (Android 13+)
    private val permisoNotificacion = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) {
            Toast.makeText(this, "Sin permiso de notificaciones los recordatorios no sonarán", Toast.LENGTH_LONG).show()
        }
    }
    private val intentOptimizacion = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Al volver, re-programar todo por si acaso
        reprogramarTodasLasAlarmas()
    }
    private val intentAlarmaExacta = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Al volver de ajustes, re-programar todas las alarmas
        reprogramarTodasLasAlarmas()
    }


        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notifHelper = NotificacionHelper(this)

        // Solicitar permiso de notificaciones en Android 13+
        solicitarPermisos()

        initRecyclerView()
        initFab()
        observarMedicamentos()
        actualizarProximoRecordatorio()
    }

    private fun solicitarPermisos() {
        // Notificaciones (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permisoNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Alarma exacta (Android 12+) — via ajustes del sistema
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    intentAlarmaExacta.launch(
                        android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        ).apply {
                            data = android.net.Uri.parse("package:$packageName")
                        }
                    )
                } catch (_: Exception) {
                    intentAlarmaExacta.launch(
                        android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        ).apply {
                            data = android.net.Uri.parse("package:$packageName")
                        }
                    )
                }
            }
        }

        // Optimización de batería — pedir que no restrinja la app
        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                intentOptimizacion.launch(
                    android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    ).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                )
            } catch (_: Exception) { }
        }
    }
    private fun reprogramarTodasLasAlarmas() {
        lifecycleScope.launch(Dispatchers.IO) {
            val medicamentos = dao.obtenerTodosUnaVez()
            for (med in medicamentos) {
                if (med.activo) {
                    notifHelper.programarAlarma(med.id, med.hora)
                }
            }
        }
    }
    private fun initRecyclerView() {
        recyclerView = findViewById(R.id.recyclerMedicamentos)
        adapter = MedicamentoAdapter(
            onTomarDosis = { med -> tomarDosis(med) },
            onReabastecer = { med -> reabastecer(med) },
            onEditar = { med -> editarMedicamento(med) },
            onEliminar = { med -> confirmarEliminar(med) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun initFab() {
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAgregar)
            .setOnClickListener {
                DialogAgregarMedicamento(this) { medicamento ->
                    guardarMedicamento(medicamento)
                }.mostrar()
            }

    }

    private fun observarMedicamentos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.obtenerTodos().collect { lista ->
                    adapter.submitList(lista)

                    // Mostrar/ocultar estado vacío
                    val layoutVacio = findViewById<android.widget.LinearLayout>(R.id.layoutVacio)
                    if (lista.isEmpty()) {
                        layoutVacio.visibility = android.view.View.VISIBLE
                        recyclerView.visibility = android.view.View.GONE
                    } else {
                        layoutVacio.visibility = android.view.View.GONE
                        recyclerView.visibility = android.view.View.VISIBLE
                    }

                    actualizarProximoRecordatorio()
                }
            }
        }
    }

    /**
     * Calcula y muestra cuál es el próximo recordatorio del día.
     */
    private fun actualizarProximoRecordatorio() {
        lifecycleScope.launch(Dispatchers.IO) {
            val medicamentos = dao.obtenerTodosUnaVez()
            val ahora = Calendar.getInstance()
            val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            val horaActual = formatoHora.format(ahora.time)

            val proximo = medicamentos
                .filter { it.hora >= horaActual }
                .minByOrNull { it.hora }

            val texto = if (proximo != null) {
                getString(R.string.proximo, "${proximo.hora} — ${proximo.nombre}")
            } else {
                // Buscar el primero del día siguiente (el más temprano)
                val primeroManana = medicamentos.minByOrNull { it.hora }
                if (primeroManana != null) {
                    "Mañana: ${primeroManana.hora} — ${primeroManana.nombre}"
                } else {
                    getString(R.string.sin_proximo)
                }
            }

            runOnUiThread {
                findViewById<android.widget.TextView>(R.id.tvProximoRecordatorio).text = texto
            }
        }
    }

    // ==================== ACCIONES CRUD ====================

    private fun guardarMedicamento(medicamento: Medicamento) {
        lifecycleScope.launch(Dispatchers.IO) {
            val esNuevo = medicamento.id == 0

            if (esNuevo) {
                val id = dao.insertar(medicamento).toInt()
                notifHelper.programarAlarma(id, medicamento.hora)
            } else {
                dao.actualizar(medicamento)
                notifHelper.cancelarAlarma(medicamento.id)
                notifHelper.programarAlarma(medicamento.id, medicamento.hora)
            }

            runOnUiThread {
                val msg = if (esNuevo) "Medicamento guardado" else "Medicamento actualizado"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun editarMedicamento(med: Medicamento) {
        DialogAgregarMedicamento(this, med) { medicamentoActualizado ->
            guardarMedicamento(medicamentoActualizado)
        }.mostrar()
    }

    private fun confirmarEliminar(med: Medicamento) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.eliminar)
            .setMessage(R.string.confirmar_eliminar)
            .setPositiveButton(R.string.si) { _, _ ->
                eliminarMedicamento(med)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun eliminarMedicamento(med: Medicamento) {
        lifecycleScope.launch(Dispatchers.IO) {
            notifHelper.cancelarAlarma(med.id)
            dao.desactivar(med.id)
            registroDao.eliminarPorMedicamento(med.id)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Medicamento eliminado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==================== STOCK ====================

    private fun tomarDosis(med: Medicamento) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (med.stockActual <= 0) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Sin stock de ${med.nombre}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            // Registrar la toma
            registroDao.insertar(
                RegistroToma(
                    medicamentoId = med.id,
                    fechaHora = System.currentTimeMillis(),
                    tomado = true
                )
            )

            // Restar 1 del stock
            dao.actualizarStock(med.id, -1)

            // Verificar alertas de stock
            val medActualizado = dao.obtenerPorId(med.id)
            if (medActualizado != null) {
                if (medActualizado.stockActual <= 0) {
                    notifHelper.mostrarAlertaSinStock(medActualizado.nombre)
                } else if (medActualizado.stockActual <= medActualizado.stockMinimo) {
                    notifHelper.mostrarAlertaStockBajo(
                        medActualizado.nombre,
                        medActualizado.stockActual
                    )
                }
            }

            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "Dosis de ${med.nombre} registrada",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun reabastecer(med: Medicamento) {
        lifecycleScope.launch(Dispatchers.IO) {
            dao.actualizarStock(med.id, 1)
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "+1 unidad de ${med.nombre}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}