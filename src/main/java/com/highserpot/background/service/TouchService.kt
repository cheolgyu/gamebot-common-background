package com.highserpot.background.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

var touchService: TouchService? = null

class TouchService : AccessibilityService() {
    override fun onServiceConnected() {
        touchService = this
        super.onServiceConnected()
    }

    fun click(x: Float, y: Float) {
        val clickPath = Path()
        clickPath.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(StrokeDescription(clickPath, 0, 1000))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    override fun onInterrupt() {
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
    }


}
