package ru.ikar.floatingbutton_ikar

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
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingButtonService : Service() {

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var lastAction: Int? = null
    private val layoutDimens = 430
    private val radiusFirst = 160f
    private var isExpanded = false // состояние кнопок (кнопки развернуты/свёрнуты)
    private var hasMoved = false
    private lateinit var pm: PackageManager
    private lateinit var buttons: MutableList<View>
    private lateinit var windowManager: WindowManager
    private lateinit var mainButton: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var floatingButtonLayout: View
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var packageNames: List<String>
    private val ACTION_FIVE_POINTS: String = "com.xbh.fivePoint"
    private val ACTION_RECENT_TASK: String = "com.xbh.action.RECENT_TASK"
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
        sharedPreferences = getSharedPreferences("app_package_names", Context.MODE_PRIVATE)
        packageNames = getPackageNamesFromSharedPreferences()

        // Регистрация сервиса
        registerReceiverOnService()

        // инфлейтим макет кнопки (floating button).
        floatingButtonLayout =
            LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)
        mainButton = floatingButtonLayout.findViewById(R.id.ellipse_outer)

        buttons = mutableListOf(
            floatingButtonLayout.findViewById(R.id.settings_button),
            floatingButtonLayout.findViewById(R.id.additional_settings_button),
            floatingButtonLayout.findViewById(R.id.back_button),
            floatingButtonLayout.findViewById(R.id.home_button),
            floatingButtonLayout.findViewById(R.id.show_all_running_apps_button)
        )

        // Добавляем базовые кнопки (включая иконки приложений)
        addButtonsToLayout()

        // Ставим слушатели на вспомогательные кнопки
        setListenersForButtons()

        // Применяем настройки WindowManager
        setupOverlayWindow()

        // Установка слушателя на основную кнопку
        onMoveButtonLogic()

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun onMoveButtonLogic() {
        floatingButtonLayout.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    // Сохраняем исходную позицию пальца
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    // Обновляем флаг определения движения.
                    hasMoved = false
                    // Сохраняем последнее действие при касании.
                    lastAction = event.action
                    isMoving = false // Сбросить флаг перед началом нового касания
                    return@setOnTouchListener false // Пока что не потребляем событие
                }

                MotionEvent.ACTION_UP -> {
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

                        return@setOnTouchListener true
                    }
                }

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
                    isMoving = true // Установить флаг, так как происходит перемещение
                    return@setOnTouchListener true // Потребляем событие, так как это движение
                }

                else -> return@setOnTouchListener false
            }
        }
    }

    private fun registerReceiverOnService() {
        val filter = IntentFilter().apply {
            addAction(ACTION_FIVE_POINTS)
            addAction(ACTION_RECENT_TASK)
        }
        registerReceiver(reciever, filter)
    }

    private fun addButtonsToLayout() {
        for (packageName in packageNames) {
            try {
                val appIcon = pm.getApplicationIcon(packageName)
                val newButton = ImageButton(this)
                val size = 130

                newButton.setImageDrawable(appIcon)
                newButton.tag = packageName
                newButton.scaleType = ImageView.ScaleType.CENTER_CROP// Добавьте эту строку
                newButton.background = null

                // Установка размеров для кнопки
                val layoutParams = FrameLayout.LayoutParams(size, size)
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

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            layoutDimens,
            layoutDimens,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
    }

    private fun setupOverlayWindow() {
        params = createLayoutParams()
        windowManager.addView(floatingButtonLayout, params)
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
            // Кнопки развёрнуты. Надо свернуть
            for (button in selectedButtons) {
                // Возьмём центр основной кнопки за целевые координаты
                button.animate().x(mainButton.x + mainButton.width / 2 - button.width / 2)
                    .y(mainButton.y + mainButton.height / 2 - button.height / 2).scaleX(0f)
                    .scaleY(0f)  // Добавление уменьшения
                    .alpha(0f)              // Добавление анимации прозрачности
                    .setDuration(300).withEndAction {
                        button.visibility = View.INVISIBLE
                    }.start()
            }
        } else {
            // Кнопки уже свёрнуты, надо их развернуть
            for ((index, button) in selectedButtons.withIndex()) {
                // Рассчитаем последнюю позицию кнопки
                val (finalX, finalY) = calculateFinalPosition(
                    button, mainButton, index, selectedButtons.size
                )

                // Устанавливаем начальную позицию и делаем кнопку видимой
                button.x = mainButton.x + mainButton.width / 2 - button.width / 2
                button.y = mainButton.y + mainButton.height / 2 - button.height / 2
                button.visibility = View.VISIBLE

                // Восстанавливаем прозрачность и масштаб до исходных значений
                button.alpha = 1f
                button.scaleX = 1f
                button.scaleY = 1f

                // Анимация перемещения кнопки в финальную позицию
                button.animate().x(finalX).y(finalY).setDuration(300).start()
            }
        }
        // Меняем состояние развёрнутости (если развёрнуто, свернём - и обратно)
        isExpanded = !isExpanded
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
        button: View, mainButton: View, index: Int, totalButtons: Int
    ): Pair<Float, Float> {
        val mainButtonCenterX = mainButton.x + mainButton.width / 2
        val mainButtonCenterY = mainButton.y + mainButton.height / 2
        val angleIncrement = 360.0 / totalButtons
        val angle =
            index * angleIncrement * (Math.PI / 180)  // Конвертирует угол из градусов в радианы
        val finalX =
            (radiusFirst * kotlin.math.cos(angle) + mainButtonCenterX).toFloat() - button.width / 2
        val finalY =
            (radiusFirst * kotlin.math.sin(angle) + mainButtonCenterY).toFloat() - button.height / 2
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
                when (button.id) {
                    R.id.settings_button -> {
                        settingsButtonHandler(button)
                    }

                    R.id.home_button -> {
                        homeButtonHandler(button)
                    }

                    R.id.back_button -> {
                        onFloatingButtonClick(button)
                    }

                    R.id.show_all_running_apps_button -> {
                        onShowRecentAppsButtonClick(button)
                    }

                    R.id.additional_settings_button -> {
                        additionalSettingsButtonHandler(button)
                    }

                    else -> {
                        // Если кнопка не является одной из базовых, считаем ее кнопкой-иконкой приложения
                        val launchIntent =
                            packageManager.getLaunchIntentForPackage(button.tag as? String ?: "")
                        launchIntent?.let { startActivity(it) }
                        animateButton(it)
                    }

                }
            }
        }
    }

    private fun animateButton(button: View) {
        button.startAnimation(
            AnimationUtils.loadAnimation(
                this, R.anim.button_animation
            )
        )
    }

    private fun additionalSettingsButtonHandler(button: View) {

    }

    private fun onFloatingButtonClick(button: View) {
        val intent = Intent("com.myapp.ACTION_PERFORM_BACK")
        sendBroadcast(intent)
        animateButton(button)
    }

    private fun onShowRecentAppsButtonClick(button: View) {
        val intent = Intent("com.xbh.action.RECENT_TASK")
        sendBroadcast(intent)
    }

    /**
     * вызываем функцию `onDestroy`, когда сервис уничтожается.
     * Проходим по if и проверяем, всё ли уничтожилось.
     */
    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingButtonLayout)
        unregisterReceiver(reciever)
    }

    //Обработчик кнопки "ДОМОЙ"
    private fun homeButtonHandler(button: View) {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        animateButton(button)
    }

    private fun settingsButtonHandler(button: View) {
        try {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Добавление ф
            startActivity(settingsIntent)
            animateButton(button)
        } catch (e: Exception) {
            Log.e("settingsButtonHandler", "Ошибка при попытке открыть системные настройки", e)
        }
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

        val notification =
            NotificationCompat.Builder(this, SERVICE_CHANNEL_ID).setContentTitle("FAB_Service")
                .setContentText("Сервис запущен").setSmallIcon(R.drawable.ic_launcher_background)
                .build()

        startForeground(1123124590, notification)

        return START_NOT_STICKY
    }

    private val reciever: BroadcastReceiver = object : BroadcastReceiver() {
        private val ACTION_FIVE_POINTS = "com.xbh.fivePoint"
        private val ACTION_RECENT_TASK = "com.xbh.action.RECENT_TASK"

        override fun onReceive(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, FloatingButtonService::class.java))
            } else {
                context.startService(Intent(context, FloatingButtonService::class.java))
            }
            Log.d("PPOIUYTRE", "onReceive: ." + intent.action)
            if (intent.action == ACTION_FIVE_POINTS) {
                Log.d("UFEGIUHEIUFHIWEF", "onReceive: .")
                val posX: Int? = intent.extras?.getInt("PosX")
                val posY: Int? = intent.extras?.getInt("PosY")

                val screenSize = getScreenSize(context)
                val screenWidth = screenSize.x
                val screenHeight = screenSize.y

                params.x = posX!! - screenWidth / 2
                params.y = posY!! - screenHeight / 2
                windowManager.updateViewLayout(floatingButtonLayout, params)
            }
        }
    }
}