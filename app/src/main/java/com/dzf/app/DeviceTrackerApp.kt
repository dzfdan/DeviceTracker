package com.dzf.app

import android.app.Application
import com.amap.api.maps.MapsInitializer
import com.dzf.app.util.AppLifecycleState

class DeviceTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        AppLifecycleState.init()
    }
}
