package com.example.thirdeyeautomotivesurveillence

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import com.example.thirdeyeautomotivesurveillence.ui.theme.ThirdEyeAutomotiveSurveillenceTheme

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.Recording
import androidx.camera.video.FileOutputOptions
import androidx.core.content.ContextCompat
import androidx.work.*
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {
    private lateinit var previewView: androidx.camera.view.PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var videoRecording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isVibrationDetected = false

    private lateinit var locationManager: LocationManager
    private var currentLocation: Location? = null

    private val PHONE_NUMBER = "1234567890" // Replace with actual phone number

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize OpenCV (if needed)
        if (!OpenCVLoader.initDebug()) {
            Log.e("MainActivity", "OpenCV initialization failed")
        } else {
            Log.d("MainActivity", "OpenCV initialization succeeded")
        }

        // Initialize CameraX and UI
        previewView = findViewById(R.id.previewView)
        val buttonCaptureImage: Button = findViewById(R.id.button_capture_image)
        val buttonRecordVideo: Button = findViewById(R.id.button_record_video)

        // Request camera and storage permissions
        requestPermissions()

        // Initialize sensor manager and accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Set up the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize location manager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Set up the camera
        startCamera()

        buttonCaptureImage.setOnClickListener { captureImage() }
        buttonRecordVideo.setOnClickListener { recordVideo() }
    }

    override fun onResume() {
        super.onResume()
        // Register accelerometer sensor listener
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        // Request location updates
        requestLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        // Unregister accelerometer sensor listener
        sensorManager.unregisterListener(this)
        // Stop location updates to save battery
        locationManager.removeUpdates(this)
    }

    private fun requestPermissions() {
        val requestPermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[Manifest.permission.CAMERA] == true &&
                    permissions[Manifest.permission.RECORD_AUDIO] == true &&
                    permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true &&
                    permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true &&
                    permissions[Manifest.permission.SEND_SMS] == true
                ) {
                    startCamera()
                    requestLocationUpdates()
                }
            }

        requestPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS
            )
        )
    }

    private fun requestLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000L, // Request location updates every 60 seconds
                10f,
                this
            )
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Location permission not granted", e)
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val recorder = Recorder.Builder()
                .setExecutor(cameraExecutor)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // ImageAnalysis for motion detection
            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor, MotionDetectionAnalyzer())
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("MainActivity", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {
        val timestamp = System.currentTimeMillis()
        val location = currentLocation
        val locationString = if (location != null) {
            "LAT_${location.latitude}_LON_${location.longitude}"
        } else {
            "LAT_UNKNOWN_LON_UNKNOWN"
        }
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            "IMG_${timestamp}_$locationString.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("MainActivity", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("MainActivity", "Photo capture succeeded: ${photoFile.absolutePath}")
                    processAndCompressImage(photoFile)
                    sendAlertWithImage("Vibration detected! See attached image.", photoFile.absolutePath)
                }
            }
        )
    }

    private fun processAndCompressImage(photoFile: File) {
        // Load the image into a Bitmap
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

        // Process the image if needed (e.g., resize)
        val processedBitmap = processImage(bitmap)

        // Save the compressed image
        saveCompressedImage(photoFile, processedBitmap)
    }

    private fun processImage(bitmap: Bitmap): Bitmap {
        // Example of resizing the image
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = width / 2
        val newHeight = height / 2

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun saveCompressedImage(photoFile: File, bitmap: Bitmap) {
        val outputFile = File(photoFile.parent, "COMPRESSED_${photoFile.name}")

        try {
            FileOutputStream(outputFile).use { outputStream ->
                // Compress the image to JPEG with 85% quality
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                Log.d("MainActivity", "Compressed image saved: ${outputFile.absolutePath}")
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "Error saving compressed image: ${e.message}", e)
        }
    }

    private fun recordVideo() {
        if (videoRecording != null) {
            // Stop the current recording
            videoRecording?.stop()
            videoRecording = null
        } else {
            val timestamp = System.currentTimeMillis()
            val location = currentLocation
            val locationString = if (location != null) {
                "LAT_${location.latitude}_LON_${location.longitude}"
            } else {
                "LAT_UNKNOWN_LON_UNKNOWN"
            }
            val videoFile = File(
                externalMediaDirs.firstOrNull(),
                "VID_${timestamp}_$locationString.mp4"
            )

            val outputOptions = FileOutputOptions.Builder(videoFile).build()
            videoRecording = videoCapture.output
                .prepareRecording(this, outputOptions)
                .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.d("MainActivity", "Video recording started")
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (!recordEvent.hasError()) {
                                Log.d("MainActivity", "Video recording succeeded: ${videoFile.absolutePath}")
                            } else {
                                Log.e("MainActivity", "Video recording failed: ${recordEvent.error}")
                            }
                        }
                        else -> {
                            Log.d("MainActivity", "Recording event: $recordEvent")
                        }
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shutdown camera executor
        cameraExecutor.shutdown()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Process accelerometer sensor data for vibration detection
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate magnitude of acceleration
            val magnitude = Math.sqrt((x * x + y * y + z * z).toDouble())

            // Example threshold for vibration detection (adjust as needed)
            if (magnitude > 15) {
                // Vibration detected, trigger action
                handleVibrationDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun handleVibrationDetected() {
        // Implement your action when vibration is detected
        Log.d("MainActivity", "Vibration detected!")
        isVibrationDetected = true
        startTakingPhotos() // Example action: Capture image on vibration detection
        sendAlert("Vibration detected!")
    }

    private fun startTakingPhotos() {
        handler.post(object : Runnable {
            override fun run() {
                if (isVibrationDetected) {
                    captureImage()
                    handler.postDelayed(this, TimeUnit.SECONDS.toMillis(2))
                }
            }
        })
    }

    private fun stopTakingPhotos() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onProviderDisabled(provider: String) {
        // Handle provider disabled
    }

    override fun onProviderEnabled(provider: String) {
        // Handle provider enabled
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {
        // Handle status changes
    }

    private inner class MotionDetectionAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            // Implement motion detection analysis here if needed
            image.close()
        }
    }

    private fun sendAlert(message: String) {
        SMSUtil.sendTextMessage(this, PHONE_NUMBER, message)
    }

    private fun sendAlertWithImage(message: String, imagePath: String) {
        // Sending MMS can be more complex and may need additional permissions and implementations.
        // Here we just send an SMS with a message including the image path.
        SMSUtil.sendTextMessage(this, PHONE_NUMBER, "$message Image path: $imagePath")
    }
}


