package org.fmdx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TabStateControllerTest {

    @Test
    fun selectTabClampsWithinRange() {
        val controller = TabStateController(pageCount = 3)

        controller.selectTab(2)
        assertEquals(2, controller.selectedTab)

        controller.selectTab(10)
        assertEquals(2, controller.selectedTab)

        controller.selectTab(-5)
        assertEquals(0, controller.selectedTab)
    }

    @Test
    fun pagerChangeUpdatesSelectedTab() {
        val controller = TabStateController(pageCount = 4)

        controller.selectTab(1)
        controller.onPagerPageChanged(3)

        assertEquals(3, controller.selectedTab)
    }

    @Test
    fun resetReturnsToFirstTab() {
        val controller = TabStateController(pageCount = 5)

        controller.selectTab(4)
        controller.reset()

        assertEquals(0, controller.selectedTab)
    }

    @Test
    fun saverRestoresPreviousSelection() {
        val restored = TabStateController.saver(pageCount = 6).restore(4)

        assertNotNull(restored)
        assertEquals(4, restored?.selectedTab)
    }
}
