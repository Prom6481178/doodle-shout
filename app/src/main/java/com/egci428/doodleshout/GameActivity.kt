package com.egci428.doodleshout

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlin.math.abs
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button

class GameActivity : AppCompatActivity(), SensorEventListener {

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gameView: GameView? = null
    private var audioRecord: AudioRecord? = null // Sensor for detecting sound from microphone
    private var audioThread: Thread? = null // Sensor for detecting the sound strength
    private var isRecording = false
    private val audioBufferSize = AudioRecord.getMinBufferSize(
        44100,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) // calculates the minimum buffer size needed for audio recording in Android
    private val audioReadBufferSize = 512 // Smaller buffer for lower latency
    private val jumpThreshold = 16000 // Adjust for sensitivity
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        gameView = findViewById(R.id.gridView)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        button = findViewById<Button>(R.id.menuButton)

        button.setOnClickListener {
            finish()
        }

        // Set up game over callback
        gameView?.setGameOverCallback {
            Log.d("DoodleDebug", "Game over callback triggered")
            button.visibility = View.VISIBLE
        }

        // Set up restart game callback
        gameView?.setRestartGameCallback {
            Log.d("DoodleDebug", "Restart")
            button.visibility = View.INVISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        // Start audio recording if permission granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAudioRecording()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        stopAudioRecording()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            gameView?.onAccelerometerChanged(x)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Handle touch events to restart the game
        if (event?.action == MotionEvent.ACTION_DOWN) {
            if (gameView?.isGameOver() == true) {
                gameView?.restartGame()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startAudioRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        }
        if (audioRecord == null) {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                audioBufferSize
            )
        }
        isRecording = true
        audioRecord?.startRecording()

        /* Start a new thread for detecting sound strength.
           Handle the case that if the sensor receive louder sound,
           then the character should jump higher */
        audioThread = Thread {
            val buffer = ShortArray(audioReadBufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val max = buffer.take(read).maxOf { value: Short -> abs(value.toInt()) }
                    if (max > jumpThreshold) {
                        // Map loudness to jump strength (e.g., 1.0 to 2.5)
                        val strength = 1.0f + ((max - jumpThreshold).coerceAtMost(30000) / 30000f) * 1.5f
                        runOnUiThread {
                            gameView?.boost(strength)
                        }
                    }
                }
                Thread.sleep(10)
            }
        }
        audioThread?.start()
    }

    private fun stopAudioRecording() {
        isRecording = false
        audioThread?.join(200)
        audioThread = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}