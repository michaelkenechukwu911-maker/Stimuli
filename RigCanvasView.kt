package com.example.riggingtool

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

data class BoneNode(
    val id: Int,
    var x: Float,
    var y: Float,
    var parentId: Int? = null
)

class RigCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val nodes = mutableListOf<BoneNode>()
    private var selectedNode: BoneNode? = null
    var isPassThroughMode = false
    var isAddMode = true // true = Add Bone, false = Move Bone

    private val nodePaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val selectedNodePaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val bonePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isPassThroughMode) return

        // 1. Draw Bones (Lines connecting parent to child)
        for (node in nodes) {
            node.parentId?.let { parentId ->
                val parent = nodes.find { it.id == parentId }
                if (parent != null) {
                    canvas.drawLine(parent.x, parent.y, node.x, node.y, bonePaint)
                }
            }
        }

        // 2. Draw Joints/Nodes (Circles)
        for (node in nodes) {
            val paint = if (node == selectedNode) selectedNodePaint else nodePaint
            canvas.drawCircle(node.x, node.y, 28f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isPassThroughMode) return false

        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchedNode = findTouchedNode(touchX, touchY)
                if (touchedNode != null) {
                    selectedNode = touchedNode
                } else if (isAddMode) {
                    // Add new joint, connected to previously selected node
                    val newId = (nodes.maxOfOrNull { it.id } ?: 0) + 1
                    val newNode = BoneNode(newId, touchX, touchY, selectedNode?.id)
                    nodes.add(newNode)
                    selectedNode = newNode
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                selectedNode?.let { node ->
                    node.x = touchX
                    node.y = touchY
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                // Keep selected node active for attaching next child bone
            }
        }
        return true
    }

    private fun findTouchedNode(x: Float, y: Float): BoneNode? {
        return nodes.find { hypot(it.x - x, it.y - y) <= 60f }
    }

    fun clearRig() {
        nodes.clear()
        selectedNode = null
        invalidate()
    }
}
