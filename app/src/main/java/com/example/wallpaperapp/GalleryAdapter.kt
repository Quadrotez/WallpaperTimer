package com.example.wallpaperapp

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class GalleryAdapter(
    private val paths: MutableList<String>,
    private val onCrop: (Int) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onPick: ((Int) -> Unit)? = null  // null = не режим выбора
) : RecyclerView.Adapter<GalleryAdapter.VH>() {

    var pickMode = onPick != null
    val selectedPaths = mutableSetOf<String>()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val image: ImageView = v.findViewById(R.id.imgGalleryPhoto)
        val btnCrop: ImageButton = v.findViewById(R.id.btnGalleryCrop)
        val btnDelete: ImageButton = v.findViewById(R.id.btnGalleryDelete)
        val overlay: FrameLayout = v.findViewById(R.id.selectOverlay)
        val checkmark: ImageView = v.findViewById(R.id.ivCheckmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_photo, parent, false))

    override fun getItemCount() = paths.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val path = paths[position]
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
        holder.image.setImageBitmap(BitmapFactory.decodeFile(path, opts))

        if (pickMode) {
            holder.btnCrop.visibility = View.GONE
            holder.btnDelete.visibility = View.GONE
            val selected = selectedPaths.contains(path)
            holder.overlay.visibility = if (selected) View.VISIBLE else View.GONE
            holder.checkmark.visibility = if (selected) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                if (selectedPaths.contains(path)) {
                    selectedPaths.remove(path)
                    holder.overlay.visibility = View.GONE
                    holder.checkmark.visibility = View.GONE
                } else {
                    selectedPaths.add(path)
                    holder.overlay.visibility = View.VISIBLE
                    holder.checkmark.visibility = View.VISIBLE
                }
            }
        } else {
            holder.btnCrop.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.VISIBLE
            holder.overlay.visibility = View.GONE
            holder.checkmark.visibility = View.GONE
            holder.btnCrop.setOnClickListener { onCrop(holder.adapterPosition) }
            holder.btnDelete.setOnClickListener { onDelete(holder.adapterPosition) }
            holder.itemView.setOnClickListener(null)
        }
    }

    fun getPath(index: Int) = paths[index]

    fun addPath(path: String) {
        paths.add(path); notifyItemInserted(paths.size - 1)
    }

    fun updatePath(index: Int, newPath: String) {
        paths[index] = newPath; notifyItemChanged(index)
    }

    fun removePath(index: Int) {
        paths.removeAt(index)
        notifyItemRemoved(index)
        notifyItemRangeChanged(index, paths.size)
    }
}
