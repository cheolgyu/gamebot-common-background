package com.highserpot.background.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

lateinit var touchService: TouchService

class TouchService : AccessibilityService() {
    override fun onServiceConnected() {
        touchService = this
        super.onServiceConnected()
    }

    fun click(act_info: BackgroundService.ActionInfo) {
        val clickPath = Path()


        if (act_info.action_type == "click") {
            clickPath.moveTo(act_info.x, act_info.y)
        }else if (act_info.action_type == "swipe"){
            clickPath.moveTo(act_info.x / 2, act_info.y / 2)
            clickPath.lineTo(act_info.x , act_info.y / 2)
        }

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(StrokeDescription(clickPath, 0, 100))
        dispatchGesture(gestureBuilder.build(), null, null)
    }


    override fun onInterrupt() {
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
    }


}
