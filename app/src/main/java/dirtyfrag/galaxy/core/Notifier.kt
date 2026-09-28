package dirtyfrag.galaxy.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dirtyfrag.galaxy.MainActivity
import dirtyfrag.galaxy.R

/**
 * Posts the root-grant request as a notification. We cannot show a popup from a
 * background thread, so the notification is the reliable way to tell the user to
 * toggle ROOT for this app inside the KernelSU manager.
 */
object Notifier {

    private const val CHANNEL_ID = "dirtyfrag_root"
    private const val ID_GRANT = 2001

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            ch.description = ctx.getString(R.string.notif_channel_desc)
            nm.createNotificationChannel(ch)
        }
    }

    fun grantRequest(ctx: Context) {
        ensureChannel(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openManager = PendingIntent.getActivity(
            ctx, 1,
            Intent().setComponent(
                ComponentName(KERNELSU_PKG, "$KERNELSU_PKG.ui.MainActivity")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openApp = PendingIntent.getActivity(
            ctx, 2,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(ctx.getString(R.string.notif_grant_title))
            .setContentText(ctx.getString(R.string.notif_grant_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(ctx.getString(R.string.notif_grant_text)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .addAction(0, ctx.getString(R.string.notif_open_manager), openManager)
            .build()
        nm.notify(ID_GRANT, notif)
    }

    fun cancelGrant(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ID_GRANT)
    }

    private const val ID_MODULE = 2004

    /**
     * Notifies the user that root succeeded but modules (Zygisk-Next / LSPosed) are
     * still missing. Root is required before modules can be installed, so this is the
     * step where first-time users must open KernelSU manager > Modules.
     */
    fun moduleInstallRequired(ctx: Context, names: String) {
        ensureChannel(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openManager = PendingIntent.getActivity(
            ctx, 3,
            Intent().setComponent(
                ComponentName(KERNELSU_PKG, "$KERNELSU_PKG.ui.MainActivity")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openApp = PendingIntent.getActivity(
            ctx, 4,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = ctx.getString(R.string.notif_module_title)
        val text = ctx.getString(R.string.notif_module_text)
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .addAction(0, ctx.getString(R.string.notif_open_manager), openManager)
            .build()
        nm.notify(ID_MODULE, notif)
    }

    fun cancelModule(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ID_MODULE)
    }

    const val CHANNEL_ACTION_ID = "dirtyfrag_action"
    private const val ID_RESTART = 2005

    fun ensureAction(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ACTION_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ACTION_ID,
                    ctx.getString(R.string.notif_channel_action),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    /**
     * Root + modules are ready but LSPosed needs a framework restart. Offer a one-tap
     * "soft restart now" action so the user is not left staring at a spinner.
     */
    fun softRestartNeeded(ctx: Context) {
        ensureAction(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openApp = PendingIntent.getActivity(
            ctx, 5,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val restart = PendingIntent.getBroadcast(
            ctx, 6,
            Intent(ctx, dirtyfrag.galaxy.exploit.SoftRestartReceiver::class.java)
                .setAction("dirtyfrag.galaxy.SOFT_RESTART"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = ctx.getString(R.string.notif_restart_text)
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ACTION_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(ctx.getString(R.string.notif_restart_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .addAction(0, ctx.getString(R.string.notif_restart_action), restart)
            .build()
        nm.notify(ID_RESTART, notif)
    }

    fun cancelSoftRestart(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ID_RESTART)
    }

    private const val ID_PROGRESS = 2002
    private const val ID_RESULT = 2003

    const val CHANNEL_AUTO_ID = "dirtyfrag_auto"

    fun ensureAuto(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_AUTO_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_AUTO_ID, ctx.getString(R.string.notif_channel_auto), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun progress(ctx: Context, text: String) {
        ensureAuto(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(ctx, CHANNEL_AUTO_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(ctx.getString(R.string.notif_progress_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        nm.notify(ID_PROGRESS, notif)
    }

    fun progressDone(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ID_PROGRESS)
    }

    fun result(ctx: Context, success: Boolean, text: String) {
        ensureAuto(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(ctx, CHANNEL_AUTO_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(ctx.getString(if (success) R.string.notif_done_title else R.string.notif_fail_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(ID_RESULT, notif)
    }

    @Suppress("unused")
    fun notificationsEnabled(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .areNotificationsEnabled()
        } else true
    }
}
