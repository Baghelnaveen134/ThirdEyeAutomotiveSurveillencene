package com.example.thirdeyeautomotivesurveillence
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class OwnerActivity : AppCompatActivity() {

    private lateinit var lvAlerts: ListView
    private lateinit var lvMedia: ListView

    private val alerts = mutableListOf<String>()
    private val mediaFiles = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner)

        lvAlerts = findViewById(R.id.lvAlerts)
        lvMedia = findViewById(R.id.lvMedia)

        setupAlertsListView()
        setupMediaListView()
    }

    private fun setupAlertsListView() {
        // Load alerts (For simplicity, using hardcoded alerts. You should load actual alerts from your data source)
        alerts.add("Vibration detected at 2024-07-04 10:00")
        alerts.add("Vibration detected at 2024-07-04 10:02")

        val alertsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, alerts)
        lvAlerts.adapter = alertsAdapter
    }

    private fun setupMediaListView() {
        // Load media files (For simplicity, using hardcoded file names. You should load actual files from your storage)
        val mediaDir = getExternalFilesDir(null)
        if (mediaDir != null && mediaDir.isDirectory) {
            mediaFiles.addAll(mediaDir.listFiles()!!.filter { it.isFile })
        }

        val mediaAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mediaFiles.map { it.name })
        lvMedia.adapter = mediaAdapter

        lvMedia.setOnItemClickListener { _, _, position, _ ->
            val selectedFile = mediaFiles[position]
            if (selectedFile.extension == "jpg" || selectedFile.extension == "jpeg") {
                // Open ImageActivity to view the image
                val intent = Intent(this, ImageActivity::class.java).apply {
                    putExtra("IMAGE_PATH", selectedFile.absolutePath)
                }
                startActivity(intent)
            } else if (selectedFile.extension == "mp4") {
                // Open VideoActivity to play the video
                val intent = Intent(this, VideoActivity::class.java).apply {
                    putExtra("VIDEO_PATH", selectedFile.absolutePath)
                }
                startActivity(intent)
            }
        }
    }
}