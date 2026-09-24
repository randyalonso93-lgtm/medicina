package com.dosis

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dosis.data.Dosis
import com.dosis.data.Medicamento
import com.dosis.ui.FormMedicamentoViewModel
import com.dosis.util.FormatoHora
import com.dosis.util.ProgramacionDosis
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.Locale
import android.view.Gravity
/** Vista independiente de alta/edición de medicamento. */
class FormMedicamentoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MEDICAMENTO_ID = "medicamento_id"
    }

    private val viewModel: FormMedicamentoViewModel by viewModels { FormMedicamentoViewModel.Factory }

    private lateinit var toolbarFormulario: MaterialToolbar

    private lateinit var tilNombre: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var tilDosis: TextInputLayout
    private lateinit var etDosis: TextInputEditText
    private lateinit var tilUnidades: TextInputLayout
    private lateinit var etUnidades: TextInputEditText
    private lateinit var tilDescripcion: TextInputLayout
    private lateinit var etDescripcion: TextInputEditText

    private lateinit var contenedorHoras: LinearLayout
    private lateinit var txtAgregarHora: TextView

    private lateinit var spFrecuencia: MaterialAutoCompleteTextView
    private var opcionesFrecuencia = listOf<String>()
    private val frecuenciaPorOpcion = intArrayOf(
        Dosis.DIARIA, Dosis.SEMANAL, Dosis.CADA_X
    )

    private lateinit var contenedorDias: LinearLayout
    private lateinit var tilIntervalo: TextInputLayout
    private lateinit var etIntervalo: TextInputEditText
    private val diasPorId = HashMap<Int, Int>()

    private lateinit var tilStock: TextInputLayout
    private lateinit var etStock: TextInputEditText
    private lateinit var tilStockMinimo: TextInputLayout
    private lateinit var etStockMinimo: TextInputEditText

    private lateinit var btnGuardar: MaterialButton

    private val horas = mutableListOf<String>()

    private var frecuencia = Dosis.DIARIA
    private var diasSemana = ProgramacionDosis.LUN or ProgramacionDosis.MARTES or
            ProgramacionDosis.MIERCOLES or ProgramacionDosis.JUEVES or ProgramacionDosis.VIERNES

    private var medicamentoOriginal: Medicamento? = null
    private var dosisOriginales: List<Dosis> = emptyList()
    private var medicamentoCargado = false
    private var dosisCargadas = false
    private var datosAplicados = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_medicamento)

        val idEditar = intent.getIntExtra(EXTRA_MEDICAMENTO_ID, -1)

        vincularVistas()
        toolbarFormulario.title = getString(if (idEditar != -1) R.string.title_editar else R.string.agregar)
        toolbarFormulario.setNavigationOnClickListener { finish() }

        txtAgregarHora.setOnClickListener { mostrarSelectorHora(null) }
        btnGuardar.setOnClickListener { guardar() }

        if (idEditar != -1) {
            viewModel.cargar(idEditar)
            observarDatos()
        } else {
            etUnidades.setText("1")
            etStock.setText("30")
            etStockMinimo.setText("5")
            horas.add("08:00")
            renderHoras()
            cambiarFrecuencia(Dosis.DIARIA)
            seleccionarFrecuenciaEnDropdown(Dosis.DIARIA)
        }
    }

    private fun vincularVistas() {
        toolbarFormulario = findViewById(R.id.toolbarFormulario)

        tilNombre = findViewById(R.id.tilNombre)
        etNombre = findViewById(R.id.etNombre)
        tilDosis = findViewById(R.id.tilDosis)
        etDosis = findViewById(R.id.etDosis)
        tilUnidades = findViewById(R.id.tilUnidades)
        etUnidades = findViewById(R.id.etUnidades)
        tilDescripcion = findViewById(R.id.tilDescripcion)
        etDescripcion = findViewById(R.id.etDescripcion)

        contenedorHoras = findViewById(R.id.contenedorHoras)
        txtAgregarHora = findViewById(R.id.txtAgregarHora)

        spFrecuencia = findViewById(R.id.spFrecuencia)
        opcionesFrecuencia = resources.getStringArray(R.array.opciones_frecuencia).toList()
        spFrecuencia.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, opcionesFrecuencia)
        )
        spFrecuencia.setOnItemClickListener { _, _, posicion, _ ->
            cambiarFrecuencia(frecuenciaPorOpcion[posicion])
        }

        contenedorDias = findViewById(R.id.contenedorDias)
        tilIntervalo = findViewById(R.id.tilIntervalo)
        etIntervalo = findViewById(R.id.etIntervalo)

        tilStock = findViewById(R.id.tilStock)
        etStock = findViewById(R.id.etStock)
        tilStockMinimo = findViewById(R.id.tilStockMinimo)
        etStockMinimo = findViewById(R.id.etStockMinimo)

        btnGuardar = findViewById(R.id.btnGuardar)
        findViewById<MaterialButton>(R.id.btnCancelar).setOnClickListener { finish() }

        diasPorId[findViewById<TextView>(R.id.btnDiaLun).id] = ProgramacionDosis.LUN
        diasPorId[findViewById<TextView>(R.id.btnDiaMar).id] = ProgramacionDosis.MARTES
        diasPorId[findViewById<TextView>(R.id.btnDiaMie).id] = ProgramacionDosis.MIERCOLES
        diasPorId[findViewById<TextView>(R.id.btnDiaJue).id] = ProgramacionDosis.JUEVES
        diasPorId[findViewById<TextView>(R.id.btnDiaVie).id] = ProgramacionDosis.VIERNES
        diasPorId[findViewById<TextView>(R.id.btnDiaSab).id] = ProgramacionDosis.SABADO
        diasPorId[findViewById<TextView>(R.id.btnDiaDom).id] = ProgramacionDosis.DOMINGO

        diasPorId.forEach { (id, bit) ->
            findViewById<TextView>(id).setOnClickListener { marcarDia(bit) }
        }
        sincronizarDias()
    }

    private fun marcarDia(bit: Int) {
        val marcado = (diasSemana and bit) != 0
        if (marcado && (diasSemana and bit.inv()) == 0) return
        diasSemana = if (marcado) diasSemana and bit.inv() else diasSemana or bit
        sincronizarDias()
    }

    private fun sincronizarDias() {
        diasPorId.forEach { (id, bit) ->
            findViewById<TextView>(id).isSelected = (diasSemana and bit) != 0
        }
    }

    private fun observarDatos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.medicamento.collect { med ->
                    if (med != null) {
                        medicamentoOriginal = med
                        medicamentoCargado = true
                        aplicarSiListo()
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dosis.collect { lista ->
                    if (lista.isNotEmpty()) {
                        dosisOriginales = lista
                        dosisCargadas = true
                        aplicarSiListo()
                    }
                }
            }
        }
    }

    private fun aplicarSiListo() {
        if (!datosAplicados && medicamentoCargado && dosisCargadas) {
            datosAplicados = true
            aplicarDatos()
        }
    }

    private fun aplicarDatos() {
        val med = medicamentoOriginal ?: return
        etNombre.setText(med.nombre)
        etDosis.setText(med.dosis)
        etUnidades.setText(med.unidadesPorToma.toString())
        etDescripcion.setText(med.descripcion)
        etStock.setText(med.stockActual.toString())
        etStockMinimo.setText(med.stockMinimo.toString())

        val primera = dosisOriginales.firstOrNull()
        if (primera != null) {
            frecuencia = primera.frecuencia
            diasSemana = primera.diasSemana
            etIntervalo.setText(primera.intervaloDias.takeIf { it > 0 }?.toString() ?: "")
            horas.clear()
            horas.addAll(dosisOriginales.map { it.hora })
        } else {
            horas.clear()
            horas.add("08:00")
        }
        renderHoras()
        sincronizarDias()

        seleccionarFrecuenciaEnDropdown(frecuencia)
        cambiarFrecuencia(frecuencia)
    }

    private fun seleccionarFrecuenciaEnDropdown(frec: Int) {
        val posicion = frecuenciaPorOpcion.indexOf(frec)
        if (posicion >= 0 && opcionesFrecuencia.isNotEmpty()) {
            spFrecuencia.setText(opcionesFrecuencia[posicion], false)
        }
    }

    private fun cambiarFrecuencia(nueva: Int) {
        frecuencia = nueva
        contenedorDias.visibility = if (nueva == Dosis.SEMANAL) View.VISIBLE else View.GONE
        tilIntervalo.visibility = if (nueva == Dosis.CADA_X) View.VISIBLE else View.GONE
    }

    private fun renderHoras() {
        contenedorHoras.removeAllViews()
        horas.sorted().forEach { hora ->
            contenedorHoras.addView(filaHora(hora))
        }
    }

    private fun filaHora(hora: String): View {
        val colorPrimary = ContextCompat.getColor(this, R.color.md_theme_primary)
        val colorError = ContextCompat.getColor(this, R.color.md_theme_error)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)

            val etiqueta = TextView(context).apply {
                text = FormatoHora.a12(hora)
                textSize = 18f
                setTextColor(colorPrimary)
                isClickable = true
                isFocusable = true
                setOnClickListener { mostrarSelectorHora(hora) }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val botonEditar = TextView(context).apply {
                text = getString(R.string.editar)
                textSize = 14f
                setTextColor(colorPrimary)
                setPadding(8, 4, 8, 4)
                isClickable = true
                isFocusable = true
                setOnClickListener { mostrarSelectorHora(hora) }
            }
            val botonQuitar = TextView(context).apply {
                text = getString(R.string.quitar_hora)
                textSize = 14f
                setTextColor(colorError)
                setPadding(8, 4, 8, 4)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    horas.remove(hora)
                    renderHoras()
                }
            }
            addView(etiqueta)
            addView(botonEditar)
            addView(botonQuitar)
        }
    }

    private fun mostrarSelectorHora(existente: String?) {
        val referencia = existente ?: horas.firstOrNull() ?: "08:00"
        val partes = referencia.split(":")
        val hora24 = partes.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val minuto = partes.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val esPm = hora24 >= 12
        val hora12 = if (hora24 % 12 == 0) 12 else hora24 % 12

        val vista = layoutInflater.inflate(R.layout.dialog_selector_hora, null)
        val npHora = vista.findViewById<NumberPicker>(R.id.np_hora)
        val npMinuto = vista.findViewById<NumberPicker>(R.id.np_minuto)
        val npPeriodo = vista.findViewById<NumberPicker>(R.id.np_periodo)
        npHora.wrapSelectorWheel = false
        npMinuto.wrapSelectorWheel = false
        npPeriodo.wrapSelectorWheel = false
        npHora.minValue = 1
        npHora.maxValue = 12
        npHora.value = hora12
        npMinuto.minValue = 0
        npMinuto.maxValue = 59
        npMinuto.value = minuto
        npPeriodo.minValue = 0
        npPeriodo.maxValue = 1
        npPeriodo.displayedValues = arrayOf(
            getString(R.string.am),
            getString(R.string.pm)
        )
        npPeriodo.value = if (esPm) 1 else 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.elegir_hora)
            .setView(vista)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val horaSel = npHora.value % 12 + npPeriodo.value * 12
                val nueva = String.format(Locale.ROOT, "%02d:%02d", horaSel, npMinuto.value)
                if (existente == nueva) return@setPositiveButton
                if (horas.contains(nueva)) {
                    Toast.makeText(this, getString(R.string.msj_hora_repetida), Toast.LENGTH_SHORT).show()
                } else {
                    if (existente != null) {
                        horas.remove(existente)
                    }
                    horas.add(nueva)
                    renderHoras()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun guardar() {
        val nombre = etNombre.text.toString().trim()
        val dosis = etDosis.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val unidades = etUnidades.text.toString().toIntOrNull() ?: 1
        val stock = etStock.text.toString().toIntOrNull() ?: 0
        val stockMin = etStockMinimo.text.toString().toIntOrNull() ?: 5

        val horasLimpia = horas.distinct().sorted()
        val intervalo = etIntervalo.text.toString().toIntOrNull()
        var valido = true

        tilNombre.error = null
        tilDosis.error = null
        tilUnidades.error = null
        tilIntervalo.error = null

        if (nombre.isBlank()) {
            tilNombre.error = getString(R.string.requerido)
            valido = false
        }
        if (dosis.isBlank()) {
            tilDosis.error = getString(R.string.requerido)
            valido = false
        }
        if (unidades <= 0) {
            tilUnidades.error = getString(R.string.requerido)
            valido = false
        }
        if (horasLimpia.isEmpty()) {
            Toast.makeText(this, getString(R.string.msj_falta_hora), Toast.LENGTH_SHORT).show()
            valido = false
        }
        if (frecuencia == Dosis.SEMANAL && diasSemana == 0) {
            Toast.makeText(this, getString(R.string.msj_falta_dia), Toast.LENGTH_SHORT).show()
            valido = false
        }
        if (frecuencia == Dosis.CADA_X && (intervalo == null || intervalo < 1)) {
            tilIntervalo.error = getString(R.string.msj_intervalo_invalido)
            valido = false
        }
        if (!valido) return

        val med = Medicamento(
            id = medicamentoOriginal?.id ?: 0,
            nombre = nombre,
            dosis = dosis,
            descripcion = descripcion,
            stockActual = stock,
            stockMinimo = stockMin,
            unidadesPorToma = unidades,
            activo = true,
            fechaCreacion = medicamentoOriginal?.fechaCreacion ?: System.currentTimeMillis()
        )

        val dosisFinales = horasLimpia.map {
            Dosis(
                medicamentoId = med.id,
                hora = it,
                frecuencia = frecuencia,
                diasSemana = diasSemana,
                intervaloDias = if (frecuencia == Dosis.CADA_X) (intervalo ?: 1) else 0,
                fechaBase = 0
            )
        }

        lifecycleScope.launch {
            val msj = viewModel.guardar(med, dosisFinales)
            Toast.makeText(this@FormMedicamentoActivity, msj, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}