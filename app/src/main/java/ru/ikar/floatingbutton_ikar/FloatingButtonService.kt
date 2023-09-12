package ru.ikar.floatingbutton_ikar

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
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
    private var isExpanded = false // состояние кнопок (кнопки развернуты/свёрнуты)
    private var hasMoved = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mainButton: View
    private lateinit var buttons: List<View>
    private lateinit var volumeSliderLayout: View
    private lateinit var volumeSlider: SeekBar
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    private lateinit var overlayView: View
    private lateinit var floatingButtonView: View
    private val collapseRunnable = Runnable {
        Log.d("CollapseRunnable", "Running collapse")
        if (isExpanded) {
            toggleButtonsVisibility(buttons, mainButton)
        }
        changeOpacity(opacity)  // Делает кнопку полупрозрачной
    }


    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {

        super.onCreate()

        // инициализация WindowManager для кастомных настроек отображения.
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // инициализация AudioManager для управления аудио-настройками.
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // инфлейтим макет кнопки (floating button).
        floatingButtonLayout =
            LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)
                    as FrameLayout
        // Получаем ссылку на главную кнопку внутри макета floating button
        mainButton = floatingButtonLayout.findViewById<View>(R.id.floating_button)
        buttons = listOf(
            floatingButtonLayout.findViewById<ImageButton>(R.id.settings_button),
            floatingButtonLayout.findViewById<ImageButton>(R.id.volume_button),
            floatingButtonLayout.findViewById<ImageButton>(R.id.home_button),
            floatingButtonLayout.findViewById<ImageButton>(R.id.brightness_button),
            floatingButtonLayout.findViewById<ImageButton>(R.id.background_button),
            floatingButtonLayout.findViewById<ImageButton>(R.id.any_button),
        )

        // инфлейтим ползунок громкости, который отображается по нажатию кнопки громкости
        volumeSliderLayout = LayoutInflater.from(this).inflate(R.layout.volume_slider_layout, null)
        // даём ссылку на слайдер внутри макета volume slider.
        volumeSlider = volumeSliderLayout.findViewById<SeekBar>(R.id.volume_slider)

        // Определеяем параметры макета для отображения вьюшки с кнопкой.
        // Эти настройки определяют то, как вьюшка отображается на экране.
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            // Если версия андроида Oreo или выше, используется TYPE_APPLICATION_OVERLAY, иначе TYPE_PHONE
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                //старый легаси параметр для перекрытия вьюшки над остальными элементами телефона
                WindowManager.LayoutParams.TYPE_PHONE
            },
            // вьюшка не потребялет фокус и не мешает тыкать по остальным элементам экрана
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT // задаёт параметр полупрозрачности
        )

        overlayView = LayoutInflater.from(this).inflate(R.layout.fullscreen_overlay, null)
        floatingButtonView = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

//        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        if (overlayView.parent == null) {
//            windowManager.addView(overlayView, overlayParams)
//        }

