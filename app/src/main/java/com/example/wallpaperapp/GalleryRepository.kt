package com.example.wallpaperapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GalleryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("wp_gallery", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<String> {
        val json = prefs.getString("paths", "[]") ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun add(path: String) {
        val list = getAll().toMutableList()
        if (!list.contains(path)) {
            list.add(path)
            save(list)
        }
    }

    fun update(oldPath: String, newPath: String) {
        val list = getAll().toMutableList()
        val idx = list.indexOf(oldPath)
        if (idx >= 0) {
            list[idx] = newPath
            save(list)
        } else {
            list.add(newPath)
            save(list)
        }
    }

    fun delete(path: String) {
        val list = getAll().filter { it != path }
        save(list)
    }

    private fun save(list: List<String>) {
        prefs.edit().putString("paths", gson.toJson(list)).apply()
    }
}
