package com.dosis

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dosis.ui.HistorialAdapter
import com.dosis.ui.HistorialViewModel
import kotlinx.coroutines.launch

class HistorialActivity : AppCompatActivity() {

    private lateinit var recyclerHistorial: RecyclerView
    private lateinit var layoutVacio: View
    private lateinit var spinner: Spinner
    private lateinit var adapter: HistorialAdapter

    private val viewModel: HistorialViewModel by viewModels { HistorialViewModel.Factory }

    private var opcionesFiltro: List<Pair<Int, String>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        setSupportActionBar(findViewById(R.id.toolbarHistorial))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerHistorial = findViewById(R.id.recyclerHistorial)
        layoutVacio = findViewById(R.id.layoutHistorialVacio)
        spinner = findViewById(R.id.spinnerFiltro)
        adapter = HistorialAdapter()
        recyclerHistorial.layoutManager = LinearLayoutManager(this)
        recyclerHistorial.adapter = adapter

        cargarFiltro()
        observarHistorial()
    }

    private fun cargarFiltro() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.medicamentos.collect { medicamentos ->
                    val opciones = listOf(0 to getString(R.string.todos)) +
                            medicamentos.map { it.id to it.nombre }
                    opcionesFiltro = opciones
                    val adaptador = ArrayAdapter(
                        this@HistorialActivity,
                        android.R.layout.simple_spinner_item,
                        opciones.map { it.second }
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    spinner.adapter = adaptador
                }
            }
        }
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val filtroId = opcionesFiltro.getOrNull(position)?.first
                viewModel.setFiltro(if (filtroId == 0) null else filtroId)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                viewModel.setFiltro(null)
            }
        }
    }

    private fun observarHistorial() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registros.collect { registros ->
                    val items = HistorialAdapter.agrupar(this@HistorialActivity, registros)
                    adapter.submitList(items)
                    if (items.isEmpty()) {
                        layoutVacio.visibility = View.VISIBLE
                        recyclerHistorial.visibility = View.GONE
                    } else {
                        layoutVacio.visibility = View.GONE
                        recyclerHistorial.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}