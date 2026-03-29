package com.example.wallpaperapp

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.wallpaperapp.R

class ScheduleAdapter(
    private val items: MutableList<WallpaperSchedule>,
    private val onToggle: (WallpaperSchedule, Boolean) -> Unit,
    private val onEdit: (WallpaperSchedule) -> Unit,
    private val onDelete: (WallpaperSchedule) -> Unit,
    private val onApplyNow: (WallpaperSchedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView       = v.findViewById(R.id.tvName)
        val tvTime: TextView       = v.findViewById(R.id.tvTime)
        val tvDays: TextView       = v.findViewById(R.id.tvDays)
        val tvCount: TextView      = v.findViewById(R.id.tvPhotoCount)
        val preview: ImageView     = v.findViewById(R.id.imgPreview)
        val toggle: Switch         = v.findViewById(R.id.swEnabled)
        val btnEdit: ImageButton   = v.findViewById(R.id.btnEdit)
        val btnDel: ImageButton    = v.findViewById(R.id.btnDelete)
        val btnApply: com.google.android.material.button.MaterialButton = v.findViewById(R.id.btnApplyNow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.tvName.text  = s.name
        holder.tvTime.text  = s.timeString()
        holder.tvDays.text  = s.dayDescription()
        holder.tvCount.text = "${s.imagePaths.size} фото"

        holder.toggle.setOnCheckedChangeListener(null)
        holder.toggle.isChecked = s.isEnabled
        holder.toggle.setOnCheckedChangeListener { _, checked -> onToggle(s, checked) }

        holder.btnEdit.setOnClickListener   { onEdit(s) }
        holder.btnDel.setOnClickListener    { onDelete(s) }
        holder.btnApply.setOnClickListener  { onApplyNow(s) }

        holder.btnApply.isEnabled = s.imagePaths.isNotEmpty()

        if (s.imagePaths.isNotEmpty()) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bmp = BitmapFactory.decodeFile(s.imagePaths[0], opts)
            holder.preview.setImageBitmap(bmp)
        } else {
            holder.preview.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    fun setItems(newItems: List<WallpaperSchedule>) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged()
    }
}
