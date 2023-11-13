package ru.ikar.floatingbutton_ikar.accessability

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class GlobalActionService : AccessibilityService() {
    companion object {
        var instance: GlobalActionService? = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for this purpose.
    }

    override fun onInterrupt() {
        // Not used for this purpose.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    fun performBackAction() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }
}