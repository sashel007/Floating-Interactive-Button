package ru.ikar.floatingbutton_ikar.projector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import android.view.View

class ProjectorView(context: Context) : View(context) {
    private var isCircle = true
    private var circleX: Float = width / 2f // Начальное положение прожектора по X
    private var circleY: Float = height / 2f // Начальное положение прожектора по Y
    private var projectorRadius: Float = 150f // Радиус прожектора

    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f

    init {
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Установка начального положения круга в центре View
        circleX = w / 2f
        circleY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Рисуем полупрозрачный фон
        canvas.drawColor(Color.parseColor("#FE000000"))

        // Рисуем прозрачный круг в месте касания
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        if (isCircle) {
            canvas.drawCircle(circleX, circleY, projectorRadius, paint)
        } else {
            canvas.drawRect(circleX - projectorRadius, circleY - projectorRadius,
                circleX + projectorRadius, circleY + projectorRadius, paint)
        }

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchDistance = Math.sqrt(
            Math.pow((event.x - circleX).toDouble(), 2.0) +
                    Math.pow((event.y - circleY).toDouble(), 2.0)
        )

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Проверяем, находится ли точка касания внутри круга
                if (touchDistance <= projectorRadius) {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    return true // начинаем обработку касаний внутри круга
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Перемещаем круг, если ранее было касание внутри круга
                if (touchDistance <= projectorRadius) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    circleX += dx
                    circleY += dy

                    invalidate()

                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
        }
        return false // не обрабатываем касания вне круга
    }

    fun updateProjectorRadius(newRadius: Float) {
        projectorRadius = newRadius
        invalidate() // Перерисовываем круг с новым радиусом
    }

    fun toggleShape() {
        isCircle = !isCircle // переключаем состояние между кругом и квадратом
        invalidate() // перерисовываем View
    }
}