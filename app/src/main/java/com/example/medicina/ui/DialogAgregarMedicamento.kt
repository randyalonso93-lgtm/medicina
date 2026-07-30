package com.example.medicina.ui


import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.medicina.R
import com.example.medicina.data.Medicamento
import java.util.Calendar
import android.view.View

class DialogAgregarMedicamento(
    private val contexto: Context,
    private val medicamentoExistente: Medicamento? = null,
    @Suppress("UNUSED_LAMBDA_EXPRESSION")
    private val onGuardar: (Medicamento) -> Unit
) {

    private lateinit var tilNombre: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var tilDosis: TextInputLayout
    private lateinit var etDosis: TextInputEditText
    private lateinit var tilDescripcion: TextInputLayout
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var tvHoraSeleccionada: TextView
    private lateinit var tilStock: TextInputLayout
    private lateinit var etStock: TextInputEditText
    private lateinit var tilStockMinimo: TextInputLayout
    private lateinit var etStockMinimo: TextInputEditText

    private var horaSeleccionada: String = "08:00"

    fun mostrar() {
        val builder = MaterialAlertDialogBuilder(contexto)

        val raiz = LinearLayout(contexto).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 16)
        }

        // --- Nombre ---
        tilNombre = TextInputLayout(contexto).apply {
            hint = contexto.getString(R.string.nombre_medicamento)
            isCounterEnabled = false
        }
        etNombre = TextInputEditText(tilNombre.context)
        tilNombre.addView(etNombre)
        raiz.addView(tilNombre)

        // Espaciado
        raiz.addView(espaciador(8))

        // --- Dosis ---
        tilDosis = TextInputLayout(contexto).apply {
            hint = contexto.getString(R.string.dosis)
        }
        etDosis = TextInputEditText(tilDosis.context)
        tilDosis.addView(etDosis)
        raiz.addView(tilDosis)

        raiz.addView(espaciador(8))

        // --- Descripción ---
        tilDescripcion = TextInputLayout(contexto).apply {
            hint = contexto.getString(R.string.descripcion)
        }
        etDescripcion = TextInputEditText(tilDescripcion.context)
        etDescripcion.minLines = 2
        etDescripcion.maxLines = 4
        tilDescripcion.addView(etDescripcion)
        raiz.addView(tilDescripcion)

        raiz.addView(espaciador(12))

        // --- Hora ---
        tvHoraSeleccionada = TextView(contexto).apply {
            text = "${contexto.getString(R.string.hora)}: $horaSeleccionada"
            setTextColor(contexto.getColor(R.color.md_theme_on_surface))
            textSize = 16f
            setPadding(0, 12, 0, 12)
            setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_recent_history, 0, 0, 0
            )
            compoundDrawablePadding = 12
            isClickable = true
            isFocusable = true
        }
        tvHoraSeleccionada.setOnClickListener { mostrarSelectorHora() }
        raiz.addView(tvHoraSeleccionada)

        raiz.addView(espaciador(8))

        // --- Stock actual ---
        tilStock = TextInputLayout(contexto).apply {
            hint = contexto.getString(R.string.stock_actual)
        }
        etStock = TextInputEditText(tilStock.context)
        etStock.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        tilStock.addView(etStock)
        raiz.addView(tilStock)

        raiz.addView(espaciador(8))

        // --- Stock mínimo para alerta ---
        tilStockMinimo = TextInputLayout(contexto).apply {
            hint = contexto.getString(R.string.stock_minimo)
        }
        etStockMinimo = TextInputEditText(tilStockMinimo.context)
        etStockMinimo.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        tilStockMinimo.addView(etStockMinimo)
        raiz.addView(tilStockMinimo)

        // Pre-llenar si estamos editando
        if (medicamentoExistente != null) {
            etNombre.setText(medicamentoExistente.nombre)
            etDosis.setText(medicamentoExistente.dosis)
            etDescripcion.setText(medicamentoExistente.descripcion)
            horaSeleccionada = medicamentoExistente.hora
            tvHoraSeleccionada.text = "${contexto.getString(R.string.hora)}: ${medicamentoExistente.hora}"
            etStock.setText(medicamentoExistente.stockActual.toString())
            etStockMinimo.setText(medicamentoExistente.stockMinimo.toString())
        } else {
            etStock.setText("30")
            etStockMinimo.setText("5")
        }

        val titulo = if (medicamentoExistente != null) "Editar medicamento" else contexto.getString(R.string.agregar)

        builder.setTitle(titulo)
        builder.setView(raiz)
        builder.setPositiveButton(R.string.guardar, null)
        builder.setNegativeButton(R.string.cancelar, null)

        val dialog = builder.create()
        dialog.show()

        // Override del botón positivo para validar antes de cerrar
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val dosis = etDosis.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val stock = etStock.text.toString().toIntOrNull() ?: 0
            val stockMin = etStockMinimo.text.toString().toIntOrNull() ?: 5

            if (nombre.isBlank() || dosis.isBlank()) {
                tilNombre.error = if (nombre.isBlank()) "Requerido" else null
                tilDosis.error = if (dosis.isBlank()) "Requerido" else null
                return@setOnClickListener
            }

            tilNombre.error = null
            tilDosis.error = null

            val medicamento = Medicamento(
                id = medicamentoExistente?.id ?: 0,
                nombre = nombre,
                dosis = dosis,
                descripcion = descripcion,
                hora = horaSeleccionada,
                stockActual = stock,
                stockMinimo = stockMin,
                activo = true,
                fechaCreacion = medicamentoExistente?.fechaCreacion ?: System.currentTimeMillis()
            )

            onGuardar(medicamento)
            dialog.dismiss()
        }
    }

    private fun mostrarSelectorHora() {
        val partes = horaSeleccionada.split(":")
        val hora = partes.getOrNull(0)?.toIntOrNull() ?: 8
        val minuto = partes.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            contexto,
            { _, h, m ->
                horaSeleccionada = String.format("%02d:%02d", h, m)
                tvHoraSeleccionada.text = "${contexto.getString(R.string.hora)}: $horaSeleccionada"
            },
            hora,
            minuto,
            true
        ).show()
    }

    private fun espaciador(dp: Int): View {
        return View(contexto).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp
            )
        }
    }
}