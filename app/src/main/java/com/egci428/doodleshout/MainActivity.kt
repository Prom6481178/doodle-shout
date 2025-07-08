package com.egci428.doodleshout

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null // Media player for background music
    private var isMuted = false
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    companion object {
        private lateinit var leaderboardText: TextView
        public lateinit var SQLiteHelper: MySQLiteHelper

        // Update leaderboard text
        public fun updateLeaderboard() {
            try {
                val topScore = SQLiteHelper.getTopScores(5)
                var str = ""
                for (i in 0..<topScore.size) {
                    val score = topScore[i]
                    str += "#${i + 1}    -     ${score.score}\n"
                }
                leaderboardText.text = str
            }
            catch (e: Exception) {
                Log.d("DoodleDebug", Log.getStackTraceString(e))
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the database
        SQLiteHelper = MySQLiteHelper(this)

        val startButton = findViewById<Button>(R.id.startButton)
        val exitButton = findViewById<Button>(R.id.exitButton)
        val speakerButton = findViewById<ImageButton>(R.id.speakerButton)
        leaderboardText = findViewById<TextView>(R.id.leaderboardText)

        // Starting button with condition to check if the microphone permission is granted, else request it
        startButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)
            }
            else {
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            }
        }

        // Exit button to exit the game
        exitButton.setOnClickListener {
            finishAffinity()
        }

        /* Set up the media player to run the background music in loop.
         User allow to mute and un-mute the music by pressing the speaker button*/
        mediaPlayer = MediaPlayer.create(this, R.raw.bgmusic)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(1.0f, 1.0f)
        mediaPlayer?.start()
        speakerButton.setOnClickListener {
            isMuted = !isMuted
            if (isMuted) {
                mediaPlayer?.setVolume(0f, 0f)
                speakerButton.setImageResource(android.R.drawable.ic_lock_silent_mode)
            } else {
                mediaPlayer?.setVolume(1f, 1f)
                speakerButton.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            }
        }

        // Everytime when the user done a game in each round, update the leaderboard
        updateLeaderboard()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}