package ru.ikar.floatingbutton_ikar

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar

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
    private var isExpanded = false // Keeps track of the current state (expanded or collapsed)
    private var hasMoved = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mainButton: View
    private lateinit var buttons: List<View>
    private lateinit var volumeSliderLayout: View
    private lateinit var volumeSlider: SeekBar
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    val collapseRunnable = Runnable {
        Log.d("CollapseRunnable", "Running collapse")
        if (isExpanded) {
            toggleButtonsVisibility(buttons, mainButton)
        }
        changeOpacity(opacity)  // Make the button translucent
    }


    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {

        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        floatingButtonLayout =
            LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)
                    as FrameLayout

        mainButton = floatingButtonLayout.findViewById<View>(R.id.floating_button)
        buttons = listOf(
            floatingButtonLayout.findViewById<ImageButton>(R.id.settings_button),
            floatingButtonLayout.findViewById<ImageButton>(R.id.volume_button),
            floatingButtonLayout.findViewById<ImageButton>(R.id.home_button),
            floatingButtonLayout.findViewById<ImageButton>(R.id.brightness_button),
            floatingButtonLayout.findViewById<ImageButton>(R.id.background_button),
            floatingButtonLayout.findViewById<ImageButton>(R.id.any_button),
        )

        volumeSliderLayout = LayoutInflater.from(this).inflate(R.layout.volume_slider_layout, null)
        volumeSlider = volumeSliderLayout.findViewById<SeekBar>(R.id.volume_slider)

        params = WindowManager.LayoutParams(
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

        fun resetCollapseTimer() {
            handler.removeCallbacks(collapseRunnable)
            handler.postDelayed(collapseRunnable, 3000) // 3 seconds delay
        }

        // Assuming buttons is a list of your additional buttons.

        volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        progress,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        // Inside your OnClickListener for the main button
        floatingButtonLayout.setOnClickListener {   toggleButtonsVisibility(buttons,mainButton)     }

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
                    hasMoved = false // Reset the movement flag
                    lastAction = event.action
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Start a timer to make the button translucent after a certain duration
                    floatingButtonLayout.postDelayed({
                        changeOpacity(opacity)
                    }, opacityDuration)

                    // If the button hasn't moved significantly, consider it as a click
                    if (!hasMoved && Math.abs(initialTouchX - event.rawX) < 10 && Math.abs(initialTouchY - event.rawY) < 10) {
                        toggleButtonsVisibility(buttons, mainButton)
                        handler.removeCallbacks(collapseRunnable)
                    }
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
                    // If the movement is significant, mark it as moved
                    if (Math.abs(initialTouchX - event.rawX) > 10 || Math.abs(initialTouchY - event.rawY) > 10) {
                        hasMoved = true
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                handleButtonClick(index)
            }
        }



        changeOpacity(opacity)
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

    fun toggleButtonsVisibility(buttons: List<View>, mainButton: View) {
        handler.removeCallbacks(collapseRunnable)  // Remove any pending collapse calls

        if (isExpanded) {
            // Collapse the buttons
            for (button in buttons) {
                button.visibility = View.INVISIBLE
                button.animate()
                    .x(mainButton.x + mainButton.width / 2 - button.width / 2)
                    .y(mainButton.y + mainButton.height / 2 - button.height / 2)
                    .setDuration(300) // in milliseconds
                    .withEndAction { button.visibility = View.INVISIBLE }
                    .start()
            }
            handler.postDelayed(collapseRunnable, 3000)
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

        handler.postDelayed(collapseRunnable, 3000)  // Always schedule the collapse after a toggle, regardless of the current state

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

    private fun handleButtonClick(index: Int) {
        when (index) {
            0 -> {
                // Handle settings button click
                Log.d("Button", "Settings clicked")
            }
            1 -> {
                if (volumeSliderLayout.parent == null) { // If the SeekBar isn't already displayed
                    // Define the layout parameters as per your needs
                    val volumeParams = WindowManager.LayoutParams(
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

                    // Get the current position of the main button
                    val mainButtonX = params.x + mainButton.width
                    val mainButtonY = params.y

                    // Set the position of the volumeSliderLayout
                    volumeParams.x = mainButtonX
                    volumeParams.y = mainButtonY

                    volumeSlider.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    volumeSlider.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

                    windowManager.addView(volumeSliderLayout, volumeParams)
                } else {
                    windowManager.removeView(volumeSliderLayout)
                }
                Log.d("Button", "Volume clicked")
            }
            2 -> {
                // Handle home button click
                Log.d("Button", "Home clicked")
            }
            3 -> {
                // Handle brightness button click
                Log.d("Button", "Brightness clicked")
            }
            4 -> {
                // Handle background button click
                Log.d("Button", "Background clicked")
            }
            5 -> {
                // Handle any button click
                Log.d("Button", "Any clicked")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (volumeSliderLayout.parent != null) {
            windowManager.removeView(volumeSliderLayout)
        }
        windowManager.removeView(floatingButtonLayout)
    }

}