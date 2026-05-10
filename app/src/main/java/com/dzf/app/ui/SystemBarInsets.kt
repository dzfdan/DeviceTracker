package com.dzf.app.ui

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

data class ViewPadding(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

fun applyInsets(
    initial: ViewPadding,
    insets: ViewPadding,
    applyLeft: Boolean = false,
    applyTop: Boolean = false,
    applyRight: Boolean = false,
    applyBottom: Boolean = false
): ViewPadding {
    return ViewPadding(
        left = initial.left + if (applyLeft) insets.left else 0,
        top = initial.top + if (applyTop) insets.top else 0,
        right = initial.right + if (applyRight) insets.right else 0,
        bottom = initial.bottom + if (applyBottom) insets.bottom else 0
    )
}

fun View.applySystemBarInsets(
    applyLeft: Boolean = false,
    applyTop: Boolean = false,
    applyRight: Boolean = false,
    applyBottom: Boolean = false
) {
    val initial = ViewPadding(
        left = paddingLeft,
        top = paddingTop,
        right = paddingRight,
        bottom = paddingBottom
    )

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val systemBars = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val resolved = applyInsets(
            initial = initial,
            insets = systemBars.toViewPadding(),
            applyLeft = applyLeft,
            applyTop = applyTop,
            applyRight = applyRight,
            applyBottom = applyBottom
        )

        view.setPadding(resolved.left, resolved.top, resolved.right, resolved.bottom)
        windowInsets
    }

    ViewCompat.requestApplyInsets(this)
}

private fun Insets.toViewPadding() = ViewPadding(left = left, top = top, right = right, bottom = bottom)
