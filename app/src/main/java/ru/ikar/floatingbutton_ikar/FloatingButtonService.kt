package ru.ikar.floatingbutton_ikar

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.core.app.NotificationCompat
import com.google.android.material.slider.Slider
import kotlin.math.abs


class FloatingButtonService : Service() {

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var xTrackingDotsForPanel: Int = 0
    private var yTrackingDotsForPanel: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var lastAction: Int? = null
    private val layoutDimens = 430
    private val radiusFirst = 160f
    private var isExpanded = false // состояние кнопок (кнопки развернуты/свёрнуты)
    private var hasMoved = false
    private lateinit var pm: PackageManager
    private lateinit var buttons: MutableList<View>
    private lateinit var panelButtons: List<View>
    private lateinit var windowManager: WindowManager
    private lateinit var mainButton: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var volumeSliderParams: WindowManager.LayoutParams
    private lateinit var floatingButtonLayout: View
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var packageNames: List<String>
    private val actionFivePoints: String = "com.xbh.fivePoint"
    private val actionRecentTask: String = "com.xbh.action.RECENT_TASK"
    private lateinit var hideButtonsRunnable: Runnable
    private var isMoving = false
    private lateinit var settingsPanelLayout: View
    private var isSettingsPanelShown = false // состояние панели (показана/скрыта)
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var audioManager: AudioManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader
    private lateinit var surface: Surface
    private lateinit var volumeSlider: Slider
    private lateinit var volumeSliderLayout: View
    private lateinit var volumeSliderGroup: Group
    private var isVolumeSliderShown = false
    private lateinit var volumeSliderButton: ImageButton
    private var xMiddleScreenDot: Int = 0
    private var yMiddleScreenDot: Int = 0
    private var isMuted: Boolean = false
    private var currentVolume: Int = 0
    private lateinit var volumeOffPanelButton: ImageButton
    //    private var hideSliderHandler = Handler(Looper.getMainLooper())
//    private var hideSliderRunnable = Runnable {
//        volumeSliderLayout.visibility = View.GONE
//        isVolumeSliderShown = false // Обновляем счетчик, так как теперь ползунок скрыт
//        Log.d("RUNNABLE", "slider hidden")
//    }
    private lateinit var brightnessSliderLayout: View
    private lateinit var brightnessSlider: Slider
    private lateinit var brightnessSliderParams: WindowManager.LayoutParams
    private var isBrightnessSliderShown = false

    companion object {
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_INTENT = "EXTRA_RESULT_INTENT"
        const val navigationSettings = "com.xbh.navigation.settings"
        const val serviceChannelId = "fab_service_channel"
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

        // Регистрация сервиса для пяти касаний
        registerReceiverOnService()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

//        // Инициализация
//        bluetoothEnableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                // Пользователь согласился включить Bluetooth
//            } else {
//                // Пользователь отказался включать Bluetooth
//            }
//        }

        // Инициализация AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // инфлейтим макет кнопки (floating button).
        floatingButtonLayout =
            LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)
        mainButton = floatingButtonLayout.findViewById(R.id.ellipse_outer)

        volumeSliderLayout =
            LayoutInflater.from(this).inflate(R.layout.volume_slider_layout, null)
        settingsPanelLayout =
            LayoutInflater.from(this).inflate(R.layout.settings_panel_layout, null)
        brightnessSliderLayout =
            LayoutInflater.from(this).inflate(R.layout.brightness_slider_layout, null)

        buttons = mutableListOf(
            floatingButtonLayout.findViewById(R.id.settings_button),
            floatingButtonLayout.findViewById(R.id.additional_settings_button),
            floatingButtonLayout.findViewById(R.id.back_button),
            floatingButtonLayout.findViewById(R.id.home_button),
            floatingButtonLayout.findViewById(R.id.show_all_running_apps_button)
        )

