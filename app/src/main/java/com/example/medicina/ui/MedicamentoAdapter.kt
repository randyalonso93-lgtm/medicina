package com.example.medicina.ui


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.example.medicina.R
import com.example.medicina.data.Medicamento

class MedicamentoAdapter(
    @Suppress("UNUSED_LAMBDA_EXPRESSION")
    private val onTomarDosis: (Medicamento) -> Unit,
    @Suppress("UNUSED_LAMBDA_EXPRESSION")
    private val onReabastecer: (Medicamento) -> Unit,
    @Suppress("UNUSED_LAMBDA_EXPRESSION")
    private val onEditar: (Medicamento) -> Unit,
    @Suppress("UNUSED_LAMBDA_EXPRESSION")
    private val onEliminar: (Medicamento) -> Unit
) : ListAdapter<Medicamento, MedicamentoAdapter.MedicamentoViewHolder>(DiffCallback) {

    inner class MedicamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHora: TextView = itemView.findViewById(R.id.tvHora)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvDosis: TextView = itemView.findViewById(R.id.tvDosis)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val indicadorStock: View = itemView.findViewById(R.id.indicadorStock)
        val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        val btnTomar: MaterialButton = itemView.findViewById(R.id.btnTomar)
        val btnReabastecer: MaterialButton = itemView.findViewById(R.id.btnReabastecer)
        val btnMenu: ImageButton = itemView.findViewById(R.id.btnMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicamentoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento, parent, false)
        return MedicamentoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: MedicamentoViewHolder, position: Int) {
        val med = currentList[position]

        holder.tvHora.text = med.hora
        holder.tvNombre.text = med.nombre
        holder.tvDosis.text = med.dosis

        // Descripción: ocultar si está vacía
        if (med.descripcion.isBlank()) {
            holder.tvDescripcion.visibility = View.GONE
        } else {
            holder.tvDescripcion.visibility = View.VISIBLE
            holder.tvDescripcion.text = med.descripcion
        }

        // Stock: color según nivel
        when {
            med.stockActual <= 0 -> {
                holder.indicadorStock.setBackgroundResource(R.color.stock_empty)
                holder.tvStock.text = "Sin stock"
                holder.btnTomar.isEnabled = false
                holder.btnTomar.alpha = 0.4f
            }
            med.stockActual <= med.stockMinimo -> {
                holder.indicadorStock.setBackgroundResource(R.color.stock_low)
                holder.tvStock.text = "Quedan ${med.stockActual} unidades"
                holder.btnTomar.isEnabled = true
                holder.btnTomar.alpha = 1f
            }
            else -> {
                holder.indicadorStock.setBackgroundResource(R.color.stock_ok)
                holder.tvStock.text = "Quedan ${med.stockActual} unidades"
                holder.btnTomar.isEnabled = true
                holder.btnTomar.alpha = 1f
            }
        }

        // Acciones
        holder.btnTomar.setOnClickListener { onTomarDosis(med) }
        holder.btnReabastecer.setOnClickListener { onReabastecer(med) }

        // Menú contextual
        holder.btnMenu.setOnClickListener { vista ->
            PopupMenu(vista.context, vista).apply {
                menu.add(0, 1, 0, R.string.editar)
                menu.add(0, 2, 0, R.string.eliminar)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> onEditar(med)
                        2 -> onEliminar(med)
                    }
                    true
                }
                show()
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Medicamento>() {
        override fun areItemsTheSame(a: Medicamento, b: Medicamento) = a.id == b.id
        override fun areContentsTheSame(a: Medicamento, b: Medicamento) = a == b
    }
}