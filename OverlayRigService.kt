package com.example.riggingtool

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat

class OverlayRigService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var canvasView: RigCanvasView
    private lateinit var toolbarView: View

    private lateinit var canvasParams: WindowManager.LayoutParams
    private lateinit var toolbarParams: WindowManager.LayoutParams

    private var isPassThrough = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        setupCanvasOverlay()
        setupToolbarOverlay()
    }

    private fun setupCanvasOverlay() {
        canvasView = RigCanvasView(this)

        canvasParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(canvasView, canvasParams)
    }

    private fun setupToolbarOverlay() {
        // Build floating dock UI programmatically
        toolbarView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xAA000000.toInt()) // Semi-transparent black background
            setPadding(16, 16, 16, 16)

            val btnAdd = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_input_add)
                setOnClickListener {
                    canvasView.isAddMode = true
                }
            }

            val btnPassThrough = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_compass)
                setOnClickListener {
                    togglePassThroughMode()
                }
            }

            val btnClear = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                setOnClickListener {
                    canvasView.clearRig()
                }
            }

            addView(btnAdd)
            addView(btnPassThrough)
            addView(btnClear)
        }

        toolbarParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // Enable dragging the toolbar around screen
        toolbarView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = toolbarParams.x
                        initialY = toolbarParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        toolbarParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        toolbarParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(toolbarView, toolbarParams)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(toolbarView, toolbarParams)
    }

    private fun togglePassThroughMode() {
        isPassThrough = !isPassThrough
        canvasView.isPassThroughMode = isPassThrough

        if (isPassThrough) {
            // Touches pass straight through to CapCut or underlying apps
            canvasParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            // Touches captured by overlay canvas
            canvasParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }

        windowManager.updateViewLayout(canvasView, canvasParams)
        canvasView.invalidate()
    }

    private fun startForegroundService() {
        val channelId = "rig_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rigging Tool Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Active Rigging Service")
            .setContentText("Overlay active over other apps")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::canvasView.isInitialized) windowManager.removeView(canvasView)
        if (::toolbarView.isInitialized) windowManager.removeView(toolbarView)
    }
}
