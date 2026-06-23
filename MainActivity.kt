package com.realme.fpsbooster

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.realme.fpsbooster.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) { return }

        binding.btnLockHardware.setOnClickListener {
            if (Settings.System.canWrite(this)) applyGlitchLock() else askPermission()
        }

        binding.btnCameraLatch.setOnClickListener {
            latchHardware()
        }

        binding.btnStartWatchdog.setOnClickListener {
            launchOptimizedGameMode()
        }

        binding.btnStopAll.setOnClickListener {
            if (Settings.System.canWrite(this)) shutdownAndRestore() else askPermission()
        }
    }

    private fun askPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
        Toast.makeText(this, "Enable 'Allow modify system settings'", Toast.LENGTH_LONG).show()
    }

    private fun applyGlitchLock() {
        try {
            val resolver = contentResolver
            
            // Prime the system to 120Hz first
            Settings.System.putFloat(resolver, "peak_refresh_rate", 120.0f)
            Settings.System.putFloat(resolver, "min_refresh_rate", 120.0f)
            Settings.System.putInt(resolver, "user_refresh_rate", 2) 
            
            Toast.makeText(this, "Priming Hardware Engine...", Toast.LENGTH_SHORT).show()

            // Wait 300ms, then inject glitch
            handler.postDelayed({
                Settings.System.putFloat(resolver, "peak_refresh_rate", 59.99f)
                Settings.System.putFloat(resolver, "min_refresh_rate", 59.99f)
                Settings.System.putInt(resolver, "user_refresh_rate", 1)
                Toast.makeText(this@MainActivity, "Hardware Uncapped! Tap Step 2.", Toast.LENGTH_SHORT).show()
            }, 300)
            
        } catch (e: Exception) { }
    }

    private fun latchHardware() {
        try {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            if (intent.resolveActivity(packageManager) != null) startActivity(intent)
        } catch (e: Exception) { }
    }

    private fun launchOptimizedGameMode() {
        try {
            startService(Intent(this, RamMonitorService::class.java))
            Toast.makeText(this, "Performance Watchdog active.", Toast.LENGTH_SHORT).show()

            val launchIntent = packageManager.getLaunchIntentForPackage("com.pubg.imobile")
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "BGMI package not located.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) { }
    }

    private fun shutdownAndRestore() {
        try {
            stopService(Intent(this, RamMonitorService::class.java))
            val resolver = contentResolver

            // STEP 1: Flush the driver by pushing it to High (120Hz) so it doesn't get stuck
            Settings.System.putFloat(resolver, "peak_refresh_rate", 120.0f)
            Settings.System.putFloat(resolver, "min_refresh_rate", 120.0f)
            Settings.System.putInt(resolver, "user_refresh_rate", 2)
            
            Toast.makeText(this, "Flushing Display Driver...", Toast.LENGTH_SHORT).show()

            // STEP 2: Wait 400ms, then strictly lock the phone into Standard (60Hz) everywhere
            handler.postDelayed({
                Settings.System.putFloat(resolver, "peak_refresh_rate", 60.0f)
                Settings.System.putFloat(resolver, "min_refresh_rate", 60.0f)
                Settings.System.putInt(resolver, "user_refresh_rate", 1)
                
                Toast.makeText(this@MainActivity, "Optimizer disabled. Standard 60Hz Restored.", Toast.LENGTH_LONG).show()
            }, 400)
            
        } catch (e: Exception) { }
    }
}