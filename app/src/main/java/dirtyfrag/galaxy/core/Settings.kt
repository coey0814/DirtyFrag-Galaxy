package dirtyfrag.galaxy.core

import android.content.Context
import android.content.SharedPreferences

/** Simple SharedPreferences-backed settings store. */
class Settings(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("dirtyfrag_galaxy", Context.MODE_PRIVATE)

    var autoRootOnBoot: Boolean
        get() = sp.getBoolean(KEY_AUTO_ROOT, false)
        set(v) = sp.edit().putBoolean(KEY_AUTO_ROOT, v).apply()

    /**
     * Default OFF for a fresh install, but fully persisted once the user flips it.
     * (The previous default=ON caused a soft-reboot loop on devices where the
     * exploit leaves SELinux enforcing and the old permissive gate could never pass.)
     */
    var autoSoftRestart: Boolean
        get() = sp.getBoolean(KEY_AUTO_SOFT_RESTART, false)
        set(v) = sp.edit().putBoolean(KEY_AUTO_SOFT_RESTART, v).apply()

    /** Restore SELinux to Enforcing once root + modules + LSPosed are confirmed. Default ON. */
    var restoreEnforcing: Boolean
        get() = sp.getBoolean(KEY_RESTORE_ENFORCING, true)
        set(v) = sp.edit().putBoolean(KEY_RESTORE_ENFORCING, v).apply()

    /** How many soft reboots we have issued while trying to activate LSPosed. */
    var activationAttempts: Int
        get() = sp.getInt(KEY_ACTIVATION_ATTEMPTS, 0)
        set(v) = sp.edit().putInt(KEY_ACTIVATION_ATTEMPTS, v).apply()

    fun options() = Options(
        autoRootOnBoot = autoRootOnBoot,
        autoSoftRestart = autoSoftRestart,
        restoreEnforcing = restoreEnforcing
    )

    companion object {
        private const val KEY_AUTO_ROOT = "auto_root_on_boot"
        private const val KEY_AUTO_SOFT_RESTART = "auto_soft_restart"
        private const val KEY_RESTORE_ENFORCING = "restore_enforcing"
        private const val KEY_ACTIVATION_ATTEMPTS = "activation_attempts"
    }
}
