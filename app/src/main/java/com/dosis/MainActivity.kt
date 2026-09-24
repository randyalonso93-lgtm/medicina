package com.dosis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dosis.data.Medicamento
import com.dosis.ui.MedicamentoAdapter
import com.dosis.ui.MedicamentoViewModel
import com.dosis.util.BackupHelper
import com.dosis.util.NotificacionHelper
import com.dosis.util.PreferenciasPermisos
import com.dosis.util.PreferenciasTema
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MedicamentoAdapter

    private val viewModel: MedicamentoViewModel by viewModels { MedicamentoViewModel.Factory }

    private val permisoNotificacion = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) {
            Toast.makeText(this, getString(R.string.msj_sin_permiso_notif), Toast.LENGTH_LONG).show()
        }
    }
    private val intentOptimizacion = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.reprogramarTodasLasAlarmas()
    }
    private val intentAlarmaExacta = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.reprogramarTodasLasAlarmas()
        mostrarAvisoAlarmaExacta()
    }

    private val crearDocumento = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val ok = BackupHelper.exportar(this@MainActivity, uri)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        if (ok) R.string.msj_backup_ok else R.string.msj_backup_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private val abrirDocumento = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val ok = BackupHelper.importar(this@MainActivity, uri)
                if (ok) {
                    Toast.makeText(this@MainActivity, R.string.msj_restaurar_ok, Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(this@MainActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, R.string.msj_restaurar_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        // Resumen diario (idempotente: la alarma se reemplaza sola)
        NotificacionHelper(applicationContext).programarResumenDiario()

        solicitarPermisos()
        viewModel.iniciar()

        initRecyclerView()
        initFab()
        observarEstado()
        observarMensajes()

        manejarIntento(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntento(intent)
    }

    /** Abre el formulario del medicamento si la notificación del recordatorio lo solicitó. */
    private fun manejarIntento(intent: Intent?) {
        val medicamentoId = intent?.getIntExtra(FormMedicamentoActivity.EXTRA_MEDICAMENTO_ID, -1) ?: return
        if (medicamentoId > 0) {
            val i = Intent(this, FormMedicamentoActivity::class.java).apply {
                putExtra(FormMedicamentoActivity.EXTRA_MEDICAMENTO_ID, medicamentoId)
            }
            startActivity(i)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refrescarProximo()
    }

    private fun solicitarPermisos() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permisoNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    intentAlarmaExacta.launch(
                        Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        ).apply {
                            data = android.net.Uri.parse("package:$packageName")
                        }
                    )
                } catch (_: Exception) {
                    intentAlarmaExacta.launch(
                        Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        ).apply {
                            data = android.net.Uri.parse("package:$packageName")
                        }
                    )
                }
            }
        }

        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)
            && !PreferenciasPermisos.bateriaPreguntada(this)
        ) {
            PreferenciasPermisos.marcarBateriaPreguntada(this)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.bateria_titulo)
                .setMessage(R.string.bateria_desc)
                .setPositiveButton(R.string.aceptar) { _, _ -> lanzarSolicitudBateria() }
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }

    private fun lanzarSolicitudBateria() {
        try {
            intentOptimizacion.launch(
                Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                ).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
            intentOptimizacion.launch(
                Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                ).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
            )
        }
    }

    private fun mostrarAvisoAlarmaExacta() {
        val mensaje = if (soportaAlarmaExacta()) {
            getString(R.string.msj_alarma_exacta_ok)
        } else {
            getString(R.string.msj_alarma_exacta_denegada)
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun soportaAlarmaExacta(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val itemBuscar = menu.findItem(R.id.action_buscar)
        val searchView = itemBuscar?.actionView as? SearchView
        searchView?.apply {
            queryHint = getString(R.string.buscar)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    viewModel.setBusqueda(query ?: "")
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.setBusqueda(newText ?: "")
                    return true
                }
            })
        }

        val modoActual = PreferenciasTema.modoActual(this)
        menu.findItem(R.id.action_tema_sistema)?.isChecked = modoActual == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        menu.findItem(R.id.action_tema_claro)?.isChecked = modoActual == AppCompatDelegate.MODE_NIGHT_NO
        menu.findItem(R.id.action_tema_oscuro)?.isChecked = modoActual == AppCompatDelegate.MODE_NIGHT_YES
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_historial -> {
                startActivity(Intent(this, HistorialActivity::class.java))
                true
            }
            R.id.action_estadisticas -> {
                startActivity(Intent(this, EstadisticasActivity::class.java))
                true
            }
            R.id.action_tema_sistema -> { cambiarTema(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); true }
            R.id.action_tema_claro -> { cambiarTema(AppCompatDelegate.MODE_NIGHT_NO); true }
            R.id.action_tema_oscuro -> { cambiarTema(AppCompatDelegate.MODE_NIGHT_YES); true }
            R.id.action_exportar -> { crearDocumento.launch("dosis_backup_${System.currentTimeMillis()}.db"); true }
            R.id.action_importar -> { abrirDocumento.launch(arrayOf("application/octet-stream", "*/*")); true }
            R.id.action_limpiar_historial -> { confirmarLimpiarHistorial(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun cambiarTema(modo: Int) {
        PreferenciasTema.guardar(this, modo)
    }

    private fun confirmarLimpiarHistorial() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.limpiar_historial)
            .setMessage(R.string.confirmar_limpiar_historial)
            .setPositiveButton(R.string.si) { _, _ ->
                viewModel.limpiarHistorialAntiguo()
                Toast.makeText(this, R.string.msj_historial_limpio, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun initRecyclerView() {
        recyclerView = findViewById(R.id.recyclerMedicamentos)
        adapter = MedicamentoAdapter(
            onTomarDosis = { med -> viewModel.tomarDosis(med) },
            onAjustarStock = { med -> mostrarDialogoAjustarStock(med) },
            onEditar = { med -> lanzarFormulario(med) },
            onEliminar = { med -> confirmarEliminar(med) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun initFab() {
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAgregar)
            .setOnClickListener { lanzarFormulario(null) }
    }

    private fun lanzarFormulario(medicamento: Medicamento?) {
        val intent = Intent(this, FormMedicamentoActivity::class.java)
        if (medicamento != null) {
            intent.putExtra(FormMedicamentoActivity.EXTRA_MEDICAMENTO_ID, medicamento.id)
        }
        startActivity(intent)
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.medicamentos.collect { lista ->
                    adapter.submitList(lista)

                    val layoutVacio = findViewById<android.widget.LinearLayout>(R.id.layoutVacio)
                    if (lista.isEmpty()) {
                        layoutVacio.visibility = android.view.View.VISIBLE
                        recyclerView.visibility = android.view.View.GONE
                    } else {
                        layoutVacio.visibility = android.view.View.GONE
                        recyclerView.visibility = android.view.View.VISIBLE
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.proximoRecordatorio.collect { texto ->
                    findViewById<TextView>(R.id.tvProximoRecordatorio).text = texto
                }
            }
        }
    }

    private fun observarMensajes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mensajes.collect { mensaje ->
                    Toast.makeText(this@MainActivity, mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ==================== ACCIONES CRUD ====================

    private fun confirmarEliminar(med: Medicamento) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.eliminar)
            .setMessage(R.string.confirmar_eliminar)
            .setPositiveButton(R.string.si) { _, _ ->
                viewModel.eliminar(med)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    // ==================== STOCK ====================

    private fun mostrarDialogoAjustarStock(med: Medicamento) {
        val input = TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            hint = getString(R.string.hint_cantidad_ajuste)
            setText("1")
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_ajustar_stock)
            .setMessage(getString(R.string.msg_ajustar_stock, med.nombre))
            .setView(input)
            .setPositiveButton(R.string.aceptar) { _, _ ->
                val delta = input.text.toString().toIntOrNull() ?: 1
                if (delta != 0) {
                    if (med.stockActual + delta < 0) {
                        Toast.makeText(this, R.string.msj_stock_negativo, Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.ajustarStock(med, delta)
                    }
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }
}