package com.example.wallpaperapp

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WallpaperApplier {

    suspend fun apply(context: Context, imagePath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                    ?: return@withContext Result.failure(Exception("Не удалось прочитать изображение"))
                val wm = WallpaperManager.getInstance(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setBitmap(bitmap, null, true,
                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                } else {
                    wm.setBitmap(bitmap)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
