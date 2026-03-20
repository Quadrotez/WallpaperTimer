package com.example.wallpaperapp

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wallpaperapp.databinding.ActivityCropBinding
import java.io.File
import java.io.FileOutputStream

class CropActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCropBinding

    companion object {
        const val EXTRA_URI = "uri"
        const val EXTRA_RESULT_PATH = "result_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriStr = intent.getStringExtra(EXTRA_URI) ?: run { finish(); return }
        try {
            val inputStream = contentResolver.openInputStream(Uri.parse(uriStr))
            val bmp = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bmp == null) { finish(); return }
            binding.cropView.setImage(bmp)
        } catch (e: Exception) {
            finish(); return
        }

        binding.btnConfirm.setOnClickListener {
            val cropped = binding.cropView.getCroppedBitmap() ?: return@setOnClickListener
            val file = File(filesDir, "crop_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_RESULT_PATH, file.absolutePath))
            finish()
        }

        binding.btnSkip.setOnClickListener {
            // Вернуть исходный путь без кропа (уже скопирован)
            val path = intent.getStringExtra("original_path")
            if (path != null) {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_RESULT_PATH, path))
            }
            finish()
        }

        binding.btnCancel.setOnClickListener { setResult(Activity.RESULT_CANCELED); finish() }
    }
}
