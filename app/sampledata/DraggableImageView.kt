package ru.ikar.ikar_floatingbuttonfeaturetesting

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent

class DraggableImageView(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs) {
    // Переменные для сохранения последних координат, где было зарегистрировано касание
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f

    // Аннотация, которая подавляет предупреждение о том, что элемент можно нажать
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            // Когда палец касается экрана
            MotionEvent.ACTION_DOWN -> {
                // Сохраняем текущие координаты касания
                lastTouchX = event.x
                lastTouchY = event.y
            }
            // Когда палец двигается по экрану (с зажатым изображением)
            MotionEvent.ACTION_MOVE -> {
                // Вычисляем разницу между текущим положением и последним сохраненным положением касания
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                // Обновляем координаты изображения, перемещая его на вычисленное расстояние
                val x = x + dx
                val y = y + dy

                this.x = x
                this.y = y
            }
        }
        // Возвращаем true, чтобы указать, что событие касания было обработано
        return true
    }
}
