package com.example.wallpaperapp

import java.util.UUID

data class WallpaperSchedule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Расписание",
    val imagePaths: List<String> = emptyList(),
    val hour: Int = 8,
    val minute: Int = 0,
    val dayType: String = DAY_EVERY,
    val specificDays: List<Int> = emptyList(),
    val isEnabled: Boolean = true,
    val currentIndex: Int = 0
) {
    companion object {
        const val DAY_EVERY    = "EVERY_DAY"
        const val DAY_WEEKDAYS = "WEEKDAYS"
        const val DAY_WEEKLY   = "WEEKLY"
        const val DAY_SPECIFIC = "SPECIFIC_DAYS"
    }

    fun dayDescription(): String = when (dayType) {
        DAY_EVERY    -> "Каждый день"
        DAY_WEEKDAYS -> "По будням"
        DAY_WEEKLY   -> {
            val names = listOf("Вс","Пн","Вт","Ср","Чт","Пт","Сб")
            val day = specificDays.firstOrNull() ?: 2
            "Еженедельно (${names.getOrElse(day % 7) { "?" }})"
        }
        DAY_SPECIFIC -> {
            val names = mapOf(
                2 to "Пн", 3 to "Вт", 4 to "Ср", 5 to "Чт",
                6 to "Пт", 7 to "Сб", 1 to "Вс"
            )
            specificDays.sortedBy { it }.mapNotNull { names[it] }.joinToString(", ")
        }
        else -> ""
    }

    fun timeString(): String = String.format("%02d:%02d", hour, minute)
}
