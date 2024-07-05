package com.example.thirdeyeautomotivesurveillence

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        val videoView: VideoView = findViewById(R.id.videoView)
        val videoPath = intent.getStringExtra("VIDEO_PATH")

        if (videoPath != null) {
            val uri = Uri.parse(videoPath)
            videoView.setVideoURI(uri)
            videoView.setMediaController(MediaController(this))
            videoView.start()
        }
    }
}