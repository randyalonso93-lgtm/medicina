package com.dosis

import android.app.Application
import com.dosis.util.PreferenciasTema

class DosisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenciasTema.aplicar(this)
    }
}