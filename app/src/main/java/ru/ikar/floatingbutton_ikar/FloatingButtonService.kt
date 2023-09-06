package ru.ikar.floatingbutton_ikar

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import java.lang.Math.cos
import java.lang.Math.sin

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
    private val radius = 80f
    private var areButtonsVisible = false
    var isExpanded = false // Keeps track of the current state (expanded or collapsed)

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {

        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingButtonLayout =
            LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null) as FrameLayout

        val mainButton = floatingButtonLayout.findViewById<View>(R.id.floating_button)
        val buttons = listOf(
            floatingButtonLayout.findViewById<Button>(R.id.settings_button),
            floatingButtonLayout.findViewById<Button>(R.id.volume_button),
            floatingButtonLayout.findViewById<Button>(R.id.home_button),
            floatingButtonLayout.findViewById<Button>(R.id.brightness_button),
            floatingButtonLayout.findViewById<Button>(R.id.deck_button),
            floatingButtonLayout.findViewById<Button>(R.id.any_button),
        )

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

        floatingButtonLayout.post {
            positionSurroundingButtons(mainButton, buttons, radius)
        }

        windowManager.addView(floatingButtonLayout, params)

        // Assuming buttons is a list of your additional buttons.

        // Inside your OnClickListener for the main button
        floatingButtonLayout.setOnClickListener {   toggleButtonsVisibility(buttons,mainButton)     }


//        floatingButtonLayout.setOnTouchListener { _, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    // Set button to full opacity when pressed
//                    changeOpacity(initialOpacity)
//                    // Initial position
//                    initialX = params.x
//                    initialY = params.y
//
//                    // Touch point
//                    initialTouchX = event.rawX
//                    initialTouchY = event.rawY
//
//                    lastAction = event.action
//                    true
//                }
//                MotionEvent.ACTION_UP -> {
//                    // Start a timer to make the button translucent after 3 seconds
//                    floatingButtonLayout.postDelayed({
//                        changeOpacity(opacity) // Adjust this value as you see fit, 0.5f is 50% translucent
//                    }, opacityDuration)
//                    lastAction = event.action
//                    true
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    // Calculate the X and Y coordinates of the view.
//                    params.x = initialX + (event.rawX - initialTouchX).toInt()
//                    params.y = initialY + (event.rawY - initialTouchY).toInt()
//
//                    // Update the layout with new X & Y coordinates
//                    windowManager.updateViewLayout(floatingButtonLayout, params)
//                    lastAction = event.action
//                    true
//                }
//                else -> {
//                    false
//                }
//            }
//        }
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

    private fun positionSurroundingButtons(mainButton: View, buttons: List<View>, radius: Float) {
        val mainButtonCenterX = mainButton.x + mainButton.width / 2
        val mainButtonCenterY = mainButton.y + mainButton.height / 2

        val angleIncrement = 360.0 / buttons.size

        for (i in buttons.indices) {
            val angle = i * angleIncrement * (Math.PI / 180)  // Convert degrees to radians.
            val x = (radius * kotlin.math.cos(angle) + mainButtonCenterX).toFloat() - buttons[i].width / 2
            val y = (radius * kotlin.math.sin(angle) + mainButtonCenterY).toFloat() - buttons[i].height / 2

            buttons[i].x = x
            buttons[i].y = y
        }
    }


    private fun toggleButtonsVisibility(buttons: List<View>) {
        val newVisibility = if (areButtonsVisible) View.INVISIBLE else View.VISIBLE
        for (button in buttons) {
            button.visibility = newVisibility
        }
        areButtonsVisible = !areButtonsVisible
    }

    fun toggleButtonsVisibility(buttons: List<View>, mainButton: View) {
        if (isExpanded) {
            // Collapse the buttons
            for (button in buttons) {
                button.animate()
                    .x(mainButton.x + mainButton.width / 2 - button.width / 2)
                    .y(mainButton.y + mainButton.height / 2 - button.height / 2)
                    .setDuration(300) // in milliseconds
                    .withEndAction { button.visibility = View.INVISIBLE }
                    .start()
            }
        } else {
            // Expand the buttons
            for ((index, button) in buttons.withIndex()) {
                val (finalX, finalY) = calculateFinalPosition(button, mainButton, index, buttons.size)
                button.x = mainButton.x + mainButton.width / 2 - button.width / 2
                button.y = mainButton.y + mainButton.height / 2 - button.height / 2
                button.visibility = View.VISIBLE
                button.animate()
                    .x(finalX)
                    .y(finalY)
                    .setDuration(300) // in milliseconds
                    .start()
            }
        }

        isExpanded = !isExpanded // Toggle the state
    }

    fun calculateFinalPosition(button: View, mainButton: View, index: Int, totalButtons: Int): Pair<Float, Float> {
        val mainButtonCenterX = mainButton.x + mainButton.width / 2
        val mainButtonCenterY = mainButton.y + mainButton.height / 2

        val angleIncrement = 360.0 / totalButtons
        val angle = index * angleIncrement * (Math.PI / 180)  // Convert degrees to radians.

        val finalX = (radius * kotlin.math.cos(angle) + mainButtonCenterX).toFloat() - button.width / 2
        val finalY = (radius * kotlin.math.sin(angle) + mainButtonCenterY).toFloat() - button.height / 2

        return Pair(finalX, finalY)
    }



}