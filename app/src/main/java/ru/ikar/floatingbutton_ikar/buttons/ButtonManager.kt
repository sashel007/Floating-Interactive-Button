package ru.ikar.floatingbutton_ikar.buttons

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import ru.ikar.floatingbutton_ikar.R
import ru.ikar.floatingbutton_ikar.service.SettingsPanelController
import ru.ikar.floatingbutton_ikar.service.buttons.ButtonAnimator
import ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons.AdditionalSettingsButton
import ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons.BackButton
import ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons.HomeButton
import ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons.SettingsButton
import ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons.ShowAllRunningAppsButton

class ButtonManager(
    private val context: Context,
    private val floatingButtonLayout: View,
    private val settingsPanelLayout: View,
    private val packageManager: PackageManager,
    private var buttons: MutableList<View>,
    private var isSettingsPanelShown: Boolean,
    private var isExpanded: Boolean,
    private var windowManager: WindowManager,
    private var xTrackingDotsForPanel: Int,
    private var yTrackingDotsForPanel: Int,
    private var mainButton: View,
    private val settingsPanelController: SettingsPanelController
) {
    private val pm: PackageManager = context.packageManager
    private val radiusFirst = 100 // Радиус расположения кнопок вокруг основной кнопки
    private val animator = ButtonAnimator(context)

//    fun addButton(mainButtonId: Int, buttonIds: List<Int>, packageNames: List<String>) {
//        mainButton = floatingButtonLayout.findViewById(mainButtonId)
//        buttonIds.forEach { id ->
//            buttons.add(floatingButtonLayout.findViewById(id))
//        }
//        addAppIconButtons(packageNames)
//    }
//
//    private fun addAppIconButtons(packageNames: List<String>) {
//        for (packageName in packageNames) {
//            try {
//                val appIcon = pm.getApplicationIcon(packageName)
//                val newButton = ImageButton(context).apply {
//                    setImageDrawable(appIcon)
//                    tag = packageName
//                    scaleType = ImageView.ScaleType.CENTER_CROP
//                    background = null
//                    val layoutParams = FrameLayout.LayoutParams(130, 130)
//                    this.layoutParams = layoutParams
//                }
//
//                floatingButtonLayout.addView(newButton)
//                buttons.add(newButton)
//                newButton.visibility = View.INVISIBLE
//            } catch (e: PackageManager.NameNotFoundException) {
//                e.printStackTrace()
//            }
//        }
//    }

//    fun toggleButtonsVisibility() {
//        if (isExpanded) {
//            buttons.forEach { button ->
//                button.animate().x(mainButton.x + mainButton.width / 2 - button.width / 2)
//                    .y(mainButton.y + mainButton.height / 2 - button.height / 2).scaleX(0f)
//                    .scaleY(0f).alpha(0f).setDuration(300).withEndAction {
//                        button.visibility = View.INVISIBLE
//                    }.start()
//            }
//        } else {
//            buttons.forEachIndexed { index, button ->
//                button.isVisible = true // Сделать кнопку видимой перед анимацией
//                val (finalX, finalY) = calculateFinalPosition(index, buttons.size)
//                button.x = mainButton.x
//                button.y = mainButton.y
//                button.alpha = 1f
//                button.scaleX = 1f
//                button.scaleY = 1f
//                button.animate().x(finalX).y(finalY).setDuration(300).start()
//            }
//        }
//        isExpanded = !isExpanded
//    }
//
//    private fun calculateFinalPosition(index: Int, totalButtons: Int): Pair<Float, Float> {
//        val angleIncrement = 360.0 / totalButtons
//        val angle =
//            index * angleIncrement * (Math.PI / 180) // Конвертирует угол из градусов в радианы
//        val finalX =
//            (radiusFirst * kotlin.math.cos(angle) + mainButton.x + mainButton.width / 2).toFloat() - 65 // Половина размера кнопки для центрирования
//        val finalY =
//            (radiusFirst * kotlin.math.sin(angle) + mainButton.y + mainButton.height / 2).toFloat() - 65
//        return Pair(finalX, finalY)
//    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun setListenersForButtons(context: Context) {
        val settingsButton = SettingsButton(context)
        val homeButton = HomeButton(context)
        val backButton = BackButton(context)
        val showAllRunningAppsButton = ShowAllRunningAppsButton(context)
        val additionalSettingsButton = AdditionalSettingsButton(
            context,
            settingsPanelController
        )

        buttons.forEach { button ->
            button.setOnClickListener { it ->
                when (button.id) {
                    R.id.settings_button -> button.setOnClickListener {
                        settingsButton.apply {
                            animateButton(it)
                            onClick()
                        }
                    }

                    R.id.home_button -> {
                        button.setOnClickListener {
                            homeButton.apply {
                                animateButton(it)
                                onClick()
                            }
                        }
                    }

                    R.id.back_button -> {
                        button.setOnClickListener {
                            backButton.apply {
                                animateButton(it)
                                onClick()
                            }
                        }
                    }

                    R.id.show_all_running_apps_button -> {
                        button.setOnClickListener {
                            showAllRunningAppsButton.apply {
                                animateButton(it)
                                onClick()
                            }
                        }
                    }

                    R.id.additional_settings_button -> {
                        button.setOnClickListener {
                            additionalSettingsButton.apply {
                                animateButton(it)
                                onClick()
                            }
                        }
                    }

                    else -> {
                        // Если кнопка не является одной из базовых, считаем ее кнопкой-иконкой приложения
                        val launchIntent =
                            packageManager.getLaunchIntentForPackage(button.tag as? String ?: "")
                        launchIntent?.let { startActivity(context, it, null) }
                        animator.animateButton(it)
                    }
                }
            }
        }
    }
}