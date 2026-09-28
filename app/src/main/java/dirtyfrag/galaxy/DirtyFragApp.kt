package dirtyfrag.galaxy

import android.app.Application
import dirtyfrag.galaxy.core.Notifier

class DirtyFragApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
    }
}