//        overlayView.setOnTouchListener(object : View.OnTouchListener {
//            private val numberOfTaps = 3
//            private val positions = Array(3) { Pair(0f, 0f) }
//
//            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//                when (event?.actionMasked) {
//                    MotionEvent.ACTION_POINTER_DOWN -> {
//                        if (event.pointerCount == numberOfTaps) {
//                            for (i in 0 until numberOfTaps) {
//                                positions[i] = Pair(event.getX(i), event.getY(i))
//                            }
//
//                            // Calculate the centroid
//                            val centroidX = (positions[0].first + positions[1].first + positions[2].first) / 3
//                            val centroidY = (positions[0].second + positions[1].second + positions[2].second) / 3
//
//                            try {
//                                showFloatingButton(centroidX, centroidY)
//                            } catch (e: Exception) {
//                                Log.e("FloatingButtonError", "Failed to show button: ", e)
//                            }
//
//                        }
//                    }
//                }
//                return true
//            }
//        })

        if (floatingButtonLayout.parent == null) {
            floatingButtonLayout.post {
                positionSurroundingButtons(mainButton, buttons, radius)
            }
            windowManager.addView(floatingButtonLayout, params)
        }

        // устанавливает слушатель изменений на ползунок громкости
        volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // этот метод тригерится каждый раз, когда изменяется значение ползунка.
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // проверяет, сделано ли изменение пользователем (а не программно)
                if (fromUser) {
                    // Обновляет громкость исходя из значений ползунка.
                    // Отображает индикатор громкости на UI-уровне.
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        progress,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
            }

            // Вызывается, когда пользователь начинает трогать ползунок
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            // Вызывается, когда пользователь завершает движение пальцем по ползунку.
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Настройка слушателя для floatingButtonLayout.
        floatingButtonLayout.setOnClickListener {
            // Переключает видимость окружающих кнопок, когда основная кнопка прожата.
            toggleButtonsVisibility(buttons, mainButton)
            Log.d("setOnClickListener", "check setOnClickListener")
        }

        // Настройка onTouchlistener для floatingButtonLayout для обработки касаний.
        floatingButtonLayout.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                // Обработка действий при первом касании пальца (new pointer)
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    // Если это касание одним пальцем то выполняется этот блок:
                    // Установка исходного значения прозрачности для кнопки.
                    changeOpacity(initialOpacity)
                    // Сохраняем исходную позицию кнпоки.
                    initialX = params.x
                    initialY = params.y
                    // Сохраняем исходную позицию пальца
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    // Обновляем флаг определения движения.
                    hasMoved = false
                    // Сохраняем последнее действие при касании.
                    lastAction = event.action
                    Log.d("setOnTouchListener", "check setOnTouchListener")
                    true
