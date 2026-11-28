package com.doubao.tv.core.util.back

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackDispatcherTest {
    @Test
    fun `empty dispatcher does not consume`() {
        assertFalse(BackDispatcher().dispatch())
    }

    @Test
    fun `last registered handler runs first`() {
        val dispatcher = BackDispatcher()
        val calls = mutableListOf<String>()
        dispatcher.push { calls += "first"; true }
        dispatcher.push { calls += "second"; true }

        assertTrue(dispatcher.dispatch())
        assertEquals(listOf("second"), calls)
    }

    @Test
    fun `handler returning false bubbles to previous`() {
        val dispatcher = BackDispatcher()
        val calls = mutableListOf<String>()
        dispatcher.push { calls += "first"; true }
        dispatcher.push { calls += "second"; false }

        assertTrue(dispatcher.dispatch())
        assertEquals(listOf("second", "first"), calls)
    }

    @Test
    fun `remove unregisters handler`() {
        val dispatcher = BackDispatcher()
        val remove = dispatcher.push { true }
        remove()

        assertFalse(dispatcher.dispatch())
    }
}
