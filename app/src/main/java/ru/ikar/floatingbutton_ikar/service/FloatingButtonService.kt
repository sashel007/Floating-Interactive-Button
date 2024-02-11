package ru.ikar.floatingbutton_ikar.service

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
import android.hardware.display.VirtualDisplay
import android.media.AudioManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Magnifier
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.Group
import androidx.core.app.NotificationCompat
import com.google.android.material.slider.Slider
import ru.ikar.floatingbutton_ikar.R
import ru.ikar.floatingbutton_ikar.buttons.ButtonManager
import ru.ikar.floatingbutton_ikar.buttons.PanelButtonManager
import ru.ikar.floatingbutton_ikar.projector.Projector
import ru.ikar.floatingbutton_ikar.sharedpreferences.ButtonPreferenceUtil
import kotlin.math.abs


class FloatingButtonService : Service(), MuteStateListener, WifiStateUpdater,
    BluetoothStateUpdater, SettingsPanelController, VolumeControllerListener {

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var xTrackingDotsForPanel: Int = 0
    private var yTrackingDotsForPanel: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var lastAction: Int? = null
    private val expandedLayoutDimens = 430
    private val collapsedLayoutDimens = 100
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
    private lateinit var panelParams: WindowManager.LayoutParams
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
    private lateinit var magnifierViewLayout: View
    private lateinit var magnifierViewParams: WindowManager.LayoutParams
    private lateinit var magnifierImageView: ImageView
    private lateinit var brightnessSliderLayout: View
    private lateinit var brightnessSlider: Slider
    private lateinit var brightnessSliderParams: WindowManager.LayoutParams
    private var isBrightnessSliderShown = false
    private lateinit var projector: Projector
    private lateinit var bm: ButtonManager
    private lateinit var pbm: PanelButtonManager
    private lateinit var wifiStateUpdater: WifiStateUpdater

    companion object {
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_INTENT = "EXTRA_RESULT_INTENT"
        const val navigationSettings = "com.xbh.navigation.settings"
        const val serviceChannelId = "fab_service_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        wifiStateUpdater = this

        pm = this.packageManager
        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager // инициализация WindowManager для кастомных настроек отображения.
        sharedPreferences = getSharedPreferences("app_package_names", Context.MODE_PRIVATE)
        packageNames = getPackageNamesFromSharedPreferences()
        val buttonPreferenceUtl = ButtonPreferenceUtil(this)
        val buttonAssignments = buttonPreferenceUtl.getButtonAssignments()

        // Регистрация сервиса для пяти касаний
        registerReceiverOnService()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Инициализация AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

//        createVirtualDisplay()

        // инфлейтим макет кнопки (floating button).
        floatingButtonLayout =
            LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)
        mainButton = floatingButtonLayout.findViewById(R.id.ellipse_outer)

        volumeSliderLayout = LayoutInflater.from(this).inflate(R.layout.volume_slider_layout, null)
        settingsPanelLayout =
            LayoutInflater.from(this).inflate(R.layout.settings_panel_layout, null)
        brightnessSliderLayout =
            LayoutInflater.from(this).inflate(R.layout.brightness_slider_layout, null)

        buttons = mutableListOf(
            floatingButtonLayout.findViewById(R.id.settings_button),
            floatingButtonLayout.findViewById(R.id.additional_settings_button),
            floatingButtonLayout.findViewById(R.id.back_button),
            floatingButtonLayout.findViewById(R.id.home_button),
            floatingButtonLayout.findViewById(R.id.show_all_running_apps_button),
//            floatingButtonLayout.findViewById(R.id.magnifier_button)
        )

        addVolumeSliderToLayout()

        projector = Projector(this)

        createAndAddMagnifierView()

        panelButtons = listOf(
            settingsPanelLayout.findViewById(R.id.wifi_panel_btn),
            settingsPanelLayout.findViewById(R.id.bluetooth_panel_btn),
            settingsPanelLayout.findViewById(R.id.volume_panel_btn),
            settingsPanelLayout.findViewById(R.id.browser_panel_btn),
            settingsPanelLayout.findViewById(R.id.volume_off_panel_btn),
            settingsPanelLayout.findViewById(R.id.projector_panel_btn)
//            settingsPanelLayout.findViewById(R.id.screenshot_panel_btn)
        )
        val wifiPanelButton = panelButtons[0]
        val bluetoothPanelButton = panelButtons[1]

        // Управление доп.кнопками
        bm = ButtonManager(
            context = this,
            packageManager = packageManager,
            buttons = buttons,
            settingsPanelController = this,
            buttonAssignments = buttonAssignments
        )

        // Управление кнопками вспомогательной панели
        pbm = PanelButtonManager(
            panelButtons = panelButtons,
            context = this,
            bluetoothAdapter = bluetoothAdapter,
            packageManager = packageManager,
            muteStateListener = this,
            projector = projector,
            stateUpdater = wifiStateUpdater,
            wifiPanelButtonView = wifiPanelButton,
            bluetoothPanelButtonView = bluetoothPanelButton,
            volumeControllerListener = this
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

        // Установим изначальное состояние кнопки звука при запуске сервиса
        initMuteState()

        // Создаём канал уведомления для Foreground Service
        createNotificationChannel(serviceChannelId)

        // Добавляем ползунок громкости
        addVolumeSliderToLayout()

        // Ставим слушатели на вспомогательные кнопки
        bm.setListenersForButtons()

        // Слушатели на кнопки панели
        pbm.setListenersForPanelButtons()

        // Применяем настройки WindowManager
        setupOverlayWindow()

        // Установка слушателя на основную кнопку
        onMoveMainButtonLogic()
    }

    private fun createAndAddMagnifierView() {
        magnifierViewLayout =
            LayoutInflater.from(this).inflate(R.layout.magnifier_view, null) as FrameLayout
        magnifierImageView = magnifierViewLayout.findViewById(R.id.magnifier_imageview)
        magnifierImageView.scaleType = ImageView.ScaleType.FIT_CENTER

        magnifierViewParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = 0
            this.y = 0
        }

        windowManager.addView(magnifierViewLayout, magnifierViewParams)

        //Делаем Magnifier View изначально невидимой
        magnifierViewLayout.apply {
            visibility = View.GONE
            isClickable = false
            isFocusable = false
        }
    }

    override fun updateBluetoothButtonState(isBluetoothEnabled: Boolean) {
        pbm.bluetoothPanelButton.updateButtonBackground(isBluetoothEnabled)
    }

    private fun addSettingsPanelToLayout() {
        panelParams = WindowManager.LayoutParams(
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
    private fun onMoveMainButtonLogic() {
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
//        val panelLayoutParams = settingsPanelLayout.layoutParams as WindowManager.LayoutParams
//        panelLayoutParams.x = params.x + offset
//        panelLayoutParams.y = params.y
        panelParams.x = params.x + offset
        panelParams.y = params.y
        windowManager.updateViewLayout(settingsPanelLayout, panelParams)
    }

    private fun registerReceiverOnService() {
        val filter = IntentFilter().apply {
            addAction(actionFivePoints)
            addAction(actionRecentTask)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        registerReceiver(receiver, filter)
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

    /* в разработке


    private fun setCollapsedParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            collapsedLayoutDimens,
            collapsedLayoutDimens,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }

     */

    private fun setExpandedParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            expandedLayoutDimens,
            expandedLayoutDimens,
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
        params = setExpandedParams()
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
            toggleButtonsVisibility(selectedButtons, mainButton)
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
                button.x = mainButton.x /* + mainButton.width / 2  - button.width / 2 */
                button.y = mainButton.y /* + mainButton.height / 2  - button.height / 2 */
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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun magnifierButtonHandler() {
        // Переключаем видимость magnifierView
        if (magnifierViewLayout.visibility == View.GONE) {
            magnifierViewLayout.visibility = View.VISIBLE
            magnifierViewLayout.isClickable = true
            magnifierViewLayout.isFocusable = true
        } else {
            magnifierViewLayout.visibility = View.GONE
            magnifierViewLayout.isClickable = false
            magnifierViewLayout.isFocusable = false
        }

        val magnifier =
            Magnifier.Builder(magnifierViewLayout).setSize(200, 200).setInitialZoom(2.0f).build()
        Log.d("TRACK_", "Создан $magnifier")

        magnifierViewLayout.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    // Показываем Magnifier
                    magnifier.show(event.rawX, event.rawY)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    // Прячем Magnifier
                    magnifier.dismiss()
                    true
                }

                else -> false
            }
        }
    }

    private fun updateVolumeSliderPosition() {
        volumeSliderParams.x = xMiddleScreenDot
        volumeSliderParams.y = yMiddleScreenDot + 150
    }

    override fun updateWifiButtonState(isWifiEnabled: Boolean) {
        pbm.wifiPanelButton.updateButtonBackground(isWifiEnabled)

    }

    private fun animateButton(button: View) {
        button.startAnimation(
            AnimationUtils.loadAnimation(
                this, R.anim.button_animation
            )
        )
    }

    /**
     * вызываем функцию `onDestroy`, когда сервис уничтожается.
     * Проходим по if и проверяем, всё ли уничтожилось.
     */
    override fun onDestroy() {
        Log.d("FloatingButtonService", "onDestroy() - начало")
        super.onDestroy()
        // Удаление основного layout
        if (::floatingButtonLayout.isInitialized && floatingButtonLayout.isAttachedToWindow) {
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
        // Удаление MagnifierView
        if (::magnifierViewLayout.isInitialized) {
            windowManager.removeView(magnifierViewLayout)
        }
        projector.dismiss()

        try {
            unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
//        applicationContext.stopService(Intent(this, FloatingButtonService.class::java))
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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Получение данных о захвате экрана
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_INTENT)
        Log.d(
            "FloatingButtonService",
            "onStartCommand: resultCode: $resultCode, resultData: $resultData"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                serviceChannelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)

            val notification =
                NotificationCompat.Builder(this, serviceChannelId).setContentTitle("FAB_Service")
                    .setContentText("Сервис запущен").setSmallIcon(R.drawable.ikar_fab_img).build()

            // Запуск сервиса в переднем плане с указанием типа FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            startForeground(
                1123124590, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        }

        if (resultCode != null && resultData != null && resultCode == Activity.RESULT_OK) {
            // Инициализация mediaProjection
            val mediaProjectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)

//            createVirtualDisplay()
        }

        return START_STICKY
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

//    private fun createVirtualDisplay() {
//        Log.d("TRACK_", "Заход в createVirtualDisplay")
//        val displayMetrics = resources.displayMetrics
//        val width = displayMetrics.widthPixels
//        val height = displayMetrics.heightPixels
//
//        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
//        surface = imageReader.surface
//
//        virtualDisplay = mediaProjection.createVirtualDisplay(
//            "FloatingButtonService",
//            width,
//            height,
//            displayMetrics.densityDpi,
//            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//            surface,
//            null,
//            null
//        )
//
//        // Установка слушателя на imageReader
//        val handler = Handler(Looper.getMainLooper())
//
//        imageReader.setOnImageAvailableListener({ reader ->
//            val image = reader.acquireLatestImage()
//            val planes = image.planes
//            val buffer = planes[0].buffer
//            val pixelStride = planes[0].pixelStride
//            val rowStride = planes[0].rowStride
//            val rowPadding = rowStride - pixelStride * image.width
//            val bitmapWidth = image.width + rowPadding / pixelStride
//
//            // Создадим Bitmap с размерами ImageView
//            val bitmap = Bitmap.createBitmap(
//                magnifierImageView.width,
//                magnifierImageView.height,
//                Bitmap.Config.ARGB_8888
//            )
//            Log.d("BitmapSize", "Bitmap Width: ${bitmap.width}, Height: ${bitmap.height}")
//
//            buffer.rewind() // Сброс позиции буфера
//            val data = ByteArray(buffer.capacity())
//            buffer.get(data) // Копирование данных из буфера в массив байтов
//
//            // Заполнение Bitmap
//            // Итерация по строкам и столбцам изображения
//            for (y in 0 until image.height) {
//                for (x in 0 until image.width) {
//                    val pixelIndex = y * rowStride + x * pixelStride
//                    val r = data[pixelIndex].toInt() and 0xFF
//                    val g = data[pixelIndex + 1].toInt() and 0xFF
//                    val b = data[pixelIndex + 2].toInt() and 0xFF
//                    val a = data[pixelIndex + 3].toInt() and 0xFF
//
//                    val color = Color.argb(a, r, g, b)
//                    bitmap.setPixel(x, y, color)
//                }
//            }
//
//            magnifierImageView.apply {
//                setImageBitmap(bitmap)
//            }
//            image?.close()
//        }, handler)
//    }

    private fun convertDpToPixel(dp: Int, context: Context) =
        (dp * context.resources.displayMetrics.density).toInt()

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

    override fun isMuted(): Boolean {
        return isMuted
    }

    override fun onMusic() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
        volumeSlider.value = currentVolume.toFloat()
        isMuted = false
        updateMuteButtonState()
    }

    override fun offMusic() {
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        volumeSlider.value = 0f
        isMuted = true
        updateMuteButtonState()
    }

    private fun addVolumeSliderToLayout() {
        volumeSlider = volumeSliderLayout.findViewById(R.id.volume_slider)
        volumeSliderButton = volumeSliderLayout.findViewById(R.id.on_off_mute_btn)
        volumeSliderGroup = volumeSliderLayout.findViewById(R.id.volume_slider_group)
        volumeSliderLayout.visibility = View.GONE
        isVolumeSliderShown = false

        volumeSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                val progress = value.toInt()
                Log.d("VerticalSlider", "Slider progress changed: $progress")
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_SHOW_UI
                )
                isMuted = progress == 0
                updateMuteButtonState()
            }
        }


        volumeSliderButton.setOnClickListener {
            Log.d("VolumeButton", "Button clicked")
            toggleMuteVolume()
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
        // Удаление volumeSliderLayout перед его добавлением, если он уже существует
        try {
            if (::volumeSliderLayout.isInitialized) {
                windowManager.removeViewImmediate(volumeSliderLayout)
            }
        } catch (e: IllegalArgumentException) {
            // Этот блок catch предотвращает краш, если volumeSliderLayout не был добавлен ранее
        }
        windowManager.addView(volumeSliderLayout, volumeSliderParams)

        volumeSliderLayout.visibility = View.GONE
        isVolumeSliderShown = false
    }


    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        private val actionFivePoints = "com.xbh.fivePoint"

        override fun onReceive(context: Context, intent: Intent) {
            Log.d("FloatingButtonService", "BroadcastReceiver - onReceive: ${intent.action}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, FloatingButtonService::class.java))
            } else {
                context.startService(Intent(context, FloatingButtonService::class.java))
            }
            when (intent.action) {
                actionFivePoints -> {
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
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            updateBluetoothButtonState(false)
                        }

                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            // Bluetooth is turning off;
                        }

                        BluetoothAdapter.STATE_ON -> {
                            updateBluetoothButtonState(true)
                        }

                        BluetoothAdapter.STATE_TURNING_ON -> {
                            // Bluetooth is turning on
                        }
                    }
                }

                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    when (intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN
                    )) {
                        WifiManager.WIFI_STATE_ENABLED -> updateWifiButtonState(true)
                        WifiManager.WIFI_STATE_DISABLED -> updateWifiButtonState(false)
                    }
                }
            }
        }
    }

    override fun showSettingsPanel(xOffset: Int, yOffset: Int) {
        if (!isSettingsPanelShown) {
            // Рассчитаем и установим новую позицию для панели настроек
            val settingsLayoutParams =
                settingsPanelLayout.layoutParams as WindowManager.LayoutParams
            settingsLayoutParams.apply {
                x = xTrackingDotsForPanel + xOffset
                y = yTrackingDotsForPanel + yOffset
            }
            windowManager.updateViewLayout(settingsPanelLayout, settingsLayoutParams)

            // Показываем панель и применяем анимацию
            val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation)
            settingsPanelLayout.visibility = View.VISIBLE
            settingsPanelLayout.startAnimation(fadeInAnim)
            isSettingsPanelShown = true
        } else {
            // Применяем анимацию исчезновения и скрываем панель
            val fadeOutAnim = AnimationUtils.loadAnimation(this, R.anim.fade_out_animation)
            settingsPanelLayout.startAnimation(fadeOutAnim)
            settingsPanelLayout.visibility = View.GONE
            isSettingsPanelShown = false
        }
    }

    override fun isSettingsPanelShown(): Boolean {
        return isSettingsPanelShown
    }

    // Проверка состояния звука при запуске сервиса
    fun initMuteState() {
        // Проверяем текущий уровень громкости
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        // Если равен 0, ставит флаг на ноль
        isMuted = currentVolume == 0
        updateMuteButtonState()
    }

    override fun showOrHideVolumeSlider() {
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

    override fun isVolumeSliderShown(): Boolean {
        return isVolumeSliderShown
    }
}