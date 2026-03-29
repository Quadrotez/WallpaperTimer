package com.example.wallpaperapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.wallpaperapp.databinding.ActivityGalleryBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var galleryRepo: GalleryRepository
    private lateinit var adapter: GalleryAdapter
    private var pendingCropIndex = -1
    private var pendingCropOldPath = ""
    private var pickMode = false

    companion object {
        const val EXTRA_PICK_MODE = "pick_mode"
        const val EXTRA_SELECTED_PATHS = "selected_paths"
        private const val REQ_PICK = 201
        private const val REQ_CROP = 202
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pickMode = intent.getBooleanExtra(EXTRA_PICK_MODE, false)
        galleryRepo = GalleryRepository(this)

        val paths = galleryRepo.getAll().toMutableList()
        adapter = GalleryAdapter(
            paths,
            onCrop = { i -> startCrop(i) },
            onDelete = { i -> confirmDelete(i) },
            onPick = if (pickMode) { _ -> } else null
        )
        adapter.pickMode = pickMode

        binding.rvGallery.apply {
            layoutManager = GridLayoutManager(this@GalleryActivity, 3)
            adapter = this@GalleryActivity.adapter
        }

        binding.btnAddToGallery.setOnClickListener { openGallery() }
        binding.btnBack.setOnClickListener { finish() }

        if (pickMode) {
            binding.btnConfirmPick.visibility = View.VISIBLE
            binding.btnConfirmPick.setOnClickListener { confirmPick() }
            binding.tvTitle.text = "Выбрать фото"
        } else {
            binding.btnConfirmPick.visibility = View.GONE
            binding.tvTitle.text = "Галерея обоев"
        }

        updateEmpty()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQ_PICK)
    }

    private fun startCrop(index: Int) {
        pendingCropIndex = index
        pendingCropOldPath = adapter.getPath(index)
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        val intent = Intent(this, CropActivity::class.java).apply {
            putExtra(CropActivity.EXTRA_URI, Uri.fromFile(File(pendingCropOldPath)).toString())
            putExtra("original_path", pendingCropOldPath)
            putExtra(CropActivity.EXTRA_SCREEN_W, dm.widthPixels)
            putExtra(CropActivity.EXTRA_SCREEN_H, dm.heightPixels)
        }
        startActivityForResult(intent, REQ_CROP)
    }

    private fun confirmDelete(index: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить фото?")
            .setMessage("Фото будет удалено из галереи")
            .setPositiveButton("Удалить") { _, _ ->
                val path = adapter.getPath(index)
                galleryRepo.delete(path)
                adapter.removePath(index)
                updateEmpty()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmPick() {
        val selected = adapter.selectedPaths.toList()
        if (selected.isEmpty()) {
            Toast.makeText(this, "Выберите хотя бы одно фото", Toast.LENGTH_SHORT).show()
            return
        }
        val result = Intent().apply {
            putStringArrayListExtra(EXTRA_SELECTED_PATHS, ArrayList(selected))
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_PICK -> if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data ?: return
                val copied = copyToInternal(uri) ?: return
                val dm = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(dm)
                pendingCropIndex = -1
                pendingCropOldPath = copied
                val intent = Intent(this, CropActivity::class.java).apply {
                    putExtra(CropActivity.EXTRA_URI, Uri.fromFile(File(copied)).toString())
                    putExtra("original_path", copied)
                    putExtra(CropActivity.EXTRA_SCREEN_W, dm.widthPixels)
                    putExtra(CropActivity.EXTRA_SCREEN_H, dm.heightPixels)
                }
                startActivityForResult(intent, REQ_CROP)
            }
            REQ_CROP -> if (resultCode == Activity.RESULT_OK) {
                val newPath = data?.getStringExtra(CropActivity.EXTRA_RESULT_PATH) ?: return
                if (pendingCropIndex >= 0) {
                    galleryRepo.update(pendingCropOldPath, newPath)
                    adapter.updatePath(pendingCropIndex, newPath)
                } else {
                    galleryRepo.add(newPath)
                    adapter.addPath(newPath)
                }
                pendingCropIndex = -1
                updateEmpty()
            }
        }
    }

    private fun copyToInternal(uri: Uri): String? {
        return try {
            val file = File(filesDir, "gal_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { input.copyTo(it) }
            }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    private fun updateEmpty() {
        binding.tvEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }
}
