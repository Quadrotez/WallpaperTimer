package com.example.wallpaperapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wallpaperapp.databinding.ActivityAddScheduleBinding
import java.io.File
import java.io.FileOutputStream

class AddScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddScheduleBinding
    private lateinit var photoAdapter: PhotoAdapter
    private val photoPaths = mutableListOf<String>()
    private var editingId: String? = null
    private var pendingCropIndex = -1

    companion object {
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        private const val REQ_PICK   = 101
        private const val REQ_CROP   = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingId = intent.getStringExtra(EXTRA_SCHEDULE_ID)
        if (editingId != null) {
            loadSchedule(editingId!!)
        }

        setupPhotos()
        setupDayType()

        binding.btnAddPhoto.setOnClickListener { openGallery() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupPhotos() {
        photoAdapter = PhotoAdapter(
            photoPaths,
            onDelete = { i -> photoAdapter.removePath(i) },
            onCrop   = { i ->
                pendingCropIndex = i
                val path = photoAdapter.getPath(i)
                val intent = Intent(this, CropActivity::class.java).apply {
                    putExtra(CropActivity.EXTRA_URI, Uri.fromFile(File(path)).toString())
                    putExtra("original_path", path)
                }
                startActivityForResult(intent, REQ_CROP)
            }
        )
        binding.rvPhotos.apply {
            layoutManager = LinearLayoutManager(this@AddScheduleActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = photoAdapter
        }
    }

    private fun setupDayType() {
        binding.radioGroup.setOnCheckedChangeListener { _, id ->
            binding.layoutWeeklyDay.visibility  = if (id == R.id.radioWeekly)   View.VISIBLE else View.GONE
            binding.layoutSpecific.visibility   = if (id == R.id.radioSpecific) View.VISIBLE else View.GONE
        }
        // Spinner для еженедельного
        val days = arrayOf("Понедельник","Вторник","Среда","Четверг","Пятница","Суббота","Воскресенье")
        binding.spinnerWeekDay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun loadSchedule(id: String) {
        val s = ScheduleRepository(this).getById(id) ?: return
        binding.etName.setText(s.name)
        binding.timePicker.hour = s.hour
        binding.timePicker.minute = s.minute
        photoPaths.addAll(s.imagePaths)

        when (s.dayType) {
            WallpaperSchedule.DAY_EVERY    -> binding.radioEvery.isChecked = true
            WallpaperSchedule.DAY_WEEKDAYS -> binding.radioWeekdays.isChecked = true
            WallpaperSchedule.DAY_WEEKLY   -> {
                binding.radioWeekly.isChecked = true
                // Calendar: MON=2..SUN=1, spinner: 0=Mon..6=Sun
                val cal = s.specificDays.firstOrNull() ?: 2
                binding.spinnerWeekDay.setSelection(if (cal == 1) 6 else cal - 2)
            }
            WallpaperSchedule.DAY_SPECIFIC -> {
                binding.radioSpecific.isChecked = true
                val map = mapOf(2 to binding.cbMon, 3 to binding.cbTue, 4 to binding.cbWed,
                    5 to binding.cbThu, 6 to binding.cbFri, 7 to binding.cbSat, 1 to binding.cbSun)
                s.specificDays.forEach { map[it]?.isChecked = true }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQ_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_PICK -> if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data ?: return
                val copied = copyToInternal(uri) ?: return
                // Запускаем кроппер
                pendingCropIndex = -1
                val intent = Intent(this, CropActivity::class.java).apply {
                    putExtra(CropActivity.EXTRA_URI, Uri.fromFile(File(copied)).toString())
                    putExtra("original_path", copied)
                }
                startActivityForResult(intent, REQ_CROP)
            }
            REQ_CROP -> if (resultCode == Activity.RESULT_OK) {
                val path = data?.getStringExtra(CropActivity.EXTRA_RESULT_PATH) ?: return
                if (pendingCropIndex >= 0) {
                    photoAdapter.updatePath(pendingCropIndex, path)
                } else {
                    photoAdapter.addPath(path)
                }
                pendingCropIndex = -1
            }
        }
    }

    private fun copyToInternal(uri: Uri): String? {
        return try {
            val file = File(filesDir, "wp_src_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { input.copyTo(it) }
            }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    private fun save() {
        val name = binding.etName.text.toString().trim().ifEmpty { "Расписание" }
        if (photoPaths.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы одно фото", Toast.LENGTH_SHORT).show()
            return
        }

        val (dayType, specificDays) = buildDayType()
        val schedule = WallpaperSchedule(
            id           = editingId ?: java.util.UUID.randomUUID().toString(),
            name         = name,
            imagePaths   = photoAdapter.let { (0 until it.itemCount).map { i -> it.getPath(i) } },
            hour         = binding.timePicker.hour,
            minute       = binding.timePicker.minute,
            dayType      = dayType,
            specificDays = specificDays,
            isEnabled    = true
        )

        val repo = ScheduleRepository(this)
        repo.save(schedule)
        ScheduleManager(this).scheduleNext(schedule)
        Toast.makeText(this, "Расписание сохранено", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun buildDayType(): Pair<String, List<Int>> {
        return when (binding.radioGroup.checkedRadioButtonId) {
            R.id.radioEvery    -> Pair(WallpaperSchedule.DAY_EVERY, emptyList())
            R.id.radioWeekdays -> Pair(WallpaperSchedule.DAY_WEEKDAYS, emptyList())
            R.id.radioWeekly   -> {
                // Spinner pos 0=Mon(2)..5=Sat(7), 6=Sun(1)
                val pos = binding.spinnerWeekDay.selectedItemPosition
                val cal = if (pos == 6) 1 else pos + 2
                Pair(WallpaperSchedule.DAY_WEEKLY, listOf(cal))
            }
            R.id.radioSpecific -> {
                val days = mutableListOf<Int>()
                if (binding.cbMon.isChecked) days.add(2)
                if (binding.cbTue.isChecked) days.add(3)
                if (binding.cbWed.isChecked) days.add(4)
                if (binding.cbThu.isChecked) days.add(5)
                if (binding.cbFri.isChecked) days.add(6)
                if (binding.cbSat.isChecked) days.add(7)
                if (binding.cbSun.isChecked) days.add(1)
                Pair(WallpaperSchedule.DAY_SPECIFIC, days)
            }
            else -> Pair(WallpaperSchedule.DAY_EVERY, emptyList())
        }
    }
}
