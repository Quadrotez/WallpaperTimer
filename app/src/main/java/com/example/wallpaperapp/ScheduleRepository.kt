package com.example.wallpaperapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScheduleRepository(context: Context) {
    private val prefs = context.getSharedPreferences("wp_schedules", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<WallpaperSchedule> {
        val json = prefs.getString("list", "[]") ?: "[]"
        val type = object : TypeToken<List<WallpaperSchedule>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun save(schedule: WallpaperSchedule) {
        val list = getAll().toMutableList()
        val idx = list.indexOfFirst { it.id == schedule.id }
        if (idx >= 0) list[idx] = schedule else list.add(schedule)
        prefs.edit().putString("list", gson.toJson(list)).apply()
    }

    fun delete(id: String) {
        val list = getAll().filter { it.id != id }
        prefs.edit().putString("list", gson.toJson(list)).apply()
    }

    fun getById(id: String): WallpaperSchedule? = getAll().find { it.id == id }

    fun update(id: String, transform: (WallpaperSchedule) -> WallpaperSchedule) {
        getById(id)?.let { save(transform(it)) }
    }
}
