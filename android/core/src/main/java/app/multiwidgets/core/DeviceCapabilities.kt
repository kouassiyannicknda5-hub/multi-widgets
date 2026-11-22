package app.multiwidgets.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager

/**
 * Capacités RÉELLES de l'appareil, mesurées — jamais supposées.
 *
 * La spécification interdit de prétendre qu'une permission de superposition
 * suffirait à afficher six à dix applications tierces, ou qu'un appareil
 * « 8 Go de RAM » sait forcément tout faire : chaque fonction multitâche ou
 * IA est donc annoncée disponible seulement si le firmware le confirme ici.
 */
object DeviceCapabilities {

    data class Snapshot(
        val abis: List<String>,
        val arm64: Boolean,
        val totalRamMb: Long,
        val availableRamMb: Long,
        val androidSdk: Int,
        val freeformWindows: Boolean,
        val splitScreen: Boolean,
        val mediaProjection: Boolean,
        val audioPlaybackCapture: Boolean,
        val thermalHeadroom: Thermal,
        val manufacturer: String,
        val model: String,
    )

    enum class Thermal { OK, WARM, HOT }

    /** architectures natives exposées par le processeur */
    fun abis(): List<String> = Build.SUPPORTED_ABIS.toList()

    fun isArm64(): Boolean = abis().any { it.startsWith("arm64") }

    fun ramMb(context: Context): Pair<Long, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem / 1_048_576 to info.availMem / 1_048_576
    }

    /**
     * Fenêtres libres (freeform) : seule voie Android permettant plus de deux
     * applications visibles à la fois. Absente de la plupart des firmwares
     * (ZTE, Tecno, Itel, Infinix, beaucoup de Redmi/Samsung) : dans ce cas
     * [splitScreen] reste vrai et les dispositions 3/4/6 zones basculent sur
     * le panneau flottant de secours, comme l'exige la spécification.
     */
    fun freeformWindows(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)

    /** écran partagé natif (Android 7+) : deux applications côte à côte ou superposées */
    fun splitScreen(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    fun mediaProjection(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    /** capture du son d'autres applications : Android 10+ et soumise aux politiques de l'app source */
    fun audioPlaybackCapture(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun thermal(context: Context): Thermal {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE, PowerManager.THERMAL_STATUS_LIGHT -> Thermal.OK
                PowerManager.THERMAL_STATUS_MODERATE -> Thermal.WARM
                else -> Thermal.HOT
            }
        } else Thermal.OK
    }

    fun snapshot(context: Context): Snapshot {
        val (total, avail) = ramMb(context)
        return Snapshot(
            abis = abis(),
            arm64 = isArm64(),
            totalRamMb = total,
            availableRamMb = avail,
            androidSdk = Build.VERSION.SDK_INT,
            freeformWindows = freeformWindows(context),
            splitScreen = splitScreen(),
            mediaProjection = mediaProjection(),
            audioPlaybackCapture = audioPlaybackCapture(),
            thermalHeadroom = thermal(context),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
        )
    }

    /**
     * Mémoire réellement disponible pour un modèle IA : on ne réserve jamais
     * plus du quart de la mémoire libre, et jamais sous 1,5 Go de marge.
     */
    fun budgetModeleIA(context: Context): Boolean = ramMb(context).second > 1500
}
