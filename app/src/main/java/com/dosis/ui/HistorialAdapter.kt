package com.dosis.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dosis.R
import com.dosis.data.RegistroToma
import com.dosis.data.RegistroTomaConMedicamento
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class HistorialItem {
    data class Encabezado(val titulo: String) : HistorialItem()
    data class Contenido(val registro: RegistroTomaConMedicamento) : HistorialItem()
}

class HistorialAdapter :
    ListAdapter<HistorialItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int =
        if (currentList[position] is HistorialItem.Encabezado) TIPO_ENCABEZADO else TIPO_CONTENIDO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflador = LayoutInflater.from(parent.context)
        return if (viewType == TIPO_ENCABEZADO) {
            EncabezadoViewHolder(
                inflador.inflate(R.layout.item_historial_encabezado, parent, false)
            )
        } else {
            ContenidoViewHolder(
                inflador.inflate(R.layout.item_historial, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = currentList[position]) {
            is HistorialItem.Encabezado -> {
                (holder as EncabezadoViewHolder).tvTitulo.text = item.titulo
            }
            is HistorialItem.Contenido -> (holder as ContenidoViewHolder).bind(item.registro)
        }
    }

    class EncabezadoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.tvEncabezado)
    }

    class ContenidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val indicador: View = itemView.findViewById(R.id.indicadorToma)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreHis)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFechaHis)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoHis)

        fun bind(registro: RegistroTomaConMedicamento) {
            val contexto = itemView.context
            tvNombre.text = registro.nombreMedicamento
            val fecha = Date(registro.fechaHora)
            val hora = SimpleDateFormat("h:mm a", Locale.getDefault()).format(fecha)
            tvFecha.text = if (HistorialAdapter.esHoy(fecha)) {
                contexto.getString(R.string.registro_hoy, hora)
            } else if (HistorialAdapter.esAyer(fecha)) {
                contexto.getString(R.string.registro_ayer, hora)
            } else {
                val dia = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(fecha)
                contexto.getString(R.string.registro_fecha, dia, hora)
            }

            val (texto, color) = when {
                registro.tipo == RegistroToma.REABASTECIMIENTO -> {
                    contexto.getString(R.string.registro_reabastecido, registro.unidades) to R.color.md_theme_primary
                }
                registro.tomado -> contexto.getString(R.string.registro_tomado, hora) to R.color.stock_ok
                else -> contexto.getString(R.string.registro_omitido, hora) to R.color.md_theme_outline
            }
            tvEstado.text = texto
            indicador.setBackgroundColor(ContextCompat.getColor(contexto, color))
        }
    }

    companion object {
        const val TIPO_ENCABEZADO = 0
        const val TIPO_CONTENIDO = 1

        fun agrupar(contexto: Context, registros: List<RegistroTomaConMedicamento>): List<HistorialItem> {
            if (registros.isEmpty()) return emptyList()
            val items = mutableListOf<HistorialItem>()
            var ultimaClave = -1L
            for (r in registros) {
                val clave = com.dosis.util.ProgramacionDosis.inicioDia(r.fechaHora)
                if (clave != ultimaClave) {
                    ultimaClave = clave
                    items.add(HistorialItem.Encabezado(tituloDe(contexto, clave)))
                }
                items.add(HistorialItem.Contenido(r))
            }
            return items
        }

        private fun tituloDe(contexto: Context, inicioDia: Long): String {
            val hoyInicio = com.dosis.util.ProgramacionDosis.inicioDia(System.currentTimeMillis())
            val f = Date(inicioDia)
            return when (inicioDia) {
                hoyInicio -> contexto.getString(R.string.registro_hoy_titulo)
                hoyInicio - 86_400_000L -> contexto.getString(R.string.registro_ayer_titulo)
                else -> SimpleDateFormat("EEEE d MMM", Locale.getDefault()).format(f)
            }
        }

        fun esHoy(fecha: Date): Boolean {
            val (inicioHoy, finHoy) = limitesDia()
            return fecha.time >= inicioHoy && fecha.time < finHoy
        }

        fun esAyer(fecha: Date): Boolean {
            val (inicioHoy, _) = limitesDia()
            val inicioAyer = inicioHoy - 86_400_000L
            val finAyer = inicioHoy
            return fecha.time >= inicioAyer && fecha.time < finAyer
        }

        private fun limitesDia(): Pair<Long, Long> {
            val c = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return c.timeInMillis to (c.timeInMillis + 86_400_000L)
        }

        val DiffCallback = object : DiffUtil.ItemCallback<HistorialItem>() {
            override fun areItemsTheSame(a: HistorialItem, b: HistorialItem): Boolean =
                when {
                    a is HistorialItem.Encabezado && b is HistorialItem.Encabezado ->
                        a.titulo == b.titulo
                    a is HistorialItem.Contenido && b is HistorialItem.Contenido ->
                        a.registro.id == b.registro.id
                    else -> false
                }

            override fun areContentsTheSame(a: HistorialItem, b: HistorialItem): Boolean = a == b
        }
    }
}