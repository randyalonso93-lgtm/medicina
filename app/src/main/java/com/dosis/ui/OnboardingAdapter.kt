package com.dosis.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dosis.R

/** Página de onboarding: imagen + título + descripción. */
class OnboardingAdapter(
    private val paginas: List<PaginaOnboarding>
) : RecyclerView.Adapter<OnboardingAdapter.Vista>() {

    class Vista(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagen: ImageView = itemView.findViewById(R.id.imagenOnboarding)
        val titulo: TextView = itemView.findViewById(R.id.tituloOnboarding)
        val descripcion: TextView = itemView.findViewById(R.id.descripcionOnboarding)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vista {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return Vista(vista)
    }

    override fun onBindViewHolder(holder: Vista, position: Int) {
        val pagina = paginas[position]
        holder.imagen.setImageResource(pagina.icono)
        holder.titulo.setText(pagina.titulo)
        holder.descripcion.setText(pagina.descripcion)
    }

    override fun getItemCount(): Int = paginas.size
}

data class PaginaOnboarding(
    val icono: Int,
    val titulo: Int,
    val descripcion: Int
)