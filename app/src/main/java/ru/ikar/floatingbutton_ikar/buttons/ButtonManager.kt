package ru.ikar.floatingbutton_ikar.buttons

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat.startActivity
import ru.ikar.floatingbutton_ikar.service.SettingsPanelController
import ru.ikar.floatingbutton_ikar.service.buttons.ButtonAnimator
import ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons.AdditionalSettingsButton
import ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons.BackButton
import ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons.HomeButton
import ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons.SettingsButton
import ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons.ShowAllRunningAppsButton

class ButtonManager(
    private val context: Context,
    private val packageManager: PackageManager,
    private var buttons: MutableList<View>,
    private val settingsPanelController: SettingsPanelController,
    private val buttonAssignments: Map<String, String>
) {
    private val animator = ButtonAnimator(context)

    init {
        Log.d("ButtonManager", "Button assignments: $buttonAssignments")
    }

    fun setListenersForButtons() {
        buttons.forEach { button ->
            button.setOnClickListener {
                val resourceName = button.resources.getResourceEntryName(button.id)
                val action = buttonAssignments[resourceName]
                Log.d("ButtonManager", "Button: $resourceName, Action: $action")

                performButtonAction(action, button)
            }
        }
    }

    private fun performButtonAction(action: String?, button: View) {
        Log.d("ButtonManager", "Performing action: $action for button: ${button.resources.getResourceEntryName(button.id)}")
        when (action) {
            "settings_value" -> {
                SettingsButton(context).apply {
                    animateButton(button)
                    onClick()
                }
            }

            "home_value" -> {
                HomeButton(context).apply {
                    animateButton(button)
                    onClick()
                }
            }

            "back_value" -> {
                BackButton(context).apply {
                    animateButton(button)
                    onClick()
                }
            }

            "recentapps_value" -> {
                ShowAllRunningAppsButton(context).apply {
                    animateButton(button)
                    onClick()
                }
            }

            "additionalsettings_value" -> {
                AdditionalSettingsButton(context, settingsPanelController).apply {
                    animateButton(button)
                    onClick()
                }
            }

            else -> {
                // Открываем приложение, если action соответствует пакетному имени приложения
                packageManager.getLaunchIntentForPackage(action ?: "")?.let {
                    startActivity(context, it, null)
                }
                animator.animateButton(button)
            }
        }

    }

//    @RequiresApi(Build.VERSION_CODES.P)
//    fun setListenersForButtons(context: Context) {
//        val settingsButton = SettingsButton(context)
//        val homeButton = HomeButton(context)
//        val backButton = BackButton(context)
//        val showAllRunningAppsButton = ShowAllRunningAppsButton(context)
//        val additionalSettingsButton = AdditionalSettingsButton(
//            context,
//            settingsPanelController
//        )
//
//        buttons.forEach { button ->
//            button.setOnClickListener { it ->
//                when (button.id) {
//                    R.id.settings_button -> button.setOnClickListener {
//                        settingsButton.apply {
//                            animateButton(it)
//                            onClick()
//                        }
//                    }
//
//                    R.id.home_button -> {
//                        button.setOnClickListener {
//                            homeButton.apply {
//                                animateButton(it)
//                                onClick()
//                            }
//                        }
//                    }
//
//                    R.id.back_button -> {
//                        button.setOnClickListener {
//                            backButton.apply {
//                                animateButton(it)
//                                onClick()
//                            }
//                        }
//                    }
//
//                    R.id.show_all_running_apps_button -> {
//                        button.setOnClickListener {
//                            showAllRunningAppsButton.apply {
//                                animateButton(it)
//                                onClick()
//                            }
//                        }
//                    }
//
//                    R.id.additional_settings_button -> {
//                        button.setOnClickListener {
//                            additionalSettingsButton.apply {
//                                animateButton(it)
//                                onClick()
//                            }
//                        }
//                    }
//
//                    else -> {
//                        // Если кнопка не является одной из базовых, считаем ее кнопкой-иконкой приложения
//                        val launchIntent =
//                            packageManager.getLaunchIntentForPackage(button.tag as? String ?: "")
//                        launchIntent?.let { startActivity(context, it, null) }
//                        animator.animateButton(it)
//                    }
//                }
//            }
//        }
//    }
}