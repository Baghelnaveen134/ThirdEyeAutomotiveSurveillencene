package com.example.thirdeyeautomotivesurveillence

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        val imageView: ImageView = findViewById(R.id.imageView)
        val imagePath = intent.getStringExtra("IMAGE_PATH")

        if (imagePath != null) {
            Glide.with(this).load(imagePath).into(imageView)
        }
    }
}