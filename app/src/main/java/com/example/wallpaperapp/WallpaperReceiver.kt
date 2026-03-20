package com.example.wallpaperapp

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build

class WallpaperReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("schedule_id") ?: return
        val repo = ScheduleRepository(context)
        val schedule = repo.getById(id) ?: return
        if (!schedule.isEnabled || schedule.imagePaths.isEmpty()) return

        val path = schedule.imagePaths[schedule.currentIndex % schedule.imagePaths.size]
        try {
            val bitmap = BitmapFactory.decodeFile(path) ?: return
            val wm = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            } else {
                wm.setBitmap(bitmap)
            }
            val nextIndex = (schedule.currentIndex + 1) % schedule.imagePaths.size
            val updated = schedule.copy(currentIndex = nextIndex)
            repo.save(updated)
            ScheduleManager(context).scheduleNext(updated)
        } catch (e: Exception) {
            e.printStackTrace()
            ScheduleManager(context).scheduleNext(schedule)
        }
    }
}
