package ru.ikar.floatingbutton_ikar

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs

class FloatingButtonService : Service() {

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
    private lateinit var pm: PackageManager
    private lateinit var buttons: MutableList<View>
    private lateinit var windowManager: WindowManager
    private lateinit var mainButton: View
    private lateinit var volumeSliderLayout: View
    private lateinit var volumeSlider: SeekBar
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    private lateinit var floatingButtonLayout: View
    private lateinit var floatingButtonView: View
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var packageNames: List<String>
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

        pm = this.packageManager

        // инициализация WindowManager для кастомных настроек отображения.
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // инициализация AudioManager для управления аудио-настройками.
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sharedPreferences = getSharedPreferences("app_package_name", Context.MODE_PRIVATE)
        packageNames = getPackageNamesFromSharedPreferences()

        // инфлейтим макет кнопки (floating button).
        floatingButtonLayout =
            LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null) as FrameLayout
        // Получаем ссылку на главную кнопку внутри макета floating button
        mainButton = floatingButtonLayout.findViewById(R.id.floating_button)

        buttons = mutableListOf(
            floatingButtonLayout.findViewById(R.id.settings_button),
            floatingButtonLayout.findViewById(R.id.volume_button),
            floatingButtonLayout.findViewById(R.id.home_button),
            floatingButtonLayout.findViewById(R.id.brightness_button),
            floatingButtonLayout.findViewById(R.id.background_button),
        )

        for (packageName in packageNames) {
            try {
                val appIcon = pm.getApplicationIcon(packageName)
                val newButton = ImageButton(this)
                newButton.setImageDrawable(appIcon)

                newButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE) // Добавьте эту строку

                // Преобразование dp в пиксели
                val pixelsWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
                val pixelsHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()

                // Установка размеров для кнопки
                val layoutParams = FrameLayout.LayoutParams(pixelsWidth, pixelsHeight)
                newButton.layoutParams = layoutParams

                // Добавление кнопки в layout и список
                (floatingButtonLayout as FrameLayout).addView(newButton)
                buttons.add(newButton)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        Log.d("DEBUG", "selectedButtons: $buttons")
        // инфлейтим ползунок громкости, который отображается по нажатию кнопки громкости
        volumeSliderLayout = LayoutInflater.from(this).inflate(R.layout.volume_slider_layout, null)
        // даём ссылку на слайдер внутри макета volume slider.
        volumeSlider = volumeSliderLayout.findViewById(R.id.volume_slider)
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

        floatingButtonView =
            LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)

        if (floatingButtonLayout.parent == null) {
            floatingButtonLayout.post {
                positionSurroundingButtons(mainButton, buttons, radius)
            }
            windowManager.addView(floatingButtonLayout, params)
        }
        // устанавливает слушатель изменений на ползунок громкости


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
                    Log.d("setOnTouchListener", "finger_down")
                    return@setOnTouchListener false // Пока что не потребляем событие
                }
                // Блок, где определяется подъём пальца от кнопки.
                MotionEvent.ACTION_UP -> {
                    // Устанавливаем кнопку в состоянии полупрозрачности на 0.2f
                    floatingButtonLayout.postDelayed({
                        changeOpacity(opacity)
                    }, opacityDuration)

                    // Если кнопка не сдвинулась дальше 10 пикселей на любой из осей, то считать это нажатием.
                    if (!hasMoved && abs(initialTouchX - event.rawX) < 10 && abs(
                            initialTouchY - event.rawY
                        ) < 10
                    ) {
                        toggleButtonsVisibility(buttons, mainButton)
                        // Отменяем сворачивание.
                        handler.removeCallbacks(collapseRunnable)
                        return@setOnTouchListener false
                    } else {
                        // Сохраняем и возвращаем в лямбду последнее касание.
                        lastAction = event.action
                        Log.d("finger up", "up_")
                        return@setOnTouchListener true
                    }
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
                    if (abs(initialTouchX - event.rawX) > 10 || abs(initialTouchY - event.rawY) > 10) {
                        hasMoved = true
                    }
                    Log.d("move across", "across___")
                    return@setOnTouchListener true // Потребляем событие, так как это движение
                }

                else -> return@setOnTouchListener false // Для всех остальных событий возвращаем false
            }
        }

        volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // этот метод тригерится каждый раз, когда изменяется значение ползунка.
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // проверяет, сделано ли изменение пользователем (а не программно)
                if (fromUser) {
                    // Обновляет громкость исходя из значений ползунка.
                    // Отображает индикатор громкости на UI-уровне.
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_SHOW_UI
                    )
                }
            }

            // Вызывается, когда пользователь начинает трогать ползунок
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            // Вызывается, когда пользователь завершает движение пальцем по ползунку.
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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
     * @param selectedButtons Список кнопок, которые надо расположить вокруг основной.
     * @param radius Расстояние от главной кнопки до доп.кнопок.
     */

    private fun positionSurroundingButtons(
        mainButton: View, selectedButtons: List<View>, radius: Float
    ) {
        // Рассчитываем центральные координаты главной кнпоки по осям Х/Y.
        val mainButtonCenterX = mainButton.x + mainButton.width / 2
        val mainButtonCenterY = mainButton.y + mainButton.height / 2
        // Рассчитываем приращение угла на основе количества кнопок (size = 6), чтобы расположить их равномерно
        val angleIncrement = 360.0 / selectedButtons.size
        // Итерируем по каждой кнпоке для расчёта новой позиции
        // в макете они изначально на позиции START и TOP.
        for (i in selectedButtons.indices) {
            // Рассчитаем угол для кнопки в радианах.
            val angle = i * angleIncrement * (Math.PI / 180)  // сконвертируем градусы в радианы
            // Рассчитаем координаты X/Y для текущей кнопки по углу и радиусу
            val x =
                (radius * kotlin.math.cos(angle) + mainButtonCenterX).toFloat() - selectedButtons[i].width / 2
            val y =
                (radius * kotlin.math.sin(angle) + mainButtonCenterY).toFloat() - selectedButtons[i].height / 2
            // Установим новую позицию кнопки.
            selectedButtons[i].x = x
            selectedButtons[i].y = y
        }
    }

    /**
     * Переключение видимости окружающих кнопок относительно главной кнопки.
     * Когда кнопки невидимы, эта функция разворачивает их вокруг кнопки.
     * Когда видимы - она сворачивает их обратно.
     *
     * @param selectedButtons Список кнопок.
     * @param mainButton Центральная кнопка, вокгруг которой рендерятся доп.кнопки.
     */
    private fun toggleButtonsVisibility(selectedButtons: List<View>, mainButton: View) {
        // Убедимся, отменены ли предыдущие операции со сворачиванием
        handler.removeCallbacks(collapseRunnable)  // Удалим все вызовы функции сворачивания
        Log.d("toggleVisibility", "Function called. isExpanded = $isExpanded")
        Log.d("toggleVisibility", "Number of buttons in selectedButtons: ${selectedButtons.size}")
        if (isExpanded) {
            Log.d("toggleVisibility", "Collapsing buttons...")
            // Кнопки развёрнуты. Надо свернуть
            for (button in selectedButtons) {
                // Возьмём центр основной кнопки за целевые координаты
                button.visibility = View.INVISIBLE
                button.animate().x(mainButton.x + mainButton.width / 2 - button.width / 2)
                    .y(mainButton.y + mainButton.height / 2 - button.height / 2)
                    .setDuration(300) // Длительность анимации в миллисек.
                    .withEndAction {
                        // После проигрывания анимации, спрятать кнопки.
                        button.visibility = View.INVISIBLE
                        Log.d("toggleVisibility", "Button collapsed and hidden")
                    }.start()
            }
        } else {
            Log.d("toggleVisibility", "Expanding buttons...")
            // Кнопки уже свёрнуты, надо их развернуть
            for ((index, button) in selectedButtons.withIndex()) {
                // Рассчитаем последнюю позицию кнопки.
                val (finalX, finalY) = calculateFinalPosition(
                    button, mainButton, index, selectedButtons.size
                )
                Log.d("toggleVisibility", "Button $index final position: x=$finalX, y=$finalY")
                // Установим исходную позицию кнопки в центре основной кнопки
                button.x = mainButton.x + mainButton.width / 2 - button.width / 2
                button.y = mainButton.y + mainButton.height / 2 - button.height / 2
                // Сделаем кнопку видимой.
                button.visibility = View.VISIBLE
                Log.d("toggleVisibility", "Button $index set to initial position and made visible")
                // Санимируем кнопку для перехода в финальную позицию
                button.animate().x(finalX).y(finalY)
                    .setDuration(300) // Длительность анимации в миллисек.
                    .start()
            }
        }
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

    private fun calculateFinalPosition(
        button: View, mainButton: View, index: Int, totalButtons: Int
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
        val settingsIntent = Intent(Settings.ACTION_SETTINGS)
        startActivity(settingsIntent)
    }

    private fun getPackageNamesFromSharedPreferences(): List<String> {
        // Получаем JSON строку из SharedPreferences
        val jsonString = sharedPreferences.getString("selected_packages", "")

        // Разбираем JSON строку обратно в List<String>
        return if (jsonString != "") {
            Gson().fromJson(jsonString, object : TypeToken<List<String>>() {}.type)
        } else {
            emptyList()
        }
    }

}