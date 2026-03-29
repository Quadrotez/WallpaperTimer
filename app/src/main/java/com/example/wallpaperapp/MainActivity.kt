package com.example.wallpaperapp

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wallpaperapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ScheduleAdapter
    private lateinit var repo: ScheduleRepository
    private lateinit var manager: ScheduleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo    = ScheduleRepository(this)
        manager = ScheduleManager(this)

        adapter = ScheduleAdapter(
            mutableListOf(),
            onToggle = { s, enabled ->
                val updated = s.copy(isEnabled = enabled)
                repo.save(updated)
                if (enabled) manager.scheduleNext(updated) else manager.cancel(s.id)
            },
            onEdit   = { s -> openEditor(s.id) },
            onDelete = { s ->
                AlertDialog.Builder(this)
                    .setTitle("Удалить расписание?")
                    .setMessage(s.name)
                    .setPositiveButton("Удалить") { _, _ ->
                        manager.cancel(s.id)
                        repo.delete(s.id)
                        refresh()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            },
            onApplyNow = { s ->
                val path = s.imagePaths.getOrNull(s.currentIndex % s.imagePaths.size.coerceAtLeast(1))
                if (path == null) {
                    Toast.makeText(this, "Нет фото в расписании", Toast.LENGTH_SHORT).show()
                    return@ScheduleAdapter
                }
                lifecycleScope.launch {
                    val result = WallpaperApplier.apply(this@MainActivity, path)
                    result.fold(
                        onSuccess = { Toast.makeText(this@MainActivity, "Обои установлены!", Toast.LENGTH_SHORT).show() },
                        onFailure = { Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show() }
                    )
                }
            }
        )

        binding.rvSchedules.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        binding.fab.setOnClickListener { openEditor(null) }
        binding.btnGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        checkExactAlarmPermission()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val list = repo.getAll()
        adapter.setItems(list)
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditor(scheduleId: String?) {
        val intent = Intent(this, AddScheduleActivity::class.java)
        if (scheduleId != null) intent.putExtra(AddScheduleActivity.EXTRA_SCHEDULE_ID, scheduleId)
        startActivity(intent)
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Точные будильники")
                    .setMessage("Для точного срабатывания по расписанию разрешите точные будильники в настройках.")
                    .setPositiveButton("Открыть настройки") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .setNegativeButton("Позже", null)
                    .show()
            }
        }
    }
}
