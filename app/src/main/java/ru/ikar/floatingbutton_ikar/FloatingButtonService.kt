package ru.ikar.floatingbutton_ikar

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class FloatingButtonService() : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButtonLayout: View
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var lastAction: Int? = null


    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingButtonLayout =
            LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null) as FrameLayout

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )

        windowManager.addView(floatingButtonLayout, params)

        floatingButtonLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Initial position
                    initialX = params.x
                    initialY = params.y

                    // Touch point
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    lastAction = event.action
                    true
                }
                MotionEvent.ACTION_UP -> {
                    lastAction = event.action
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Calculate the X and Y coordinates of the view.
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()

                    // Update the layout with new X & Y coordinates
                    windowManager.updateViewLayout(floatingButtonLayout, params)
                    lastAction = event.action
                    true
                }
                else -> {
                    false
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingButtonLayout)
    }
}