//                    if (event.pointerCount == 3) {
//                        Log.d("ThreeFingerTouch", "Detected 3 fingers!")
//                        // расчёт позиции по X и Y всех трёх пальцев.
//                        var sumX = 0f
//                        var sumY = 0f
//                        for (i in 0 until event.pointerCount) {
//                            sumX += event.getX(i)
//                            sumY += event.getY(i)
//                        }
//                        val avgX = sumX / event.pointerCount
//                        val avgY = sumY / event.pointerCount
//                        // Перемещение кнопки на среднюю позицию между тремя пальцами.
//                        positionFloatingButtonAt(avgX, avgY)
//                        true // Потребить touch event
//                    } else {
//                        // Если это касание одним пальцем то выполняется этот блок:
//                        // Установка исходного значения прозрачности для кнопки.
//                        changeOpacity(initialOpacity)
//                        // Сохраняем исходную позицию кнпоки.
//                        initialX = params.x
//                        initialY = params.y
//                        // Сохраняем исходную позицию пальца
//                        initialTouchX = event.rawX
//                        initialTouchY = event.rawY
//                        // Обновляем флаг определения движения.
//                        hasMoved = false
//                        // Сохраняем последнее действие при касании.
//                        lastAction = event.action
//                        Log.d("setOnTouchListener", "check setOnTouchListener")
//                        true
//                    }
                }
                // Блок, где определяется подъём пальца от кнопки.
                MotionEvent.ACTION_UP -> {
                    // Устанавливаем кнопку в состоянии полупрозрачности на 0.2f
                    floatingButtonLayout.postDelayed({
                        changeOpacity(opacity)
                    }, opacityDuration)

                    // Если кнопка не сдвинулась дальше 10 пикселей на любой из осей, то считать это нажатием.
                    if (!hasMoved && Math.abs(initialTouchX - event.rawX) < 10 && Math.abs(
                            initialTouchY - event.rawY
                        ) < 10
                    ) {
                        toggleButtonsVisibility(buttons, mainButton)
                        // Отменяем сворачивание.
                        handler.removeCallbacks(collapseRunnable)
                    }
                    // Сохраняем и возвращаем в лямбду последнее касание.
                    lastAction = event.action
                    Log.d("finger up", "up_")
                    true
                }
                // Блок, фиксирующий движение пальца.
                MotionEvent.ACTION_MOVE -> {
                    // Обновляем координаты  X и Y на основе движения пальцем.
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    // Обновляем позицию кнопки по новым координатам
                    windowManager.updateViewLayout(floatingButtonLayout, params)
                    lastAction = event.action
                    // Определяем, надо ли фиксировать движение (если палец ушёл дальше 10 пикселей)
                    // Если надо, то устанавливаем флаг на hasMoved
                    if (Math.abs(initialTouchX - event.rawX) > 10 || Math.abs(initialTouchY - event.rawY) > 10) {
                        hasMoved = true
                    }
                    Log.d("move across", "across___")
                    true
                }

                else -> {
                    false
                }
            }
        }

        // Итерируем по всем кнопкам, чтобы обработать setOnClickListener.
        buttons.forEachIndexed { index, button ->
            // Вешаем onClicklistener на каждую кнопку.
            button.setOnClickListener {
                // Вызываем функцию handleButtonClickfunction, в индекс кладём номер элемента из списка кнопки.
                handleButtonClick(index)
            }
        }
        // Устанавливаем исходную прозранчость для макета кнопки
        changeOpacity(opacity)
    }

    // Функция изменения прозрачности для floatingButtonLayout.
    private fun changeOpacity(toOpacity: Float) {
        // Используем ObjectAnimator, чтобы санимировать плавную прозрачность
        ObjectAnimator.ofFloat(floatingButtonLayout, "alpha", toOpacity).apply {
            // Устанавливаем длительность анимации.
            duration = 250  // длительность перехода
            // Начать анимацию.
            start()
        }
    }

    /**
     * Позиции маленьких (доп.) кнопок по окружности.
     *
     * @param mainButton Исходная большая кнопка, вокруг которой размещаются доп.кнопки.
     * @param buttons Список кнопок, которые надо расположить вокруг основной.
     * @param radius Расстояние от главной кнопки до доп.кнопок.
     */

    private fun positionSurroundingButtons(mainButton: View, buttons: List<View>, radius: Float) {
        // Рассчитываем центральные координаты главной кнпоки по осям Х/Y.
        val mainButtonCenterX = mainButton.x + mainButton.width / 2
        val mainButtonCenterY = mainButton.y + mainButton.height / 2

        // Рассчитываем приращение угла на основе количества кнопок (size = 6), чтобы расположить их равномерно
        val angleIncrement = 360.0 / buttons.size

        // Итерируем по каждой кнпоке для расчёта новой позиции
        // в макете они изначально на позиции START и TOP.
        for (i in buttons.indices) {
            // Рассчитаем угол для кнопки в радианах.
            val angle = i * angleIncrement * (Math.PI / 180)  // сконвертируем градусы в радианы

            // Рассчитаем координаты X/Y для текущей кнопки по углу и радиусу
            val x =
                (radius * kotlin.math.cos(angle) + mainButtonCenterX).toFloat() - buttons[i].width / 2
            val y =
                (radius * kotlin.math.sin(angle) + mainButtonCenterY).toFloat() - buttons[i].height / 2

            // Установим новую позицию кнопки.
            buttons[i].x = x
            buttons[i].y = y
        }
    }

    /**
     * Переключение видимости окружающих кнопок относительно главной кнопки.
     * Когда кнопки невидимы, эта функция разворачивает их вокруг кнопки.
     * Когда видимы - она сворачивает их обратно.
     *
     * @param buttons Список кнопок.
     * @param mainButton Центральная кнопка, вокгруг которой рендерятся доп.кнопки.
     */
    fun toggleButtonsVisibility(buttons: List<View>, mainButton: View) {
        // Убедимся, отменены ли предыдущие операции со сворачиванием
        handler.removeCallbacks(collapseRunnable)  // Удалим все вызовы функции сворачивания

        if (isExpanded) {
            // Кнопки развёрнуты. Надо свернуть
            for (button in buttons) {
                // Возьмём центр основной кнопки за целевые координаты
                button.visibility = View.INVISIBLE
                button.animate()
                    .x(mainButton.x + mainButton.width / 2 - button.width / 2)
                    .y(mainButton.y + mainButton.height / 2 - button.height / 2)
                    .setDuration(300) // Длительность анимации в миллисек.
                    .withEndAction {
                        // После проигрывания анимации, спрятать кнопки.
                        button.visibility = View.INVISIBLE
                    }
                    .start()
            }
            // Запланируем сворачивание кнопок после задержки в 3000 миллисек.
            handler.postDelayed(collapseRunnable, 3000)
        } else {
            // Кнопки уже свёрнуты, надо их развернуть
            for ((index, button) in buttons.withIndex()) {
                // Рассчитаем последнюю позицию кнопки.
                val (finalX, finalY) = calculateFinalPosition(
                    button,
                    mainButton,
                    index,
                    buttons.size
                )
                // Установим исходную позицию кнопки в центре основной кнопки
                button.x = mainButton.x + mainButton.width / 2 - button.width / 2
                button.y = mainButton.y + mainButton.height / 2 - button.height / 2
                // Сделаем кнопку видимой.
                button.visibility = View.VISIBLE
                // Санимируем кнопку для перехода в финальную позицию
                button.animate()
                    .x(finalX)
                    .y(finalY)
                    .setDuration(300) // Длительность анимации в миллисек.
                    .start()
            }
        }

        // Несмотря на состояние, при котором кнопки развёрнуты, всегда устанавливается команда на сворачивание
        handler.postDelayed(collapseRunnable, 3000)

        // Меняем состояние развёрнутости (если развёрнуто, свернём - и обратно)
        isExpanded = !isExpanded
    }

    /**
     * Рассчитаем последнюю позицию доп.кнопки, когда она развёрнута вокруг основной.
     * Это достигается путём упорядочивания кнопок по окружности (выше был метод).
     *
     * @param button Кнопка, позицию которой надо рассчитать.
     * @param mainButton Основная кнопка.
     * @param index Номер кнопки при итерировании по списку.
     * @param totalButtons Количество кнопок, которые надо равномерно распределить.
     * @return Возвращаем Float-значение, в которое вкладываем X/Y координаты кнопок.
     */

    fun calculateFinalPosition(
        button: View,
        mainButton: View,
        index: Int,
        totalButtons: Int
    ): Pair<Float, Float> {
        val mainButtonCenterX = mainButton.x + mainButton.width / 2
        val mainButtonCenterY = mainButton.y + mainButton.height / 2
        val angleIncrement = 360.0 / totalButtons
        val angle =
            index * angleIncrement * (Math.PI / 180)  // Конвертирует угол из градусов в радианы
        val finalX =
            (radius * kotlin.math.cos(angle) + mainButtonCenterX).toFloat() - button.width / 2
        val finalY =
            (radius * kotlin.math.sin(angle) + mainButtonCenterY).toFloat() - button.height / 2

        return Pair(finalX, finalY)
    }

    /**
     * Обработаем логику доп.кнопок при итерировании по списку кнопок.
     *
     *
     * @param index Возвращаем кнопку по индексу в списке.
     */

    private fun handleButtonClick(index: Int) {
        when (index) {
            0 -> {
                // НАСТРОЙКИ
                settingsButtonHandler()
                Log.d("Button", "Settings clicked")
            }

            1 -> {
                // ГРОМКОСТЬ
                volumeButtonHandler()
            }

            2 -> {
                // ДОМОЙ
                homeButtonHandler()
            }

            3 -> {
                // ЯРКОСТЬ
                Log.d("Button", "Brightness clicked")
            }

            4 -> {
                // ВЫБОР ФОНА
                Log.d("Button", "Background clicked")
            }

            5 -> {
                // ПУСТО
                Log.d("Button", "Any clicked")
            }
        }
    }

    /**
     * вызываем функцию `onDestroy`, когда сервис уничтожается.
     * Проходим по if и проверяем, всё ли уничтожилось.
     */
    override fun onDestroy() {
        super.onDestroy()
        // Если ползунок громкости отображен на экране, то удалим его
        if (volumeSliderLayout.parent != null) {
            windowManager.removeView(volumeSliderLayout)
        }
        // Всегда удаляем кнопку на экране, если сервис прекращает выполнение.
        windowManager.removeView(floatingButtonLayout)
    }

    /**
     * Определяем позицию главной кнопки по X/Y-координатам.
     */
    private fun positionFloatingButtonAt(x: Float, y: Float) {
        // получаем текущие параметры макета кнопки.
        val params = floatingButtonLayout.layoutParams as WindowManager.LayoutParams
        params.x = x.toInt() - floatingButtonLayout.width / 2
        params.y = y.toInt() - floatingButtonLayout.height / 2

        // Применяем обновленные координаты к кнопке.
        windowManager.updateViewLayout(floatingButtonLayout, params)
    }

    /* Функция сброса таймера, который контролирует авто-сворачивание кнопок
        В разработке................
         */
    fun resetCollapseTimer() {
        handler.removeCallbacks(collapseRunnable) // удаляет любые вызовы на сворачивание.
        handler.postDelayed(collapseRunnable, 3000)
    } // Задаем график на сворачивание с 3-х секундной задержкой

    private fun showFloatingButton(x: Float, y: Float) {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Позиция кнопки (х/у-оси) с учетом половины длины/ширины
        params.x = (x - (floatingButtonView.width / 2)).toInt()
        params.y = (y - (floatingButtonView.height / 2)).toInt()

        if (floatingButtonView.parent == null) {
            // если кнопка не прикреплена, добавить
            windowManager.addView(floatingButtonView, params)
        } else {
            // если кнопка прикреплена, обновить макетIf the button is already attached, just update its layout
            windowManager.updateViewLayout(floatingButtonView, params)
        }

//        floatingButtonView.postDelayed({ windowManager.removeView(floatingButtonView) }, 5000)
    }

    private fun moveFloatingButtonTo(x: Float, y: Float) {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = floatingButtonView.layoutParams as WindowManager.LayoutParams

        // Обновить позицию кнопки  (x, y)
        params.x = x.toInt()
        params.y = y.toInt()

        if (floatingButtonView.parent != null) {
            // если кнопка уже добавлена, обновить позицию
            windowManager.updateViewLayout(floatingButtonView, params)
        } else {
            // если кнопка еще не добавлена, добавить на экран
            windowManager.addView(floatingButtonView, params)
        }
    }

    //обработчик кнопки "Громкость"
    private fun volumeButtonHandler() {
        if (volumeSliderLayout.parent == null) { // Условие, если ползунок еще не отображен
            // Определяем параметры макета для ползунка громкости
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

            // Получаем текущую позицию основной кнопки
            val mainButtonX = params.x + mainButton.width
            val mainButtonY = params.y

            // Устанавливаем позицию макета volumeSliderLayout
            volumeParams.x = mainButtonX
            volumeParams.y = mainButtonY

            // Обновляем ползунок согласно системным значениям громкости
            volumeSlider.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            volumeSlider.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            // Добавляем слайдер на экран
            if (volumeSliderLayout.parent == null) {
                windowManager.addView(volumeSliderLayout, volumeParams)
            }
        } else {
            // Если ползунок уже отображен, удаляем с экрана
            windowManager.removeView(volumeSliderLayout)
        }
        Log.d("Button", "Volume clicked")
    }

    //Обработчик кнопки "ДОМОЙ"
    private fun homeButtonHandler() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        Log.d("Button", "Home clicked")
    }

    private fun settingsButtonHandler() {
//        Intent intent = new Intent(Settings.ACTION_SETTINGS);
//        startActivity(intent);
        val settingsIntent = Intent(Settings.ACTION_SETTINGS)
        startActivity(settingsIntent)
    }

}