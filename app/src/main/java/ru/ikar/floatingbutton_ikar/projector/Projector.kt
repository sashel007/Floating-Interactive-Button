package ru.ikar.floatingbutton_ikar.projector

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.SeekBar
import ru.ikar.floatingbutton_ikar.R

class Projector(private val context: Context) {
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var projectorView: ProjectorView? = ProjectorView(context)
    private var seekBarView: View? = LayoutInflater.from(context).inflate(R.layout.projector_seekbar_layout, null)
    private var isViewAdded = false

//    init {
//        // Настройка и добавление ProjectorView и SeekBar в WindowManager
//        setupProjectorView()
//        setupSeekBarView()
//
//        // Настройка кнопок
//        setupButtons()
//    }

    private fun setupProjectorView() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(projectorView, layoutParams)
    }

    private fun setupSeekBarView() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.BOTTOM
        windowManager.addView(seekBarView, layoutParams)

        val seekBar = seekBarView!!.findViewById<SeekBar>(R.id.projector_seekbar)
        // Настройка SeekBar
        seekBar.max = 300
        seekBar.progress = 150

        // Обработчик изменений SeekBar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                projectorView?.updateProjectorRadius(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun setupButtons() {
        val changeShapeButton = seekBarView!!.findViewById<ImageButton>(R.id.change_shape)
        val closeButton = seekBarView!!.findViewById<ImageButton>(R.id.close_service)

        closeButton.setOnClickListener {
            // Обработка нажатия на кнопку закрытия
            dismiss()
        }

        changeShapeButton.setOnClickListener {
            projectorView?.toggleShape()
        }
    }

    fun dismiss() {
        if (isViewAdded) {
            projectorView?.let { windowManager.removeView(it) }
            seekBarView?.let { windowManager.removeView(it) }
            isViewAdded = false
        }
    }

    fun show() {
        if (!isViewAdded) {
            // Инициализация и добавление ProjectorView
            projectorView = ProjectorView(context)
            setupProjectorView()

            // Инициализация и добавление SeekBar
            seekBarView = LayoutInflater.from(context).inflate(R.layout.projector_seekbar_layout, null)
            setupSeekBarView()

            // Настройка кнопок
            setupButtons()

            isViewAdded = true
        }
    }
}