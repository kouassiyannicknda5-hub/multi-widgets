package app.multiwidgets.ai.subtitles

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fenêtre flottante des sous-titres traduits : posée par-dessus les autres
 * applications, alimentée par [SubtitleBus]. Un appui long copie le dernier
 * segment traduit. Aucun enregistrement n'est conservé ici.
 */
class SubtitleOverlayService : Service() {

    private var wm: WindowManager? = null
    private var vue: LinearLayout? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xCC10151B.toInt())
            setPadding(24, 16, 24, 16)
        }
        vue = root
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            0, 0,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = 120 }
        runCatching { wm?.addView(root, p) }
        root.setOnLongClickListener {
            val dernier = SubtitleBus.segments.value.lastOrNull() ?: return@setOnLongClickListener false
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("traduction", dernier.traduction))
            true
        }
        job = scope.launch {
            SubtitleBus.segments.collectLatest { liste ->
                root.removeAllViews()
                liste.takeLast(3).forEach { s ->
                    root.addView(TextView(this@SubtitleOverlayService).apply {
                        text = s.traduction
                        setTextColor(0xFFFFFFFF.toInt())
                        textSize = 15f
                    })
                }
            }
        }
    }

    override fun onDestroy() {
        job?.cancel(); scope.cancel()
        vue?.let { runCatching { wm?.removeView(it) } }
        vue = null
        super.onDestroy()
    }
}
