package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.loadDrawable
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.ui.ApkIOUtils
import kotlinx.android.synthetic.main.activity_capture_image.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class CaptureImageActivity : AppCompatActivity() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private lateinit var previewView: PreviewView

    private lateinit var imagePreview: Preview
    private lateinit var imageCapture: ImageCapture

    private lateinit var cameraControl: CameraControl
    private lateinit var cameraInfo: CameraInfo

    private lateinit var cameraCaptureButton: ImageButton
    private lateinit var cameraTorchButton: ImageButton

    private val executor = Executors.newSingleThreadExecutor()

    companion object {

        const val RESULT_PATH = "RESULT_PATH"

        private const val TAG = "CaptureImageActivity"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private lateinit var outputDirectory: File

        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, CaptureImageActivity::class.java)
        }

        private fun createFile(baseFolder: File, format: String, extension: String): File {
            val file = File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
            ApkIOUtils.ensureFilePath(file)
            return file
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_image)
        previewView = activity_capture_image_preview_view

        // Let's put images into the `files\camera` folder
        outputDirectory = File(this.filesDir, "camera")

        cameraCaptureButton = activity_capture_image_camera_capture_button
        cameraTorchButton = activity_capture_image_camera_torch_button

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        if (allPermissionsGranted()) {
            previewView.post { startCamera() }
            setupButtons()
        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun startCamera() {

        imagePreview = Preview.Builder().apply {
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
            setTargetRotation(previewView.display.rotation)
        }.build()

        imageCapture = ImageCapture.Builder().apply {
            setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        }.build()

        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imagePreview,
                imageCapture
            )
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            setTorchStateObserver()

            previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            imagePreview.setSurfaceProvider(previewView.surfaceProvider)
        }, ContextCompat.getMainExecutor(this))

    }

    private fun takePicture() {

        // Create destination file
        val file = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions, executor, object : ImageCapture.OnImageSavedCallback {

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                lifecycleScope.launchWhenCreated {
                    Log.d(TAG, "Photo captured to: ${file.path}")
                    val data = Intent().apply {
                        putExtra(RESULT_PATH, file.path)
                    }
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                val msg = "Photo capture failed: ${exception.message}"
                lifecycleScope.launchWhenCreated {
                    longToast(msg)
                }
            }

        })
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                activity_capture_image_preview_view.post { startCamera() }
            } else {
                toast("Permissions not granted by the user.")
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
               baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupButtons() {
        cameraCaptureButton.setOnClickListener {
            takePicture()
        }
        cameraTorchButton.setOnClickListener {
            toggleTorch()
        }
    }

    private fun toggleTorch() {
        if (cameraInfo.torchState.value == TorchState.ON) {
            cameraControl.enableTorch(false)
        } else {
            cameraControl.enableTorch(true)
        }
    }

    private fun setTorchStateObserver() {
        cameraInfo.torchState.observe(this, Observer { state ->
            if (state == TorchState.ON) {
                cameraTorchButton.setImageDrawable(
                    loadDrawable(
                        R.drawable.ic_flash_on_24px
                    )
                )
            } else {
                cameraTorchButton.setImageDrawable(
                    loadDrawable(
                        R.drawable.ic_flash_off_24px
                    )
                )
            }
        })
    }
}

