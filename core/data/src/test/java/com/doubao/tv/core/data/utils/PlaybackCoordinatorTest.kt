package com.doubao.tv.core.data.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCoordinatorTest {
    private var clock = 10_000L
    private val coordinator = PlaybackCoordinator(now = { clock })

    @Test
    fun `catchup playback never switches`() {
        val decision = coordinator.onError(
            lineCount = 3,
            currentLineIdx = 0,
            isCatchup = true,
            isLineAlive = { true },
        )

        assertTrue(decision is PlaybackCoordinator.Decision.Ignored)
    }

    @Test
    fun `single line never switches`() {
        val decision = coordinator.onError(
            lineCount = 1,
            currentLineIdx = 0,
            isCatchup = false,
            isLineAlive = { true },
        )

        assertTrue(decision is PlaybackCoordinator.Decision.Ignored)
    }

    @Test
    fun `picks next alive line and skips current`() {
        val decision = coordinator.onError(
            lineCount = 3,
            currentLineIdx = 0,
            isCatchup = false,
            isLineAlive = { it == 2 },
        )

        assertTrue(decision is PlaybackCoordinator.Decision.Switch)
        assertEquals(2, (decision as PlaybackCoordinator.Decision.Switch).lineIndex)
    }

    @Test
    fun `cooldown suppresses rapid errors`() {
        coordinator.onError(3, 0, false) { true }

        val suppressed = coordinator.onError(3, 1, false) { true }
        assertTrue(suppressed is PlaybackCoordinator.Decision.Ignored)

        clock += PlaybackCoordinator.DEFAULT_COOLDOWN_MS
        val allowed = coordinator.onError(3, 1, false) { true }
        assertTrue(allowed is PlaybackCoordinator.Decision.Switch)
    }

    @Test
    fun `exhausts after all other lines were tried`() {
        assertEquals(1, coordinator.onError(3, 0, false) { true }.lineIndexOrNull())
        clock += PlaybackCoordinator.DEFAULT_COOLDOWN_MS
        assertEquals(2, coordinator.onError(3, 1, false) { true }.lineIndexOrNull())
        clock += PlaybackCoordinator.DEFAULT_COOLDOWN_MS

        val exhausted = coordinator.onError(3, 2, false) { true }
        assertTrue(exhausted is PlaybackCoordinator.Decision.Exhausted)
    }

    @Test
    fun `reset allows a new round after success`() {
        coordinator.onError(3, 0, false) { true }
        clock += PlaybackCoordinator.DEFAULT_COOLDOWN_MS
        coordinator.onError(3, 1, false) { true }
        coordinator.reset()
        clock += PlaybackCoordinator.DEFAULT_COOLDOWN_MS

        val decision = coordinator.onError(3, 2, false) { true }
        assertTrue(decision is PlaybackCoordinator.Decision.Switch)
    }

    @Test
    fun `falls back to next line when no line is known alive`() {
        val decision = coordinator.onError(3, 1, false) { false }

        assertTrue(decision is PlaybackCoordinator.Decision.Switch)
        assertEquals(2, (decision as PlaybackCoordinator.Decision.Switch).lineIndex)
    }

    private fun PlaybackCoordinator.Decision.lineIndexOrNull(): Int? =
        (this as? PlaybackCoordinator.Decision.Switch)?.lineIndex
}
