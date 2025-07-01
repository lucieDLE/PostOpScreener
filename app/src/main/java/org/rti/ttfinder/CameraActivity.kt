package org.rti.ttfinder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.launch
import org.rti.ttfinder.data.preference.AppPreference
import org.rti.ttfinder.data.preference.PrefKey
import org.rti.ttfinder.databinding.ActivityCameraBinding
import org.rti.ttfinder.databinding.LayoutCameraBinding
import org.rti.ttfinder.models.ClassificationModel
import org.rti.ttfinder.models.ImageModel
import org.rti.ttfinder.utils.ClassificationAsyncTask
import org.rti.ttfinder.utils.ImageSavingsAsyncTask
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private var camera: Camera? = null
    private lateinit var binding: ActivityCameraBinding
    private var imageFile: File? = null
    private var imageUri: Uri? = null
    private lateinit var outputDirectory: File
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraBinding: LayoutCameraBinding
    private var isFlash: Boolean = false
    private lateinit var eyeType: String;
    private lateinit var ttId: String;
    private var isAutomaticGradingEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraBinding = binding.cameraView

        eyeType = intent.getStringExtra("eyeType").toString()
        ttId = intent.getStringExtra("ttId").toString()

        isFlash = AppPreference.getInstance(applicationContext).getBoolean(PrefKey.FLASH, false)

        // Request camera permissions
        if (allPermissionsGranted()) {
            cameraBinding.viewFinder.post{
                lifecycleScope.launch {
                    startCamera()
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
        cameraBinding = binding.cameraView

        cameraBinding.flash.setOnClickListener{
            toggleFlash()
        }

        outputDirectory = getOutputDirectory()
        isAutomaticGradingEnabled = AppPreference.getInstance(applicationContext).getBoolean(PrefKey.AUTOMATIC_GRADING_ENABLE, true)

        binding.retake.setOnClickListener {
            retakeImage()
        }
        binding.cancel.setOnClickListener {
            deleteImageWhenRetake()
            finish()
        }
        cameraBinding.captureImageButton.setOnClickListener { takePicture() }
        binding.classifyBtn.setOnClickListener {
            if(isAutomaticGradingEnabled) {
                ClassificationAsyncTask(this@CameraActivity, imageUri) { value ->
                    if(value){
                        retakeImage()
                    }
                }.execute()
            }
            else{
                val image = ImageModel()
                image.imagePath = imageUri?.path
                image.imageName = imageUri?.lastPathSegment
                val model = ClassificationModel()
                model.image = image
                model.imageName = image.imageName

                val returnIntent = Intent()
                returnIntent.putExtra("result", Gson().toJson(model))
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        }
    }

    private suspend fun startCamera() {
        val cameraProvider = ProcessCameraProvider.getInstance(this).await()
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            // Set initial target rotation
            .setTargetRotation(binding.borderView.display.rotation)
            .build()

        val flashMode: Int = if(isFlash)
            FLASH_MODE_ON
        else
            FLASH_MODE_OFF

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            //.setTargetResolution(Size(3000, 3000))
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.borderView.display.rotation)
            .build()

        // Select back camera as a default
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(cameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
        cameraBinding.seekBarZoom.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {
                Log.i(TAG, seekParams.progressFloat.toString())
                camera!!.cameraControl.setLinearZoom(seekParams.progressFloat/100)
            }
            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {}
        }
    }

    /**
     * This will save image automatically after capture from camera
     */
    private fun savePicture() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val eyeName = if (eyeType == "Right") "OD" else "OS"
        val imageName = "${ttId}_${eyeName}_${timeStamp}.jpg"
        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            imageName
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        handleCaptureBtnLoader(true)
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    handleCaptureBtnLoader(false)
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    showImage()
                    val savedUri = Uri.fromFile(photoFile)
                    imageUri = savedUri
                    onImageCaptured(savedUri)
                    val msg = "Photo capture succeeded: $savedUri"
                    //Toast.makeText(this@CameraActivity, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    handleCaptureBtnLoader(false)
                }
            }
        )
    }

    /**
     * This will need to save image manually after capture from camera
     */
    private fun takePicture() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val eyeName = if (eyeType == "Right") "OD" else "OS"
        val imageName = "${ttId}_${eyeName}_${timeStamp}.jpg"
        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            imageName
        )

        // set image URI
        val savedUri = Uri.fromFile(photoFile)
        imageUri = savedUri

        // Set up image capture listener, which is triggered after photo has been taken
        handleCaptureBtnLoader(true)
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
                override fun onCaptureSuccess(image: ImageProxy) {
                    println("onCaptureSuccess")
                    handleCaptureBtnLoader(false)
                    val imgProxy = image.image?: return
                    var bitmap = imgProxy.toBitmap()
                    image.close()
                    val rotationDegrees = image.imageInfo.rotationDegrees.toFloat()
                    if (rotationDegrees != 0f) {
                        val matrix = Matrix()
                        matrix.postRotate(rotationDegrees)
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    ImageSavingsAsyncTask(bitmap, photoFile.absolutePath, rotationDegrees).execute()
                    // show the preview
                    binding.image.setImageBitmap(bitmap)
                    showImage()
                }
            }
        )
    }

    private fun showImage() {
        binding.image.visibility = VISIBLE
        binding.layout.visibility = VISIBLE
        binding.borderView.visibility = View.GONE
        binding.cameraView.root.visibility = View.GONE
    }

    private fun onImageCaptured(uri: Uri) {
        val file = File(uri.path!!)
        imageFile = file
        binding.image.setImageURI(uri);
    }

    private fun retakeImage(){
        binding.cameraView.root.visibility = View.VISIBLE
        binding.image.visibility = View.INVISIBLE
        //binding.borderView.visibility = View.VISIBLE
        binding.layout.visibility = View.GONE
        deleteImageWhenRetake()
    }

    private fun handleCaptureBtnLoader(show: Boolean){
        if(show){
            cameraBinding.pbImageCaptureLoader.visibility = View.VISIBLE
            cameraBinding.captureImageButton.isEnabled = false
        }
        else{
            cameraBinding.pbImageCaptureLoader.visibility = View.INVISIBLE
            cameraBinding.captureImageButton.isEnabled = true
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val root: String
        root = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString()
        } else {
            Environment.getExternalStorageDirectory().toString()
        }
        val myDir = File("$root/TTScreener/Images")
        myDir.mkdirs()
        return if (myDir != null && myDir.exists())
            myDir else filesDir
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraBinding.viewFinder.post{
                    lifecycleScope.launch {
                        startCamera()
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                // finish()
            }
            return
        }
    }

    fun alert(message: String?) {
        val bld = AlertDialog.Builder(MyApplication.getContext())
        bld.setMessage(message)
        bld.setCancelable(false)
        bld.setNeutralButton(
            "OK"
        ) { dialog, which -> dialog.dismiss() }
        bld.create().show()
    }

    private fun deleteImageWhenRetake(){
        val file = File(imageUri?.path)
        if (file.exists()) {
            if (file.delete()) {
                println("file Deleted :${imageUri?.path}")
            } else {
                println("file not Deleted :${imageUri?.path}")
            }
        }
    }

    private fun toggleFlash() {
        if (isFlash) {
            isFlash = false
            cameraBinding.flash.setImageResource(R.drawable.ic_flash_off)
        } else {
            isFlash = true
            cameraBinding.flash.setImageResource(R.drawable.ic_flash_on)
        }
        AppPreference.getInstance(applicationContext).setBoolean(PrefKey.FLASH, isFlash)
        cameraBinding.viewFinder.post{
            lifecycleScope.launch {
                startCamera()
            }
        }
    }

    fun Image.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    companion object {
        internal const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}