package com.dosis.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dosis.R
import com.dosis.data.Medicamento
import com.dosis.data.MedicamentoConDosis
import com.google.android.material.button.MaterialButton

class MedicamentoAdapter(
    private val onTomarDosis: (Medicamento) -> Unit,
    private val onAjustarStock: (Medicamento) -> Unit,
    private val onEditar: (Medicamento) -> Unit,
    private val onEliminar: (Medicamento) -> Unit
) : ListAdapter<MedicamentoConDosis, MedicamentoAdapter.MedicamentoViewHolder>(DiffCallback) {

    private var ultimoTapTomar = 0L

    inner class MedicamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHora: TextView = itemView.findViewById(R.id.tvHora)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvDosis: TextView = itemView.findViewById(R.id.tvDosis)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val indicadorStock: View = itemView.findViewById(R.id.indicadorStock)
        val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        val tvDias: TextView = itemView.findViewById(R.id.tvDias)
        val btnTomar: MaterialButton = itemView.findViewById(R.id.btnTomar)
        val btnReabastecer: ImageButton = itemView.findViewById(R.id.btnReabastecer)
        val btnMenu: ImageButton = itemView.findViewById(R.id.btnMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicamentoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento, parent, false)
        return MedicamentoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: MedicamentoViewHolder, position: Int) {
        val item = currentList[position]
        val med = item.medicamento
        val contexto = holder.itemView.context

        holder.tvHora.text = com.dosis.util.FormatoHora.lista12(item.horas)
        holder.tvNombre.text = med.nombre
        holder.tvDosis.text = med.dosis

        if (med.descripcion.isBlank()) {
            holder.tvDescripcion.visibility = View.GONE
        } else {
            holder.tvDescripcion.visibility = View.VISIBLE
            holder.tvDescripcion.text = med.descripcion
        }

        // Días restantes estimados (stock / dosis diarias)
        val dosisDiarias = item.horas.split(",").count { it.isNotBlank() }
        val dias = if (dosisDiarias > 0) med.stockActual / dosisDiarias else 0
        if (dias > 0) {
            holder.tvDias.visibility = View.VISIBLE
            holder.tvDias.text = contexto.getString(R.string.dias_estimados, dias)
        } else {
            holder.tvDias.visibility = View.GONE
        }

        when {
            med.stockActual <= 0 -> {
                holder.indicadorStock.backgroundTintList =
                    ContextCompat.getColorStateList(contexto, R.color.stock_empty)
                holder.tvStock.text = contexto.getString(R.string.sin_stock)
                holder.btnTomar.isEnabled = false
                holder.btnTomar.alpha = 0.4f
            }
            med.stockActual <= med.stockMinimo -> {
                holder.indicadorStock.backgroundTintList =
                    ContextCompat.getColorStateList(contexto, R.color.stock_low)
                holder.tvStock.text = contexto.resources.getQuantityString(
                    R.plurals.quedan_unidades,
                    med.stockActual,
                    med.stockActual
                )
                holder.btnTomar.isEnabled = true
                holder.btnTomar.alpha = 1f
            }
            else -> {
                holder.indicadorStock.backgroundTintList =
                    ContextCompat.getColorStateList(contexto, R.color.stock_ok)
                holder.tvStock.text = contexto.resources.getQuantityString(
                    R.plurals.quedan_unidades,
                    med.stockActual,
                    med.stockActual
                )
                holder.btnTomar.isEnabled = true
                holder.btnTomar.alpha = 1f
            }
        }

        // Acciones
        holder.btnTomar.setOnClickListener {
            val ahora = System.currentTimeMillis()
            if (ahora - ultimoTapTomar > 500) {
                ultimoTapTomar = ahora
                onTomarDosis(med)
            }
        }
        holder.btnReabastecer.setOnClickListener { onAjustarStock(med) }
        holder.tvStock.setOnClickListener { onAjustarStock(med) }

        holder.btnMenu.setOnClickListener { vista ->
            PopupMenu(vista.context, vista).apply {
                menu.add(0, 1, 0, R.string.editar)
                menu.add(0, 2, 0, R.string.eliminar)
                setOnMenuItemClickListener { itemMenu ->
                    when (itemMenu.itemId) {
                        1 -> onEditar(med)
                        2 -> onEliminar(med)
                    }
                    true
                }
                show()
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MedicamentoConDosis>() {
        override fun areItemsTheSame(a: MedicamentoConDosis, b: MedicamentoConDosis) =
            a.medicamento.id == b.medicamento.id

        override fun areContentsTheSame(a: MedicamentoConDosis, b: MedicamentoConDosis) = a == b
    }
}