package com.sog.gpsuser

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class gpsservice : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            try {

            }catch (ex:Exception){}
        }
    }

    override fun onInterrupt() {

    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            var xIntent = Intent(applicationContext, MyForegroundService::class.java)
            startService(xIntent)
        }catch (ex:Exception){
        }
    }

}