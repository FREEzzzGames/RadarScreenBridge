package de.freezzzgames.radarscreenbridge

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private val busy = AtomicBoolean(false)
    private var sharedPreviewUri: android.net.Uri? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = Notification.Builder(this, "capture")
            .setContentTitle("Radar Screen Bridge")
            .setContentText("Захват экрана включён")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
        startForeground(7, notification)
        val code = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("data", Intent::class.java) else @Suppress("DEPRECATION") intent?.getParcelableExtra("data")
        if (code != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }
        projection = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(code, data)
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                display?.release()
                reader?.close()
                stopSelf()
            }
        }, android.os.Handler(mainLooper))
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
        val scale = minOf(320f / metrics.widthPixels, 385f / metrics.heightPixels, 1f)
        val w = (metrics.widthPixels * scale).toInt().coerceAtLeast(1)
        val h = (metrics.heightPixels * scale).toInt().coerceAtLeast(1)
        reader = ImageReader.newInstance(w, h, android.graphics.PixelFormat.RGBA_8888, 2)
        display = projection?.createVirtualDisplay("RadarPreview", w, h, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, null)
        reader?.setOnImageAvailableListener({ r ->
            if (!busy.compareAndSet(false, true)) { r.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
            var image: Image? = null
            var full: Bitmap? = null
            var cropped: Bitmap? = null
            try {
                image = r.acquireLatestImage()
                if (image != null) {
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * w
                    full = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888)
                    full.copyPixelsFromBuffer(buffer)
                    cropped = Bitmap.createBitmap(full, 0, 0, w, h)
                    // Keep the existing private preview and update one shared gallery image.
                    FileOutputStream(File(cacheDir, "screen_preview.jpg")).use {
                        cropped.compress(Bitmap.CompressFormat.JPEG, 55, it)
                    }
                    writeSharedPreview(cropped)
                }
            } catch (_: Exception) {
                // Screen capture should continue even if gallery storage is unavailable.
            } finally {
                cropped?.recycle()
                full?.recycle()
                image?.close()
                busy.set(false)
            }
        }, null)
        return START_NOT_STICKY
    }

    private fun writeSharedPreview(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT < 29) {
            val folder = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: return
            val file = File(folder, "RadarScreenBridge/screen_preview.jpg")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 55, it) }
            return
        }

        var uri = sharedPreviewUri
        if (uri == null) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "RadarScreenBridge_screen_preview.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RadarScreenBridge")
            }
            uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            sharedPreviewUri = uri
        }
        if (uri != null) {
            contentResolver.openOutputStream(uri, "wt")?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 55, out)
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("capture", "Screen capture", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    override fun onDestroy() {
        display?.release(); reader?.close(); projection?.stop()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
