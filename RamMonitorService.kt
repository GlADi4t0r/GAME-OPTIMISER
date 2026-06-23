package com.realme.fpsbooster

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class RamMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val cleanInterval = 120000L // Keeps memory clean every 2 minutes

    private val ramCleanerRunnable = object : Runnable {
        override fun run() {
            safeUserRamSweep()
            handler.postDelayed(this, cleanInterval)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "OPTIMIZER_ENGINE")
                .setContentTitle("BGMI Optimizer Active")
                .setContentText("Guarding RAM allocation safely...")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("BGMI Optimizer Active")
                .setContentText("Guarding RAM allocation safely...")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .build()
        }

        startForeground(1, notification)
        handler.post(ramCleanerRunnable)
        return START_STICKY
    }

    private fun safeUserRamSweep() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val packages = packageManager.getInstalledApplications(0)
            
            for (packageInfo in packages) {
                val appName = packageInfo.packageName
                
                // CRUCIAL LAG FIX: If it is a core Android/Realme system file, completely ignore it.
                if ((packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                    continue
                }

                // Protects your optimizer app and BGMI from getting terminated
                if (appName != packageName && appName != "com.pubg.imobile") {
                    am.killBackgroundProcesses(appName)
                }
            }
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(ramCleanerRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? { return null }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "OPTIMIZER_ENGINE",
                "RAM Optimization",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}