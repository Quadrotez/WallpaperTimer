package com.example.wallpaperapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wallpaperapp.databinding.ActivityAddScheduleBinding

class AddScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddScheduleBinding
    private lateinit var photoAdapter: PhotoAdapter
    private val photoPaths = mutableListOf<String>()
    private var editingId: String? = null

    companion object {
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        private const val REQ_GALLERY = 103
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingId = intent.getStringExtra(EXTRA_SCHEDULE_ID)
        if (editingId != null) loadSchedule(editingId!!)

        setupPhotos()
        setupDayType()

        binding.btnAddPhoto.setOnClickListener { openGalleryPicker() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupPhotos() {
        photoAdapter = PhotoAdapter(
            photoPaths,
            onDelete = { i -> photoAdapter.removePath(i) },
            onCrop   = { i ->
                // Кроп прямо из редактора расписания — открываем через GalleryActivity не нужно,
                // просто убираем из списка и просим выбрать заново
                Toast.makeText(this, "Удалите фото и добавьте снова из Галереи для повторной обрезки", Toast.LENGTH_LONG).show()
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
        val days = arrayOf("Понедельник","Вторник","Среда","Четверг","Пятница","Суббота","Воскресенье")
        binding.spinnerWeekDay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun loadSchedule(id: String) {
        val s = ScheduleRepository(this).getById(id) ?: return
        binding.etName.setText(s.name)
        binding.timePicker.hour   = s.hour
        binding.timePicker.minute = s.minute
        photoPaths.addAll(s.imagePaths)

        when (s.dayType) {
            WallpaperSchedule.DAY_EVERY    -> binding.radioEvery.isChecked = true
            WallpaperSchedule.DAY_WEEKDAYS -> binding.radioWeekdays.isChecked = true
            WallpaperSchedule.DAY_WEEKLY   -> {
                binding.radioWeekly.isChecked = true
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

    private fun openGalleryPicker() {
        val intent = Intent(this, GalleryActivity::class.java).apply {
            putExtra(GalleryActivity.EXTRA_PICK_MODE, true)
        }
        startActivityForResult(intent, REQ_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_GALLERY && resultCode == Activity.RESULT_OK) {
            val selected = data?.getStringArrayListExtra(GalleryActivity.EXTRA_SELECTED_PATHS) ?: return
            selected.forEach { path ->
                if (!photoPaths.contains(path)) {
                    photoAdapter.addPath(path)
                }
            }
        }
    }

    private fun save() {
        val name = binding.etName.text.toString().trim().ifEmpty { "Расписание" }
        if (photoPaths.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы одно фото из галереи", Toast.LENGTH_SHORT).show()
            return
        }

        val (dayType, specificDays) = buildDayType()
        val schedule = WallpaperSchedule(
            id           = editingId ?: java.util.UUID.randomUUID().toString(),
            name         = name,
            imagePaths   = (0 until photoAdapter.itemCount).map { photoAdapter.getPath(it) },
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
