package com.dzf.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemBarInsetsTest {

    @Test
    fun applyInsets_addsOnlyRequestedEdgesToInitialPadding() {
        val resolved = applyInsets(
            initial = ViewPadding(left = 24, top = 32, right = 24, bottom = 16),
            insets = ViewPadding(left = 6, top = 18, right = 8, bottom = 20),
            applyLeft = true,
            applyTop = true,
            applyRight = true
        )

        assertEquals(ViewPadding(left = 30, top = 50, right = 32, bottom = 16), resolved)
    }

    @Test
    fun applyInsets_leavesUnrequestedEdgesUntouched() {
        val resolved = applyInsets(
            initial = ViewPadding(left = 10, top = 12, right = 14, bottom = 18),
            insets = ViewPadding(left = 4, top = 5, right = 6, bottom = 7),
            applyBottom = true
        )

        assertEquals(ViewPadding(left = 10, top = 12, right = 14, bottom = 25), resolved)
    }
}
