package com.dzf.app.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.concurrent.atomic.AtomicBoolean

object AppLifecycleState : DefaultLifecycleObserver {
    private val inForeground = AtomicBoolean(false)

    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun isInForeground(): Boolean = inForeground.get()

    override fun onStart(owner: LifecycleOwner) {
        inForeground.set(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        inForeground.set(false)
    }
}
