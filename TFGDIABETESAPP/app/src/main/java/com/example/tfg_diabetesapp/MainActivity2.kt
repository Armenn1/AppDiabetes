package com.example.tfg_diabetesapp

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button


class MainActivity2 : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        val btnPlay = findViewById<Button>(R.id.btnPlay)
        val btnPause = findViewById<Button>(R.id.btnPause)
        val btnStop = findViewById<Button>(R.id.btnStop)

        mediaPlayer = MediaPlayer.create(this, R.raw.shapeofyou1)

        btnPlay.setOnClickListener {
            mediaPlayer.start()
        }

        btnPause.setOnClickListener {
            if(mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
        }

        btnStop.setOnClickListener {
            if(mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer = MediaPlayer.create(this, R.raw.shapeofyou1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()

    }
}