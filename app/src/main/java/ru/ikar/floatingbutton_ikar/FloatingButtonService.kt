package ru.ikar.floatingbutton_ikar

import android.animation.ObjectAnimator
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
    private val initialOpacity = 1.0f
    private val opacity = 0.2f
    private val opacityDuration = 2500L


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
                    // Set button to full opacity when pressed
                    changeOpacity(initialOpacity)
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
                    // Start a timer to make the button translucent after 3 seconds
                    floatingButtonLayout.postDelayed({
                        changeOpacity(opacity) // Adjust this value as you see fit, 0.5f is 50% translucent
                    }, opacityDuration)
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
        changeOpacity(opacity)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingButtonLayout)
    }

    private fun changeOpacity(toOpacity: Float) {
        ObjectAnimator.ofFloat(floatingButtonLayout, "alpha", toOpacity).apply {
            duration = 250  // duration of the transition, you can adjust this
            start()
        }
    }
}