package app.multiwidgets.core

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Demande des permissions AU BON MOMENT, avec explication :
 * chaque écran appelle [explication] avant de déclencher la demande système.
 */
object Permissions {

    fun overlayGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun overlayIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    fun micGranted(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    fun notificationsGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.areNotificationsEnabled()
        } else true

    const val MIC = Manifest.permission.RECORD_AUDIO
    const val NOTIF = Manifest.permission.POST_NOTIFICATIONS

    /** Textes d'explication affichés avant chaque demande système. */
    fun explication(what: String): String = when (what) {
        "overlay" -> "Multi-widgets affiche sa barre latérale, ses sous-titres et ses fenêtres de traduction par-dessus les autres applications. Sans cette autorisation, rien ne peut flotter à l'écran."
        "mic" -> "La traduction vocale écoute le micro uniquement pendant une session lancée par vous, et s'arrête avec elle. Aucun enregistrement n'est conservé sans historique activé."
        "projection" -> "La traduction d'écran capture l'affichage APRÈS votre consentement explicite à chaque session, via la boîte système Android. Les fenêtres sécurisées (DRM, bancaires) restent noires : c'est une protection, pas un défaut."
        else -> ""
    }
}
