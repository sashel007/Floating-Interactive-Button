package ru.ikar.floatingbutton_ikar

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Point
import android.media.AudioManager
import android.os.Build
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
import androidx.core.app.NotificationCompat
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
    private lateinit var pm: PackageManager
    private lateinit var buttons: MutableList<View>
    private lateinit var windowManager: WindowManager
    private lateinit var mainButton: View
    private lateinit var volumeSliderLayout: View
    private lateinit var volumeSlider: SeekBar
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    private lateinit var floatingButtonLayout: View
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var packageNames: List<String>
    private lateinit var testView: View
    private lateinit var overlayViewLayout: View
    private val ACTION_FIVE_POINTS: String = "com.xbh.fivePoint"
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var hideButtonsRunnable: Runnable
    private var isMoving = false

    companion object {
        private const val REQUEST_CODE = 101 // or any other integer
        private const val FADE_DURATION = 250L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        pm = this.packageManager
        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager // инициализация WindowManager для кастомных настроек отображения.
        audioManager =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager // инициализация AudioManager для управления аудио-настройками.
        sharedPreferences = getSharedPreferences("app_package_names", Context.MODE_PRIVATE)
        packageNames = getPackageNamesFromSharedPreferences()

        val filter = IntentFilter()
        filter.addAction(ACTION_FIVE_POINTS)
        registerReceiver(reciever, filter)

        // инфлейтим макет кнопки (floating button).
        floatingButtonLayout =
            LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null) as FrameLayout
        testView = floatingButtonLayout.findViewById(R.id.floating_button)
        // прозрачный оверлей
        overlayViewLayout =
            LayoutInflater.from(this).inflate(R.layout.fullscreen_overlay, null) as FrameLayout
        // Получаем ссылку на главную кнопку внутри макета floating button
        mainButton = floatingButtonLayout.findViewById(R.id.ellipse_outer)

        buttons = mutableListOf(
            floatingButtonLayout.findViewById(R.id.settings_button),
            floatingButtonLayout.findViewById(R.id.volume_button),
            floatingButtonLayout.findViewById(R.id.home_button),
            floatingButtonLayout.findViewById(R.id.back_button),
            floatingButtonLayout.findViewById(R.id.background_button),
            floatingButtonLayout.findViewById(R.id.show_all_running_apps_button)
        )

        addButtonsToLayout()

        setListenersForButtons()

        Log.d("DEBUG", "selectedButtons: $buttons")
        // инфлейтим ползунок громкости, который отображается по нажатию кнопки громкости
        volumeSliderLayout = LayoutInflater.from(this).inflate(R.layout.volume_slider_layout, null)
        // даём ссылку на слайдер внутри макета volume slider.
        volumeSlider = volumeSliderLayout.findViewById(R.id.volume_slider)
        // Определеяем параметры макета для отображения вьюшки с кнопкой.
        // Эти настройки определяют то, как вьюшка отображается на экране.

        // set up params
        setupOverlayWindow()

        setupAndDisplayOverlayButtons()

        floatingButtonLayout.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                // Обработка действий при первом касании пальца (new pointer)
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    // Если это касание од  ним пальцем то выполняется этот блок:
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
                    isMoving = false // Сбросить флаг перед началом нового касания
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
                    Log.d("EVENT RAW X: ", (event.rawX).toString())
                    Log.d("EVENT RAW Y: ", (event.rawY).toString())
                    Log.d("EVENT INITIONAL X: ", (initialX).toString())
                    Log.d("EVENT INITIONAL Y: ", (initialY).toString())
                    Log.d("EVENT INITIONAL TOUCH X: ", (initialTouchX).toString())
                    Log.d("EVENT INITIONAL TOUCH Y: ", (initialTouchX).toString())
                    Log.d("EVENT INITIONAL TOUCH Y: ", "--------------------------")

                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    Log.d("PARAMS_X", "$params.x  $params.y")
                    // Обновляем позицию кнопки по новым координатам
                    windowManager.updateViewLayout(floatingButtonLayout, params)
                    lastAction = event.action
                    // Определяем, надо ли фиксировать движение (если палец ушёл дальше 10 пикселей)
                    // Если надо, то устанавливаем флаг на hasMoved
                    if (abs(initialTouchX - event.rawX) > 10 || abs(initialTouchY - event.rawY) > 10) {
                        hasMoved = true
                    }
                    Log.d("move across", "across___")
                    isMoving = true // Установить флаг, так как происходит перемещение
                    return@setOnTouchListener true // Потребляем событие, так как это движение
                }

                else -> return@setOnTouchListener false
            }
        }


        volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // этот метод тригерится каждый раз, когда изменяется значение ползунка.
            override fun onProgressChanged(
                seekBar: SeekBar?, progress: Int, fromUser: Boolean
            ) {
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

    private fun setupAndDisplayOverlayButtons() {
        if (floatingButtonLayout.parent == null) {
            floatingButtonLayout.post {
                positionSurroundingButtons(mainButton, buttons)
            }
            Log.d("PARAMS_", "$params")
            windowManager.addView(floatingButtonLayout, params)
        }
    }

    private fun addButtonsToLayout() {
        for (packageName in packageNames) {
            try {
                val appIcon = pm.getApplicationIcon(packageName)
                val newButton = ImageButton(this)
                newButton.rotation = 180f
                newButton.setImageDrawable(appIcon)

                newButton.tag = packageName
                newButton.scaleType = ImageView.ScaleType.CENTER_INSIDE // Добавьте эту строку

                // Преобразование dp в пиксели
                val pixelsWidth = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics
                ).toInt()
                val pixelsHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics
                ).toInt()

                // Установка размеров для кнопки
                val layoutParams = FrameLayout.LayoutParams(pixelsWidth, pixelsHeight)
                newButton.layoutParams = layoutParams

                // Добавление кнопки в layout и список
                (floatingButtonLayout as FrameLayout).addView(newButton)
                buttons.add(newButton)
                for (button in buttons) {
                    button.visibility = View.INVISIBLE
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    private fun setupOverlayWindow() {
        params = createLayoutParams()
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
    }

    // Функция изменения прозрачности для floatingButtonLayout.
    private fun changeOpacity(toOpacity: Float) {
        // Используем ObjectAnimator, чтобы санимировать плавную прозрачность
        ObjectAnimator.ofFloat(floatingButtonLayout, "alpha", toOpacity).apply {
            duration = FADE_DURATION  // длительность перехода
            start() // Начать анимацию.
        }
    }

    /**
     * Позиционирование маленьких (доп.) кнопок по окружности.
     *
     * @param mainButton Исходная большая кнопка, вокруг которой размещаются доп.кнопки.
     * @param selectedButtons Список кнопок, которые надо расположить вокруг основной.
     * @param radius Расстояние от главной кнопки до доп.кнопок.
     */

    private fun positionSurroundingButtons(
        mainButton: View,
        selectedButtons: List<View>
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
        hideButtonsRunnable = Runnable {
            if (isExpanded) {
                toggleButtonsVisibility(selectedButtons, mainButton)
            }
        }
        // Убедимся, отменены ли предыдущие операции со сворачиванием
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
                        button.visibility =
                            View.INVISIBLE // После проигрывания анимации, спрятать кнопки.
                        Log.d("toggleVisibility", "Button collapsed and hidden")
                    }.start()
            }
            handler.postDelayed(hideButtonsRunnable, 5000)
        } else {
            Log.d("toggleVisibility", "Expanding buttons...")
            // Кнопки уже свёрнуты, надо их развернуть
            for ((index, button) in selectedButtons.withIndex()) {
                // Рассчитаем последнюю позицию кнопки.
                val (finalX, finalY) = calculateFinalPosition(
                    button,
                    mainButton,
                    index,
                    selectedButtons.size
                )
                Log.d("toggleVisibility", "Button $index final position: x=$finalX, y=$finalY")
                // Установим исходную позицию кнопки в центре основной кнопки
                button.x = mainButton.x + mainButton.width / 2 - button.width / 2
                button.y = mainButton.y + mainButton.height / 2 - button.height / 2
                button.visibility = View.VISIBLE // Сделаем кнопку видимой.
                Log.d("toggleVisibility", "Button $index set to initial position and made visible")
                button.animate().x(finalX)
                    .y(finalY) // Санимируем кнопку для перехода в финальную позицию
                    .setDuration(300) // Длительность анимации в миллисек.
                    .start()
            }
        }
        isExpanded =
            !isExpanded // Меняем состояние развёрнутости (если развёрнуто, свернём - и обратно)
    }

    /**
     * Рассчитаем последнюю позицию доп.кнопки, когда она развёрнута вокруг основной.
     * Это достигается путём упорядочивания кнопок по окружности (выше был метод).
     * @param button Кнопка, позицию которой надо рассчитать.
     * @param mainButton Основная кнопка.
     * @param index Номер кнопки при итерировании по списку.
     * @param totalButtons Количество кнопок, которые надо равномерно распределить.
     * @return Возвращаем Float-значение, в которое вкладываем X/Y координаты кнопок.
     */
    private fun calculateFinalPosition(
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

    private fun setListenersForButtons() {
        for (button in buttons) {
            button.setOnClickListener {
                handler.removeCallbacks(hideButtonsRunnable)

                when (button.id) {
                    R.id.settings_button -> {
                        settingsButtonHandler()
                    }

                    R.id.volume_button -> {
                        volumeButtonHandler()
                    }

                    R.id.home_button -> {
                        homeButtonHandler()
                    }

                    R.id.back_button -> {
//                        backButtonHandler()
                        onFloatingButtonClick()
//                        onShowRecentAppsButtonClick()
                    }

                    R.id.show_all_running_apps_button -> {
//                        showRunningAppsButtonHandler()
                        onShowRecentAppsButtonClick()
                    }


                    else -> {
                        Log.d(
                            "ButtonClick", "App icon clicked: ${button.tag as? String ?: "Unknown"}"
                        )
                        // Если кнопка не является одной из базовых, считаем ее кнопкой-иконкой приложения
                        val launchIntent =
                            packageManager.getLaunchIntentForPackage(button.tag as? String ?: "")
                        launchIntent?.let { startActivity(it) }
                    }

                }
                if (isExpanded) {
                    handler.postDelayed(hideButtonsRunnable, 700)
                }
            }
        }
    }

    private fun backButtonHandler() {
        floatingButtonLayout.setOnClickListener {
//            GlobalActionService.instance?.performBackAction()
            Log.d("BUTTONBACK", "backButtonHandler: ")

        }
    }


    fun onFloatingButtonClick() {
        val intent = Intent("com.myapp.ACTION_PERFORM_BACK")
        sendBroadcast(intent)
    }

    fun onShowRecentAppsButtonClick() {
        val intent = Intent("com.myapp.ACTION_SHOW_RECENT_APPS")
        sendBroadcast(intent)
    }

    private fun showRunningAppsButtonHandler() {
        val showRunningAppsIntent = Intent("com.android.sys")

//        val homeIntent = Intent(Intent.ACTION_MAIN)
//        homeIntent.addCategory(Intent.CATEGORY_HOME)
//        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        startActivity(homeIntent)
//        Log.d("Button", "Home clicked")

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
        unregisterReceiver(reciever)
    }

    //обработчик кнопки "Громкость"
    private fun volumeButtonHandler() {
        if (volumeSliderLayout.parent == null) { // Условие, если ползунок еще не отображен
            // Определяем параметры макета для ползунка громкости
            val volumeParams = createLayoutParams()
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
        val keys = listOf(
            "package_name_key_0", "package_name_key_1", "package_name_key_2", "package_name_key_3"
        )
        val packageNames = mutableListOf<String>()
        keys.forEach { key ->
            sharedPreferences.getString(key, null)?.let {
                packageNames.add(it)
            }
        }
        Log.d("проверка_", "$packageNames")
        val allEntries = sharedPreferences.all
        allEntries.forEach { (key, value) ->
            Log.d("SharedPreferences__", "$key: $value")
        }
        return packageNames
    }

    private fun getScreenSize(context: Context): Point {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val SERVICE_CHANNEL_ID = "fab_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }

        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("FAB_Service")
            .setContentText("Сервис запущен")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()

        startForeground(1123124590, notification)

        return START_NOT_STICKY
    }

    private val reciever: BroadcastReceiver = object : BroadcastReceiver() {
        private val ACTION_FIVE_POINTS = "com.xbh.fivePoint"

        override fun onReceive(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, FloatingButtonService::class.java))
            } else {
                context.startService(Intent(context, FloatingButtonService::class.java))
            }

            if (intent.action == ACTION_FIVE_POINTS) {
                val posX: Int? = intent.extras?.getInt("PosX")
                val posY: Int? = intent.extras?.getInt("PosY")

                val screenSize = getScreenSize(context)
                val screenWidth = screenSize.x
                val screenHeight = screenSize.y

                params.x = posX!! - screenWidth / 2
                params.y = posY!! - screenHeight / 2
                if (floatingButtonLayout.visibility == View.INVISIBLE
                    || floatingButtonLayout.visibility == View.GONE
                ) {
                    floatingButtonLayout.visibility = View.VISIBLE
                    windowManager.updateViewLayout(floatingButtonLayout, params)
                }
                windowManager.updateViewLayout(floatingButtonLayout, params)

            }
        }
    }

}