package com.example.wallpaperapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class CropImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private var imageRect = RectF()
    var cropRect = RectF()
        private set

    var fixedAspectRatio: Float? = null
        set(value) {
            field = value
            if (value != null) applyAspectRatio()
            invalidate()
        }

    private val overlayPaint = Paint().apply { color = Color.argb(140, 0, 0, 0) }
    private val borderPaint = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2.5f
    }
    private val cornerPaint = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 5f; strokeCap = Paint.Cap.ROUND
    }
    private val gridPaint = Paint().apply {
        color = Color.argb(70, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 1f
    }

    private val HANDLE = 50f
    private var activeHandle = Handle.NONE
    private var lastX = 0f
    private var lastY = 0f

    private enum class Handle { NONE, MOVE, TL, TR, BL, BR }

    fun setImage(bmp: Bitmap) {
        bitmap = bmp
        requestLayout()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmap?.let { bmp ->
            val br = bmp.width.toFloat() / bmp.height
            val vr = w.toFloat() / h
            imageRect = if (br > vr) {
                val ih = w / br
                RectF(0f, (h - ih) / 2, w.toFloat(), (h + ih) / 2)
            } else {
                val iw = h * br
                RectF((w - iw) / 2, 0f, (w + iw) / 2, h.toFloat())
            }
            val px = imageRect.width() * 0.1f
            val py = imageRect.height() * 0.1f
            cropRect = RectF(imageRect.left + px, imageRect.top + py,
                imageRect.right - px, imageRect.bottom - py)
            applyAspectRatio()
        }
    }

    private fun applyAspectRatio() {
        val ratio = fixedAspectRatio ?: return
        if (imageRect.isEmpty) return
        val maxW = imageRect.width() * 0.9f
        val maxH = imageRect.height() * 0.9f
        val cw: Float
        val ch: Float
        if (maxW / ratio <= maxH) {
            cw = maxW; ch = maxW / ratio
        } else {
            ch = maxH; cw = maxH * ratio
        }
        val cx = imageRect.centerX()
        val cy = imageRect.centerY()
        cropRect = RectF(cx - cw / 2, cy - ch / 2, cx + cw / 2, cy + ch / 2)
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = bitmap ?: return
        canvas.drawBitmap(bmp, null, imageRect, null)

        canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, cropRect.top, overlayPaint)
        canvas.drawRect(imageRect.left, cropRect.bottom, imageRect.right, imageRect.bottom, overlayPaint)
        canvas.drawRect(imageRect.left, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
        canvas.drawRect(cropRect.right, cropRect.top, imageRect.right, cropRect.bottom, overlayPaint)

        val tw = cropRect.width() / 3
        val th = cropRect.height() / 3
        for (i in 1..2) {
            canvas.drawLine(cropRect.left + tw * i, cropRect.top, cropRect.left + tw * i, cropRect.bottom, gridPaint)
            canvas.drawLine(cropRect.left, cropRect.top + th * i, cropRect.right, cropRect.top + th * i, gridPaint)
        }

        canvas.drawRect(cropRect, borderPaint)

        val c = 28f
        val corners = listOf(
            cropRect.left to cropRect.top,
            cropRect.right to cropRect.top,
            cropRect.left to cropRect.bottom,
            cropRect.right to cropRect.bottom
        )
        corners.forEachIndexed { i, (x, y) ->
            val sx = if (i % 2 == 0) 1f else -1f
            val sy = if (i < 2) 1f else -1f
            canvas.drawLine(x, y, x + sx * c, y, cornerPaint)
            canvas.drawLine(x, y, x, y + sy * c, cornerPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeHandle = hitTest(event.x, event.y)
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                applyDrag(dx, dy)
                lastX = event.x
                lastY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> activeHandle = Handle.NONE
        }
        return true
    }

    private fun hitTest(x: Float, y: Float): Handle {
        fun near(px: Float, py: Float) = Math.abs(x - px) < HANDLE && Math.abs(y - py) < HANDLE
        return when {
            near(cropRect.left,  cropRect.top)    -> Handle.TL
            near(cropRect.right, cropRect.top)    -> Handle.TR
            near(cropRect.left,  cropRect.bottom) -> Handle.BL
            near(cropRect.right, cropRect.bottom) -> Handle.BR
            cropRect.contains(x, y)               -> Handle.MOVE
            else                                  -> Handle.NONE
        }
    }

    private fun applyDrag(dx: Float, dy: Float) {
        val minSize = 80f
        val ratio = fixedAspectRatio

        when (activeHandle) {
            Handle.MOVE -> {
                val nl = max(imageRect.left, min(cropRect.left + dx, imageRect.right - cropRect.width()))
                val nt = max(imageRect.top,  min(cropRect.top  + dy, imageRect.bottom - cropRect.height()))
                cropRect.offsetTo(nl, nt)
            }
            Handle.TL -> {
                if (ratio != null) {
                    val newW = max(minSize, cropRect.right - (cropRect.left + dx))
                    val newH = newW / ratio
                    cropRect.left = cropRect.right - newW
                    cropRect.top  = cropRect.bottom - newH
                } else {
                    cropRect.left = max(imageRect.left, min(cropRect.left + dx, cropRect.right - minSize))
                    cropRect.top  = max(imageRect.top,  min(cropRect.top  + dy, cropRect.bottom - minSize))
                }
            }
            Handle.TR -> {
                if (ratio != null) {
                    val newW = max(minSize, cropRect.right + dx - cropRect.left)
                    val newH = newW / ratio
                    cropRect.right = cropRect.left + newW
                    cropRect.top   = cropRect.bottom - newH
                } else {
                    cropRect.right = min(imageRect.right, max(cropRect.right + dx, cropRect.left + minSize))
                    cropRect.top   = max(imageRect.top,   min(cropRect.top   + dy, cropRect.bottom - minSize))
                }
            }
            Handle.BL -> {
                if (ratio != null) {
                    val newW = max(minSize, cropRect.right - (cropRect.left + dx))
                    val newH = newW / ratio
                    cropRect.left   = cropRect.right - newW
                    cropRect.bottom = cropRect.top + newH
                } else {
                    cropRect.left   = max(imageRect.left,   min(cropRect.left   + dx, cropRect.right - minSize))
                    cropRect.bottom = min(imageRect.bottom, max(cropRect.bottom + dy, cropRect.top   + minSize))
                }
            }
            Handle.BR -> {
                if (ratio != null) {
                    val newW = max(minSize, cropRect.right + dx - cropRect.left)
                    val newH = newW / ratio
                    cropRect.right  = cropRect.left + newW
                    cropRect.bottom = cropRect.top  + newH
                } else {
                    cropRect.right  = min(imageRect.right,  max(cropRect.right  + dx, cropRect.left + minSize))
                    cropRect.bottom = min(imageRect.bottom, max(cropRect.bottom + dy, cropRect.top  + minSize))
                }
            }
            else -> {}
        }

        if (cropRect.left   < imageRect.left)   cropRect.offsetTo(imageRect.left, cropRect.top)
        if (cropRect.top    < imageRect.top)    cropRect.offsetTo(cropRect.left, imageRect.top)
        if (cropRect.right  > imageRect.right)  cropRect.right  = imageRect.right
        if (cropRect.bottom > imageRect.bottom) cropRect.bottom = imageRect.bottom
    }

    fun getCroppedBitmap(): Bitmap? {
        val bmp = bitmap ?: return null
        val sx = bmp.width  / imageRect.width()
        val sy = bmp.height / imageRect.height()
        val l = ((cropRect.left   - imageRect.left) * sx).toInt().coerceIn(0, bmp.width  - 1)
        val t = ((cropRect.top    - imageRect.top)  * sy).toInt().coerceIn(0, bmp.height - 1)
        val w = (cropRect.width()  * sx).toInt().coerceIn(1, bmp.width  - l)
        val h = (cropRect.height() * sy).toInt().coerceIn(1, bmp.height - t)
        return Bitmap.createBitmap(bmp, l, t, w, h)
    }
}
