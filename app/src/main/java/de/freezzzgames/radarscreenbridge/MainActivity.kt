package de.freezzzgames.radarscreenbridge

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import java.io.File

class MainActivity : Activity() {
    private val requestCapture = 41
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private val cameraActions = setOf(
        "android.media.action.IMAGE_CAPTURE",
        "android.media.action.STILL_IMAGE_CAMERA"
    )
    private val refresh = object : Runnable {
        override fun run() {
            val file = File(cacheDir, "screen_preview.jpg")
            if (file.exists() && file.length() > 0) {
                BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
                    preview.setImageBitmap(bitmap)
                    status.text = "Захват активен • ${bitmap.width}×${bitmap.height}"
                }
            }
            handler.postDelayed(this, 700)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
        }
        status = TextView(this).apply { text = "Radar Screen Bridge — тест"; textSize = 18f }
        val info = TextView(this).apply {
            text = "Тестовый режим: захват экрана Android. Нажатие кнопки часов сработает только если приложение-компаньон отправляет стандартный Android camera intent. Передача изображения на часы ещё не реализована."
            textSize = 14f
            setPadding(0, 16, 0, 16)
        }
        val start = Button(this).apply {
            text = "Запустить захват экрана"
            setOnClickListener { requestScreenCapture() }
        }
        val stop = Button(this).apply {
            text = "Остановить захват"
            setOnClickListener {
                stopService(Intent(this@MainActivity, CaptureService::class.java))
                status.text = "Захват остановлен"
            }
        }
        preview = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER }
        root.addView(status)
        root.addView(info)
        root.addView(start)
        root.addView(stop)
        root.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        handler.post(refresh)

        if (intent?.action in cameraActions) {
            status.text = "Получена команда камеры • запрос разрешения на захват"
            root.postDelayed({ requestScreenCapture() }, 350)
        }
    }

    private fun requestScreenCapture() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), requestCapture)
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCapture && resultCode == RESULT_OK && data != null) {
            val svc = Intent(this, CaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            startForegroundService(svc)
            status.text = "Разрешение получено, запуск захвата…"
        } else if (requestCode == requestCapture) {
            status.text = "Захват не запущен: разрешение не предоставлено"
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        super.onDestroy()
    }
}
