package org.fmdx.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {

    @Test
    fun clampTabIndex_returnsZeroWhenTabCountIsZero() {
        val result = clampTabIndex(currentPage = 2, tabCount = 0)

        assertEquals(0, result)
    }

    @Test
    fun clampTabIndex_clampsToLastIndexWhenCurrentPageExceedsRange() {
        val result = clampTabIndex(currentPage = 3, tabCount = 1)

        assertEquals(0, result)
    }

    @Test
    fun clampTabIndex_clampsNegativeIndicesToZero() {
        val result = clampTabIndex(currentPage = -2, tabCount = 4)

        assertEquals(0, result)
    }

    @Test
    fun clampTabIndex_returnsCurrentPageWhenInBounds() {
        val result = clampTabIndex(currentPage = 2, tabCount = 5)

        assertEquals(2, result)
    }
}