        panelButtons = listOf(
            settingsPanelLayout.findViewById(R.id.wifi_panel_btn),
            settingsPanelLayout.findViewById(R.id.bluetooth_panel_btn),
            settingsPanelLayout.findViewById(R.id.volume_panel_btn),
            settingsPanelLayout.findViewById(R.id.brightness_panel_btn),
            settingsPanelLayout.findViewById(R.id.volume_off_panel_btn),
//            settingsPanelLayout.findViewById(R.id.screenshot_panel_btn)
        )

        // Добавляем базовые кнопки (включая иконки приложений)
        addButtonsToLayout()

        // Добавляем доп.панель
        addSettingsPanelToLayout()

        settingsPanelLayout.findViewById<ImageButton>(R.id.volume_off_panel_btn).also { it ->
            volumeOffPanelButton = it
            it.setOnClickListener {
                Log.d("volumeOffPanelButton", "Button clicked")
                animateButton(it)
                toggleMuteVolume()
            }
        }

        createNotificationChannel(serviceChannelId)

        addVolumeSliderToLayout()

        addBrightnessSliderToLayout()

        // Ставим слушатели на вспомогательные кнопки
        setListenersForButtons()

        // Слушатели на кнопки панели
        setListenersForPanelButtons()

        // Обновить состояние кнопки Wi-Fi при создании сервиса
        updateWifiButtonState(panelButtons[0] as ImageButton)

        // Применяем настройки WindowManager
        setupOverlayWindow()

        // Установка слушателя на основную кнопку
        onMoveButtonLogic()

        // Обновить состояние кнопки Блютуз
        updateBluetoothButtonState(panelButtons[1])

