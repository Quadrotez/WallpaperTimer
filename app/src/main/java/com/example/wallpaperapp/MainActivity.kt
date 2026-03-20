package com.example.wallpaperapp

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.wallpaperapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null
    private var selectedMode: Int = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK

    // Результат выбора изображения из галереи
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                showPreview(uri)
            }
        }
    }

    // Результат запроса разрешения
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Необходимо разрешение для доступа к галерее", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Кнопка выбора изображения
        binding.btnPickImage.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        // Радиокнопки выбора режима
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedMode = when (checkedId) {
                R.id.radioHome -> WallpaperManager.FLAG_SYSTEM
                R.id.radioLock -> WallpaperManager.FLAG_LOCK
                R.id.radioBoth -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
        }

        // Кнопка установки обоев
        binding.btnSetWallpaper.setOnClickListener {
            if (selectedImageUri != null) {
                setWallpaper()
            } else {
                Toast.makeText(this, "Сначала выберите изображение", Toast.LENGTH_SHORT).show()
            }
        }

        // По умолчанию выбран режим "оба"
        binding.radioBoth.isChecked = true
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                Toast.makeText(this, "Нужен доступ к галерее для выбора изображения", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun showPreview(uri: Uri) {
        binding.imagePreview.setImageURI(uri)
        binding.imagePreview.visibility = View.VISIBLE
        binding.tvPreviewHint.visibility = View.GONE
        binding.cardPreview.visibility = View.VISIBLE
        binding.btnSetWallpaper.isEnabled = true
        binding.tvSelectedFile.text = getFileName(uri)
        binding.tvSelectedFile.visibility = View.VISIBLE
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "Файл выбран"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    private fun setWallpaper() {
        val uri = selectedImageUri ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSetWallpaper.isEnabled = false
        binding.btnSetWallpaper.text = "Устанавливаю..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val wallpaperManager = WallpaperManager.getInstance(this@MainActivity)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, selectedMode)
                } else {
                    // Для Android < 7.0 устанавливаем только системные обои
                    wallpaperManager.setBitmap(bitmap)
                }

                withContext(Dispatchers.Main) {
                    val message = when (selectedMode) {
                        WallpaperManager.FLAG_SYSTEM -> "Обои рабочего стола установлены!"
                        WallpaperManager.FLAG_LOCK -> "Обои экрана блокировки установлены!"
                        else -> "Обои установлены везде!"
                    }
                    showSuccess(message)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Ошибка: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun showSuccess(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnSetWallpaper.isEnabled = true
        binding.btnSetWallpaper.text = "Установить обои"
        binding.statusCard.visibility = View.VISIBLE
        binding.statusCard.setCardBackgroundColor(getColor(R.color.success_green))
        binding.tvStatus.text = "✓ $message"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnSetWallpaper.isEnabled = true
        binding.btnSetWallpaper.text = "Установить обои"
        binding.statusCard.visibility = View.VISIBLE
        binding.statusCard.setCardBackgroundColor(getColor(R.color.error_red))
        binding.tvStatus.text = "✗ $message"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
