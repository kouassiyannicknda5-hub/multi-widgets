package app.multiwidgets.ai.capture

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import app.multiwidgets.ai.language.LanguageId
import app.multiwidgets.ai.ocr.OcrEngine
import app.multiwidgets.ai.router.ProcessingRouter
import app.multiwidgets.ai.subtitles.Segment
import app.multiwidgets.ai.subtitles.SubtitleBus
import app.multiwidgets.ai.translation.CloudTranslator
import app.multiwidgets.ai.translation.TranslationEngine
import app.multiwidgets.core.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Traduction d'écran : capture CONSENTIE (MediaProjection), OCR local,
 * détection de langue, traduction via le routeur, sous-titres flottants.
 *
 * Limites respectées : consentement système à chaque session, fenêtres
 * sécurisées (DRM, bancaires) rendues noires par Android lui-même, cadence
 * d'analyse réduite en mode économie, buffers bornés, aucune capture
 * secrète — un rappel permanent est affiché en notification.
 */
class ScreenCaptureService : Service() {

    private var projection: MediaProjection? = null
    private var virtual: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val ocr = OcrEngine()
    private var dernierTexte = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        demarrerRappel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra(EXTRA_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data: Intent? = intent?.getParcelableExtra(EXTRA_DATA)
        if (code != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(code, data)
        lancerPipeline()
        return START_NOT_STICKY
    }

    private fun lancerPipeline() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        // Buffer unique et borné : pas de file de captures accumulées en RAM.
        reader = ImageReader.newInstance(dm.widthPixels / 2, dm.heightPixels / 2, PixelFormat.RGBA_8888, 2)
        virtual = projection?.createVirtualDisplay(
            "multi-widgets", dm.widthPixels / 2, dm.heightPixels / 2, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface, null, null
        )
        job = scope.launch { boucle() }
    }

    /** Boucle d'analyse : cadence réglable, espacée en mode économie. */
    private suspend fun boucle() {
        val settings = Settings(this)
        while (true) {
            val freq = (settings.get("ocr_frequence_ms").toLongOrNull() ?: 2500L)
            val eco = settings.get("eco").toBoolean()
            delay(if (eco) maxOf(freq * 2, 5000L) else freq)
            val bitmap = capter() ?: continue
            val texte = ocr.reconnaitre(bitmap).trim()
            bitmap.recycle()
            if (texte.length < 3 || texte == dernierTexte) continue   // pas de changement de texte
            dernierTexte = texte
            traiter(texte, settings)
        }
    }

    /** Capture bornée : un seul buffer, copie tenant compte du pas de ligne. */
    private fun capter(): Bitmap? = runCatching {
        val image = reader?.acquireLatestImage() ?: return null
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixels = ByteArray(buffer.remaining())
        buffer.get(pixels)
        val pas = plane.rowStride
        val bmp = if (pas == image.width * 4) {
            Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888).also {
                it.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(pixels))
            }
        } else {
            val large = pas / 4
            val brut = Bitmap.createBitmap(large, image.height, Bitmap.Config.ARGB_8888).also {
                it.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(pixels))
            }
            Bitmap.createBitmap(brut, 0, 0, image.width, image.height).also { brut.recycle() }
        }
        image.close()
        bmp
    }.getOrNull()

    private suspend fun traiter(texte: String, settings: Settings) = withContext(Dispatchers.Default) {
        val langueSource = LanguageId.detecter(texte)
        if (langueSource == "und") return@withContext
        val cible = settings.get("langue_cible").ifBlank { "fr" }
        if (langueSource == cible) return@withContext
        val route = ProcessingRouter.decider(
            ProcessingRouter.Entree(
                reseau = connexion(),
                modeleLocal = TranslationEngine.modelePresent(this@ScreenCaptureService, langueSource) &&
                        TranslationEngine.modelePresent(this@ScreenCaptureService, cible),
                fournisseurConfigure = settings.get("fournisseur_url").isNotBlank(),
                mode = settings.get("mode_traitement").ifBlank { "auto" },
                eco = settings.get("eco").toBoolean(),
                confidentialiteStricte = settings.get("confidentialite_stricte").toBoolean(),
                thermiqueAcceptable = app.multiwidgets.core.DeviceCapabilities.thermal(this@ScreenCaptureService) !=
                        app.multiwidgets.core.DeviceCapabilities.Thermal.HOT,
                memoireSuffisante = app.multiwidgets.core.DeviceCapabilities.budgetModeleIA(this@ScreenCaptureService),
            )
        )
        val traduit: String? = when (route.route) {
            ProcessingRouter.Route.LOCAL, ProcessingRouter.Route.LOCAL_ECO ->
                TranslationEngine.traduire(texte, langueSource, cible)
            ProcessingRouter.Route.CLOUD -> {
                val url = settings.get("fournisseur_url")
                val f: CloudTranslator? = app.multiwidgets.ai.translation.fournisseurConfigure(url)
                f?.traduire(texte, langueSource, cible, settings.cleFournisseur())?.getOrNull()
            }
            ProcessingRouter.Route.REFUS -> null
        }
        SubtitleBus.publier(Segment(texte.take(400), langueSource, traduit ?: "(${route.message ?: "traduction indisponible"})"))
    }

    private fun connexion(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetwork != null
    }

    private fun demarrerRappel() {
        val nm = getSystemService(NotificationManager::class.java)
        val canal = NotificationChannel(CANAL, "Traduction d'écran", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(canal)
        val n = Notification.Builder(this, CANAL)
            .setContentTitle("Traduction d'écran active")
            .setContentText("Capture consentie en cours. Touchez pour arrêter.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    override fun onDestroy() {
        job?.cancel(); scope.cancel()
        runCatching { virtual?.release() }
        runCatching { reader?.close() }
        runCatching { projection?.stop() }
        virtual = null; reader = null; projection = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
        private const val CANAL = "capture"
        private const val NOTIF_ID = 41
    }
}