        Log.d("IS_PANEL_SHOWN","$isSettingsPanelShown")
    }


    private fun updateBluetoothButtonState(button: View) {
        if (bluetoothAdapter.isEnabled) {
            button.setBackgroundColor(getColor(R.color.bluetooth_on))
        } else {
            button.setBackgroundColor(getColor(android.R.color.transparent))
        }
    }

    private fun addSettingsPanelToLayout() {
        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        // По умолчанию скрываем панель
        settingsPanelLayout.visibility = View.GONE
        isSettingsPanelShown = false
        windowManager.addView(settingsPanelLayout, panelParams)
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
                        if (isSettingsPanelShown) {
                            settingsPanelLayout.visibility = View.GONE
                            isSettingsPanelShown = false
                        }
                        if (isVolumeSliderShown) {
                            volumeSliderLayout.visibility = View.GONE
                            isVolumeSliderShown = false
                        }
                        if (isBrightnessSliderShown) {
                            brightnessSliderLayout.visibility = View.GONE
                            isBrightnessSliderShown = false
                        }
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

                    // Обновляем позицию панели, если она отображается
                    if (isSettingsPanelShown) {
                        updatePanelPosition()
                    }

                    if (isVolumeSliderShown) {
                        volumeSliderParams.x = xMiddleScreenDot
                        volumeSliderParams.y = yMiddleScreenDot + 300
                        volumeSlider.visibility = View.VISIBLE
                        isVolumeSliderShown = true
                        windowManager.updateViewLayout(volumeSliderLayout, volumeSliderParams)
                    }

                    isMoving = true // Установить флаг, так как происходит перемещение
                    xTrackingDotsForPanel = params.x
                    yTrackingDotsForPanel = params.y
                    return@setOnTouchListener true // Потребляем событие, так как это движение
                }

                else -> return@setOnTouchListener false
            }
        }
    }

    // Функция для обновления позиции панели
    private fun updatePanelPosition() {
        val offset = convertDpToPixel(100, this) // Или любой другой нужный вам отступ
        val panelLayoutParams = settingsPanelLayout.layoutParams as WindowManager.LayoutParams
        panelLayoutParams.x = params.x + offset
        panelLayoutParams.y = params.y
        windowManager.updateViewLayout(settingsPanelLayout, panelLayoutParams)
    }

    private fun updateVolumeSliderPosition() {
        volumeSliderParams.x = xMiddleScreenDot
        volumeSliderParams.y = yMiddleScreenDot + 150
    }

    private fun registerReceiverOnService() {
        val filter = IntentFilter().apply {
            addAction(actionFivePoints)
            addAction(actionRecentTask)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
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
            PixelFormat.TRANSLUCENT
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
            // Если панель отображается и кнопки собираются свернуться, скрыть панель
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
     */

    private fun setListenersForButtons() {
        for (button in buttons) {
            button.setOnClickListener { it ->
                when (button.id) {
                    R.id.settings_button -> {
                        val navSetIntent = Intent(navigationSettings)
                        sendBroadcast(navSetIntent)
//                        settingsButtonHandler(button)
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
                        animateButton(button)
                        additionalSettingsButtonHandler()
                        Log.d("IS_MUTED","$isMuted")
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

    private fun setListenersForPanelButtons() {
        for (button in panelButtons) {
            button.setOnClickListener {
                when (button.id) {
                    R.id.wifi_panel_btn -> {
                        Log.d("WIFI_BTN", "id = ")
                        animateButton(button)
                        wifiBtnHandler()
                    }

                    R.id.bluetooth_panel_btn -> {
                        animateButton(button)
                        toggleBluetooth()
                    }

                    R.id.volume_panel_btn -> {
                        animateButton(button)
                        volumeButtonHandler()
//                        updateWifiButtonState(button as ImageButton)
                    }

                    R.id.brightness_panel_btn -> {
                        animateButton(button)
                        handleBrightnessBtn()
                    }

                    R.id.volume_off_panel_btn -> {
                        animateButton(button)
                        toggleMuteVolume()
                    }
//                    R.id.screenshot_panel_btn -> {
//                        animateButton(button)
//                    }
                }
            }
        }
    }

    private fun volumeButtonHandler() {
        if (!isVolumeSliderShown) { // Условие, если ползунок еще не отображен
            // Сначала проверяем, отображается ли brightnessSlider
            if (isBrightnessSliderShown) {
                brightnessSliderLayout.visibility = View.GONE
                isBrightnessSliderShown = false
            }
            // Определяем параметры макета для ползунка громкости
            val volumeParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            // Устанавливаем позицию макета volumeSliderLayout
            volumeSliderParams.x = xTrackingDotsForPanel
            volumeSliderParams.y = yTrackingDotsForPanel
            // Обновляем ползунок согласно системным значениям громкости
            // Устанавливаем максимальное значение слайдера
            volumeSlider.apply {
                valueTo =
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
                // Устанавливаем текущее значение слайдера
                value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            }
            volumeSliderLayout.visibility = View.VISIBLE
            isVolumeSliderShown = true
            updateVolumeSliderPosition()
//            resetHideVolumeSliderTimer()
        } else {
            // Если ползунок уже отображен, удаляем с экрана
            volumeSliderLayout.visibility = View.GONE
            isVolumeSliderShown = false
//            windowManager.removeView(volumeSliderLayout)
        }
    }

    private fun wifiBtnHandler() {
        val wifiIntent = Intent(Settings.Panel.ACTION_WIFI)
        wifiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Добавление флага
        startActivity(wifiIntent)
    }

    private fun updateWifiButtonState(button: ImageButton) {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifiManager?.let {
            val isWifiEnabled = it.isWifiEnabled

            if (isWifiEnabled) {
                // Установить синий фон, если Wi-Fi включен
                button.setBackgroundColor(getColor(R.color.bluetooth_on)) // Используйте свой синий цвет
            } else {
                // Установить прозрачный фон, если Wi-Fi выключен
                button.setBackgroundColor(getColor(android.R.color.transparent))
            }
        }
    }


    private fun toggleBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable() // Выключить Bluetooth
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(enableBtIntent) // Запросить включение Bluetooth
        }
    }

    private fun animateButton(button: View) {
        button.startAnimation(
            AnimationUtils.loadAnimation(
                this, R.anim.button_animation
            )
        )
    }

    private fun additionalSettingsButtonHandler() {
        // Загружаем анимацию прозрачности
        val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation)

        // Анимация исчезновения (fade out) - если нужна
        val fadeOutAnim = AnimationUtils.loadAnimation(this, R.anim.fade_out_animation)

        // Проверяем, отображена ли панель
        if (!isSettingsPanelShown) {
            // Устанавливаем позицию и размеры панели
            val offset = convertDpToPixel(100, this)
            val layoutParams = settingsPanelLayout.layoutParams as WindowManager.LayoutParams
            layoutParams.x = xTrackingDotsForPanel + offset
            layoutParams.y = yTrackingDotsForPanel
            windowManager.updateViewLayout(settingsPanelLayout, layoutParams)

            // Показываем панель и применяем анимацию появления
            settingsPanelLayout.visibility = View.VISIBLE
            isSettingsPanelShown = true
            settingsPanelLayout.startAnimation(fadeInAnim)
        } else {
            // Применяем анимацию исчезновения и скрываем панель
            settingsPanelLayout.startAnimation(fadeOutAnim)
            settingsPanelLayout.visibility = View.GONE
            isSettingsPanelShown = false
        }
//
//        // Переключаем состояние видимости панели
//        isSettingsPanelShown = !isSettingsPanelShown
    }

    private fun onFloatingButtonClick(button: View) {
        val intent = Intent("com.myapp.ACTION_PERFORM_BACK")
        sendBroadcast(intent)
        animateButton(button)
    }

    private fun onShowRecentAppsButtonClick(button: View) {
        val intent = Intent("com.xbh.action.RECENT_TASK")
        sendBroadcast(intent)
        animateButton(button)
    }

    /**
     * вызываем функцию `onDestroy`, когда сервис уничтожается.
     * Проходим по if и проверяем, всё ли уничтожилось.
     */
    override fun onDestroy() {
        Log.d("FloatingButtonService", "onDestroy() - начало")
        super.onDestroy()
        // Удаление основного layout
        if (::floatingButtonLayout.isInitialized) {
            Log.d("FloatingButtonService", "Removing floatingButtonLayout")
            windowManager.removeView(floatingButtonLayout)
        }
        // Удаление панели настроек
        if (::settingsPanelLayout.isInitialized) {
            Log.d("FloatingButtonService", "Removing settingsPanelLayout")
            windowManager.removeView(settingsPanelLayout)
        }
        // Удаление ползунка громкости
        if (::volumeSliderLayout.isInitialized) {
            Log.d("FloatingButtonService", "Removing volumeSliderLayout")
            windowManager.removeView(volumeSliderLayout)
        }
        // Удаление ползунка яркости
        if (::brightnessSliderLayout.isInitialized) {
            Log.d("FloatingButtonService", "Removing brightnessSliderLayout")
            windowManager.removeView(brightnessSliderLayout)
        }
        unregisterReceiver(reciever)
//        applicationContext.stopService(Intent(this, FloatingButtonService.class::java))
    }

    //Обработчик кнопки "ДОМОЙ"
    private fun homeButtonHandler(button: View) {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(homeIntent)
        animateButton(button)
    }

    private fun settingsButtonHandler(button: View) {
        try {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Добавление флага
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

        // Получение данных о захвате экрана
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_INTENT)

        if (resultCode != null && resultData != null && resultCode == Activity.RESULT_OK) {
            val mediaProjectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)

            createVirtualDisplay()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                serviceChannelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)

            val notification = NotificationCompat.Builder(this, serviceChannelId)
                .setContentTitle("FAB_Service")
                .setContentText("Сервис запущен")
                .setSmallIcon(R.drawable.ikar_fab_img)
                .build()

            startForeground(1123124590, notification)
        }

        return START_NOT_STICKY
    }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val serviceChannel = NotificationChannel(
//                serviceChannelId,
//                "Foreground Service Channel",
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//
//            val notificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(serviceChannel)
//        }

//        val notification =
//            NotificationCompat.Builder(this, serviceChannelId).setContentTitle("FAB_Service")
//                .setContentText("Сервис запущен").setSmallIcon(R.drawable.ic_launcher_background)
//                .build()

//        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        mediaProjection = resultData?.let {
//            mediaProjectionManager.getMediaProjection(resultCode,
//                it
//            )
//        }!!
//
//        createVirtualDisplay()
//
//        startForeground(1123124590, notification)
//
//        return START_NOT_STICKY
//    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createVirtualDisplay() {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        surface = imageReader.surface

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "FloatingButtonService",
            width,
            height,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )

        // Здесь можно добавить listener для imageReader, чтобы обрабатывать захваченные изображения
    }

    private fun convertDpToPixel(dp: Int, context: Context) =
        (dp * context.resources.displayMetrics.density).toInt()


//    // Метод для установки уровня громкости на 0
//    private fun muteVolume() {
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
//    }

    private fun toggleMuteVolume() {
        if (isMuted) {
            // Включаем звук
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
            volumeSlider.value = currentVolume.toFloat()
            isMuted = false
        } else {
            // Выключаем звук
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            volumeSlider.value = 0f
            isMuted = true
        }
        updateMuteButtonState()
    }


    private fun updateMuteButtonState() {
        if (isMuted) {
            // Устанавливаем синий фон, когда звук выключен
            volumeOffPanelButton.setBackgroundColor(getColor(R.color.bluetooth_on))
            volumeSliderButton.setImageResource(R.drawable.volume_off_icon)
        } else {
            // Устанавливаем прозрачный фон, когда звук включен
            volumeOffPanelButton.setBackgroundColor(getColor(android.R.color.transparent))
            volumeSliderButton.setImageResource(R.drawable.volume_on_icon)

        }
    }

    private fun addVolumeSliderToLayout() {
        volumeSlider = volumeSliderLayout.findViewById(R.id.volume_slider)
        volumeSliderButton = volumeSliderLayout.findViewById(R.id.on_off_mute_btn)
        volumeSliderGroup = volumeSliderLayout.findViewById(R.id.volume_slider_group)

//        // Обработчик касания для перезапуска таймера
//        volumeSliderLayout.setOnTouchListener { _, event ->
//            if (event.action == MotionEvent.ACTION_DOWN) {
//                resetHideVolumeSliderTimer()
//            }
//            true
//        }

        volumeSliderLayout.visibility = View.GONE
        isVolumeSliderShown = false
//        // Инициализация Handler и Runnable
//        hideSliderHandler = Handler(Looper.getMainLooper())
//        hideSliderRunnable = Runnable {
//            volumeSliderLayout.visibility = View.GONE
//            isVolumeSliderShown = false
//        }

        // Начальный запуск таймера
//        resetHideVolumeSliderTimer()

        volumeSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                Log.d("VerticalSlider", "Slider progress changed: $progress")
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    progress,
                    AudioManager.FLAG_SHOW_UI
                )
                isMuted = progress == 0
                updateMuteButtonState()
//                if (progress == 0) {
//                    // Пользователь установил звук на 0
//                    isMuted = true
//                    volumeSliderButton.setImageResource(R.drawable.volume_off_icon)
//                    volumeOffPanelButton.setBackgroundColor(getColor(R.color.bluetooth_on))
//                } else if (isMuted) {
//                    // Пользователь увеличил громскость с 0
//                    isMuted = false
//                    volumeSliderButton.setImageResource(R.drawable.volume_on_icon)
////                    volumeOffPanelButton.setBackgroundColor(getColor(android.R.color.transparent))
//                }
//                resetHideVolumeSliderTimer()
            }
