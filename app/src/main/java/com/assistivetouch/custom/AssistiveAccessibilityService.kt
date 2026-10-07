package com.assistivetouch.custom

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AssistiveAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AssistiveAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun performLock() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    fun performScreenshot() {
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }
}