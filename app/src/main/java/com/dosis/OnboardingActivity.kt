package com.dosis

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.dosis.ui.OnboardingAdapter
import com.dosis.ui.PaginaOnboarding
import com.dosis.util.PreferenciasOnboarding

/** Onboarding de primer uso. Actividad de lanzamiento de la app. */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var indicador: LinearLayout
    private lateinit var btnSiguiente: com.google.android.material.button.MaterialButton
    private lateinit var btnEmpezar: com.google.android.material.button.MaterialButton
    private lateinit var btnSaltar: View

    private val paginas = listOf(
        PaginaOnboarding(
            android.R.drawable.ic_menu_agenda,
            R.string.onb_bienvenida_titulo,
            R.string.onb_bienvenida_desc
        ),
        PaginaOnboarding(
            android.R.drawable.ic_lock_idle_alarm,
            R.string.onb_recordatorios_titulo,
            R.string.onb_recordatorios_desc
        ),
        PaginaOnboarding(
            android.R.drawable.ic_dialog_alert,
            R.string.onb_stock_titulo,
            R.string.onb_stock_desc
        ),
        PaginaOnboarding(
            android.R.drawable.ic_menu_week,
            R.string.onb_historial_titulo,
            R.string.onb_historial_desc
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PreferenciasOnboarding.visto(this)) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPagerOnboarding)
        indicador = findViewById(R.id.indicadorOnboarding)
        btnSiguiente = findViewById(R.id.btnSiguiente)
        btnEmpezar = findViewById(R.id.btnEmpezar)
        btnSaltar = findViewById(R.id.btnSaltar)

        viewPager.adapter = OnboardingAdapter(paginas)
        crearIndicador()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                actualizarIndicador(position)
                val ultima = position == paginas.lastIndex
                btnEmpezar.visibility = if (ultima) View.VISIBLE else View.GONE
                btnSiguiente.visibility = if (ultima) View.GONE else View.VISIBLE
                btnSaltar.visibility = if (ultima) View.GONE else View.VISIBLE
            }
        })

        btnSiguiente.setOnClickListener {
            viewPager.currentItem = viewPager.currentItem + 1
        }
        btnEmpezar.setOnClickListener { finalizar() }
        btnSaltar.setOnClickListener { finalizar() }
    }

    private fun crearIndicador() {
        indicador.removeAllViews()
        val tamano = (resources.displayMetrics.density * 8).toInt()
        val margen = (resources.displayMetrics.density * 4).toInt()
        repeat(paginas.size) {
            val punto = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(tamano, tamano).apply {
                    marginStart = margen
                    marginEnd = margen
                }
            }
            indicador.addView(punto)
        }
        actualizarIndicador(0)
    }

    private fun actualizarIndicador(posicion: Int) {
        val colorActivo = ContextCompat.getColor(this, R.color.md_theme_primary)
        val colorInactivo = ContextCompat.getColor(this, R.color.md_theme_outline)
        for (i in 0 until indicador.childCount) {
            val punto = indicador.getChildAt(i)
            val fondo = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (i == posicion) colorActivo else colorInactivo)
                cornerRadius = 5f
            }
            punto.background = fondo
        }
    }

    private fun finalizar() {
        PreferenciasOnboarding.marcarVisto(this)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}