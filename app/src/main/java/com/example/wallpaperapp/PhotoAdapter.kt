package com.example.wallpaperapp

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.wallpaperapp.R

class PhotoAdapter(
    private val paths: MutableList<String>,
    private val onDelete: (Int) -> Unit,
    private val onCrop: (Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val image: ImageView = v.findViewById(R.id.imgPhoto)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDeletePhoto)
        val btnCrop: ImageButton = v.findViewById(R.id.btnCropPhoto)
        val badge: android.widget.TextView = v.findViewById(R.id.tvIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false))

    override fun getItemCount() = paths.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val path = paths[position]
        val bmp = BitmapFactory.decodeFile(path)
        holder.image.setImageBitmap(bmp)
        holder.badge.text = (position + 1).toString()
        holder.btnDelete.setOnClickListener { onDelete(holder.adapterPosition) }
        holder.btnCrop.setOnClickListener { onCrop(holder.adapterPosition) }
    }

    fun getPath(index: Int) = paths[index]

    fun updatePath(index: Int, newPath: String) {
        paths[index] = newPath
        notifyItemChanged(index)
    }

    fun addPath(path: String) {
        paths.add(path)
        notifyItemInserted(paths.size - 1)
    }

    fun removePath(index: Int) {
        paths.removeAt(index)
        notifyItemRemoved(index)
        notifyItemRangeChanged(index, paths.size)
    }
}
