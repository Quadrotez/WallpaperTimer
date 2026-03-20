package com.example.wallpaperapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class ScheduleManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNext(schedule: WallpaperSchedule) {
        if (!schedule.isEnabled || schedule.imagePaths.isEmpty()) return
        val triggerTime = getNextTriggerTime(schedule) ?: return
        val pi = buildPendingIntent(schedule.id) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
        }
    }

    fun cancel(scheduleId: String) {
        val pi = buildPendingIntent(scheduleId, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pi?.let { alarmManager.cancel(it) }
    }

    fun rescheduleAll() {
        val repo = ScheduleRepository(context)
        repo.getAll().filter { it.isEnabled }.forEach { scheduleNext(it) }
    }

    fun getNextTriggerTime(schedule: WallpaperSchedule): Long? {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.hour)
            set(Calendar.MINUTE, schedule.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        repeat(8) {
            if (isValidDay(schedule, cal)) return cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    private fun isValidDay(schedule: WallpaperSchedule, cal: Calendar): Boolean {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return when (schedule.dayType) {
            WallpaperSchedule.DAY_EVERY    -> true
            WallpaperSchedule.DAY_WEEKDAYS -> dow in listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
            WallpaperSchedule.DAY_WEEKLY   -> (schedule.specificDays.firstOrNull() ?: Calendar.MONDAY) == dow
            WallpaperSchedule.DAY_SPECIFIC -> dow in schedule.specificDays
            else -> true
        }
    }

    private fun buildPendingIntent(scheduleId: String, flags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE): PendingIntent? {
        val intent = Intent(context, WallpaperReceiver::class.java).apply {
            action = "com.example.wallpaperapp.CHANGE"
            putExtra("schedule_id", scheduleId)
        }
        return PendingIntent.getBroadcast(context, scheduleId.hashCode(), intent, flags)
    }
}
