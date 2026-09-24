package com.dosis

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dosis.ui.EstadisticasViewModel
import kotlinx.coroutines.launch

class EstadisticasActivity : AppCompatActivity() {

    private val viewModel: EstadisticasViewModel by viewModels { EstadisticasViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas)

        setSupportActionBar(findViewById(R.id.toolbarEstadisticas))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.cargar()
        observar()
    }

    private fun observar() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.resumen.collect { resumen ->
                    findViewById<TextView>(R.id.tvSemanaTomado).text =
                        getString(R.string.stat_tomadas, resumen.semanaTomadas)
                    findViewById<TextView>(R.id.tvSemanaOmitido).text =
                        getString(R.string.stat_omitidas, resumen.semanaOmitidas)
                    findViewById<TextView>(R.id.tvSemanaPct).text =
                        getString(R.string.stat_cumplimiento, resumen.semanaPct)

                    findViewById<TextView>(R.id.tvMesTomado).text =
                        getString(R.string.stat_tomadas, resumen.mesTomadas)
                    findViewById<TextView>(R.id.tvMesOmitido).text =
                        getString(R.string.stat_omitidas, resumen.mesOmitidas)
                    findViewById<TextView>(R.id.tvMesPct).text =
                        getString(R.string.stat_cumplimiento, resumen.mesPct)

                    findViewById<TextView>(R.id.tvRacha).text =
                        getString(R.string.stat_racha, resumen.racha)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}