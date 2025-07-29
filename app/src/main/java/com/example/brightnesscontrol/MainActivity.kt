package com.example.brightnesscontrol

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var brightnessValue: TextView
    private lateinit var touchArea: View
    private lateinit var statusText: TextView
    private lateinit var instructionsText: TextView
    
    private val PERMISSION_REQUEST_CODE = 1001
    private val SYSTEM_ALERT_WINDOW_PERMISSION = 1002
    
    private var currentBrightness = 128
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isGestureActive = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        checkPermissions()
        setupTouchGestures()
        getCurrentBrightness()
    }
    
    private fun initViews() {
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        brightnessValue = findViewById(R.id.brightnessValue)
        touchArea = findViewById(R.id.touchArea)
        statusText = findViewById(R.id.statusText)
        instructionsText = findViewById(R.id.instructionsText)
        
        brightnessSeekBar.max = 255
        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setBrightness(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        instructionsText.text = """
            Instrucciones:
            • Desliza hacia arriba en el área táctil para aumentar brillo
            • Desliza hacia abajo para disminuir brillo
            • También puedes usar la barra deslizante
            • Pellizca con dos dedos para ajuste fino
        """.trimIndent()
    }
    
    private fun setupTouchGestures() {
        touchArea.setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    isGestureActive = true
                    statusText.text = "Gesto iniciado"
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    if (isGestureActive) {
                        val deltaY = lastTouchY - event.y
                        val sensitivity = 2.0f
                        
                        if (abs(deltaY) > 10) {
                            val brightnessChange = (deltaY * sensitivity).toInt()
                            val newBrightness = (currentBrightness + brightnessChange).coerceIn(0, 255)
                            setBrightness(newBrightness)
                            lastTouchY = event.y
                        }
                    }
                    true
                }
                
                MotionEvent.ACTION_POINTER_DOWN -> {
                    statusText.text = "Modo ajuste fino activado"
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    isGestureActive = false
                    statusText.text = "Brillo: $currentBrightness"
                    true
                }
                
                else -> false
            }
        }
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                statusText.text = "Solicitando permisos..."
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION)
            } else {
                statusText.text = "Permisos concedidos"
            }
        }
    }
    
    private fun getCurrentBrightness() {
        try {
            currentBrightness = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            brightnessSeekBar.progress = currentBrightness
            brightnessValue.text = "$currentBrightness"
        } catch (e: Settings.SettingNotFoundException) {
            currentBrightness = 128
            brightnessSeekBar.progress = currentBrightness
            brightnessValue.text = "$currentBrightness"
        }
    }
    
    private fun setBrightness(brightness: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {
                    Settings.System.putInt(
                        contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        brightness
                    )
                    
                    val layoutParams = window.attributes
                    layoutParams.screenBrightness = brightness / 255.0f
                    window.attributes = layoutParams
                    
                    currentBrightness = brightness
                    brightnessSeekBar.progress = brightness
                    brightnessValue.text = "$brightness"
                    statusText.text = "Brillo ajustado: $brightness"
                } else {
                    statusText.text = "Permisos necesarios para cambiar brillo"
                }
            }
        } catch (e: Exception) {
            statusText.text = "Error al cambiar brillo: ${e.message}"
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SYSTEM_ALERT_WINDOW_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {
                    statusText.text = "Permisos concedidos - App lista"
                    getCurrentBrightness()
                } else {
                    statusText.text = "Permisos denegados"
                }
            }
        }
    }
}