//            updateMuteButtonState()
        }


        volumeSliderButton.setOnClickListener {
            Log.d("VolumeButton", "Button clicked")
            toggleMuteVolume()
//            resetHideVolumeSliderTimer()
        }

        volumeSliderParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        volumeSliderParams.x = xMiddleScreenDot
        volumeSliderParams.y = yMiddleScreenDot + 150

        // Настройка параметров окна
        windowManager.addView(volumeSliderLayout, volumeSliderParams)

        volumeSliderLayout.visibility = View.GONE
        isVolumeSliderShown = false
    }

    private fun addBrightnessSliderToLayout() {
        brightnessSlider = brightnessSliderLayout.findViewById(R.id.brightness_slider)

        brightnessSliderParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        brightnessSliderParams.x = xMiddleScreenDot
        brightnessSliderParams.y = yMiddleScreenDot + 150

        windowManager.addView(brightnessSliderLayout, brightnessSliderParams)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(applicationContext)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }

        // Получаем текущую яркость окна
        val currentBrightness = WindowManager.LayoutParams().screenBrightness
        if (currentBrightness == -1.0f) {
            // Используем системную яркость
            val systemBrightnessValue = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                255
            )
            val sliderValue =
                systemBrightnessValue.toFloat() / 255f // Преобразование в диапазон слайдера
            brightnessSlider.value = sliderValue
        } else {
            // Используем текущую яркость окна
            brightnessSlider.value = currentBrightness
        }

        // Слушатель изменений ползунка
        brightnessSlider.addOnChangeListener { _, value, _ ->
            setSystemBrightness(this@FloatingButtonService, (value * 255).toInt())
        }

        brightnessSliderLayout.also {
            if (!isBrightnessSliderShown) {
                Log.d("isBrightnessSliderShown", "${!isBrightnessSliderShown}")
                it.visibility = View.GONE
            } else {
                it.visibility = View.VISIBLE
            }
        }
    }

    private fun handleBrightnessBtn() {
        // Показать или скрыть слайдер яркости
        if (!isBrightnessSliderShown) {
            // Сначала проверяем, отображается ли volumeSlider
            if (isVolumeSliderShown) {
                volumeSliderLayout.visibility = View.GONE
                isVolumeSliderShown = false
            }
            brightnessSliderLayout.visibility = View.VISIBLE
            isBrightnessSliderShown = true
        } else {
            brightnessSliderLayout.visibility = View.GONE
            isBrightnessSliderShown = false
        }
    }

    private fun setSystemBrightness(context: Context, brightnessValue: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        brightnessValue
                    )
                }
            } else {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightnessValue
                )
            }
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
    }

    private val reciever: BroadcastReceiver = object : BroadcastReceiver() {
        private val actionFivePoints = "com.xbh.fivePoint"

        override fun onReceive(context: Context, intent: Intent) {
            Log.d("FloatingButtonService", "BroadcastReceiver - onReceive: ${intent.action}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, FloatingButtonService::class.java))
            } else {
                context.startService(Intent(context, FloatingButtonService::class.java))
            }
            if (intent.action == actionFivePoints) {
                val posX: Int? = intent.extras?.getInt("PosX")
                val posY: Int? = intent.extras?.getInt("PosY")

                val screenSize = getScreenSize(context)
                val screenWidth = screenSize.x
                val screenHeight = screenSize.y

                xMiddleScreenDot = screenWidth / 2
                yMiddleScreenDot = screenHeight / 2

                params.x = posX!! - xMiddleScreenDot
                params.y = posY!! - yMiddleScreenDot
                windowManager.updateViewLayout(floatingButtonLayout, params)
            } else if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        updateBluetoothButtonState(panelButtons[1])
                    }

                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        // Bluetooth is turning off;
                    }

                    BluetoothAdapter.STATE_ON -> {
                        updateBluetoothButtonState(panelButtons[1])
                    }

                    BluetoothAdapter.STATE_TURNING_ON -> {
                        // Bluetooth is turning on
                    }
                }
            }
        }
    }

}