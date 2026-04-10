package com.dzf.app

import android.app.Application
import com.dzf.app.util.AppLifecycleState

class DeviceTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLifecycleState.init()
    }
}
