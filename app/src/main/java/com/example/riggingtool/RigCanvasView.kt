package com.example.riggingtool

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class RigCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
