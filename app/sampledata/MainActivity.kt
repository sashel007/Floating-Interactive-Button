package ru.ikar.ikar_floatingbuttonfeaturetesting

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    // Координаты для драг-н-дропа кнопки
    private var xDelta: Float = 0f
    private var yDelta: Float = 0f

    // Объявление кнопки как свойства класса
    private lateinit var circleImageView: ImageView

    // Слушатель для обработки касаний экрана
    private val dragListener = View.OnTouchListener { _, event ->
        when (event.pointerCount) {
            1 -> {
                // Проверка, что касание было внутри кнопки
                val isInBounds = event.rawX >= circleImageView.x &&
                        event.rawX <= circleImageView.x + circleImageView.width &&
                        event.rawY >= circleImageView.y &&
                        event.rawY <= circleImageView.y + circleImageView.height

                if (isInBounds) {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            // Запоминание начальных координат
                            xDelta = event.rawX - circleImageView.x
                            yDelta = event.rawY - circleImageView.y
                        }

                        MotionEvent.ACTION_MOVE -> {
                            // Перемещение кнопки
                            circleImageView.x = event.rawX - xDelta
                            circleImageView.y = event.rawY - yDelta
                        }
                    }
                }
            }

            3 -> {
                // Логирование для отладки
                Log.d("ThreeFingerDebug", "Three-finger touch detected")

                // Переменные для вычисления центра между тремя пальцами
                var totalWeightedX = 0f
                var totalWeightedY = 0f
                var totalDistance = 0f

                // Вычисление центра между тремя пальцами
                for (i in 0 until 3) {
                    val nextIndex = (i + 1) % 3
                    val distance = distance(event.getX(i), event.getY(i), event.getX(nextIndex), event.getY(nextIndex))
                    totalWeightedX += distance * event.getX(i)
                    totalWeightedY += distance * event.getY(i)
                    totalDistance += distance
                }

                // Расчет конечной позиции для кнопки
                val x = totalWeightedX / totalDistance
                val y = totalWeightedY / totalDistance

                // Перемещение кнопки в эту позицию
                circleImageView.x = x - circleImageView.width / 2
                circleImageView.y = y - circleImageView.height / 2
            }
        }
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов интерфейса
        val textView = findViewById<TextView>(R.id.activationText)
        val toggleButton = findViewById<SwitchCompat>(R.id.switchButton)
        circleImageView = findViewById(R.id.floatingButtonView)
        val rootLayout = findViewById<View>(R.id.rootLayout)

        // Установка слушателя для переключателя
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            textView.text = if (isChecked) "Активация кнопки: On" else "Активация кнопки: Off"
            updateCircleVisibility(isChecked)
        }

        // Установка слушателя касаний для главного экрана
        rootLayout.setOnTouchListener(dragListener)
    }

    // Функция для обновления видимости кнопки
    private fun updateCircleVisibility(isVisible: Boolean) {
        val circleImageView = findViewById<ImageView>(R.id.floatingButtonView)

        if (isVisible) {
            circleImageView.visibility = View.VISIBLE
        } else {
            circleImageView.visibility = View.GONE
        }
    }

    // Функция для вычисления расстояния между двумя точками
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return kotlin.math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }
}
