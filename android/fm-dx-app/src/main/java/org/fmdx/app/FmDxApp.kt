package org.fmdx.app

import android.app.Application
import org.fmdx.app.data.FmDxSessionController

class FmDxApp : Application() {
    lateinit var sessionController: FmDxSessionController
        private set

    override fun onCreate() {
        super.onCreate()
        sessionController = FmDxSessionController(this)
    }
}
