package com.example.flashlight

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlashlightManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraId: String? = null
    private var strobeJob: Job? = null
    private var isTorchOn = false

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        try {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) && cameraManager != null) {
                for (id in cameraManager.cameraIdList) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = id
                        break
                    }
                    if (hasFlash && cameraId == null) {
                        cameraId = id
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FlashlightManager", "Error initializing flashlight: ${e.message}")
        }
    }

    fun isAvailable(): Boolean {
        return cameraId != null
    }

    fun setTorch(on: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager?.setTorchMode(id, on)
            isTorchOn = on
        } catch (e: CameraAccessException) {
            Log.e("FlashlightManager", "CameraAccessException: ${e.message}")
        } catch (e: Exception) {
            Log.e("FlashlightManager", "Flashlight error: ${e.message}")
        }
    }

    fun startStrobe(scope: CoroutineScope, intervalMs: Long = 250L) {
        if (!isAvailable()) return
        stopStrobe()
        strobeJob = scope.launch(Dispatchers.Default) {
            var state = false
            try {
                while (isActive) {
                    state = !state
                    setTorch(state)
                    delay(intervalMs)
                }
            } finally {
                setTorch(false)
            }
        }
    }

    fun stopStrobe() {
        strobeJob?.cancel()
        strobeJob = null
        setTorch(false)
    }
}
