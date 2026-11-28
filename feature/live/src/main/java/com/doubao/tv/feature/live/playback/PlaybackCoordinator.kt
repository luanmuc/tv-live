package com.doubao.tv.feature.live.playback

/**
 * 直播播放失败后的自动换线策略。
 *
 * 只管「是否换、换到哪一条、还能换几次」，不持有 Channel/Compose 状态，
 * 方便单测，也避免把换线规则继续散落在 MainContentState 里。
 */
class PlaybackCoordinator(
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var attempts = 0
    private var lastSwitchAt = 0L

    /** 切换频道或播放成功后调用；只清空次数，不重置冷却时间。 */
    fun reset() {
        attempts = 0
    }

    fun onError(
        lineCount: Int,
        currentLineIdx: Int,
        isCatchup: Boolean,
        isLineAlive: (Int) -> Boolean,
    ): Decision {
        if (isCatchup || lineCount <= 1) return Decision.Ignored

        val timestamp = now()
        if (timestamp - lastSwitchAt < cooldownMs) return Decision.Ignored

        if (attempts >= lineCount - 1) {
            attempts = 0
            return Decision.Exhausted
        }

        val safeIdx = ((currentLineIdx % lineCount) + lineCount) % lineCount
        val nextIdx = (1 until lineCount)
            .map { (safeIdx + it) % lineCount }
            .firstOrNull(isLineAlive)
            ?: ((safeIdx + 1) % lineCount)

        lastSwitchAt = timestamp
        attempts += 1
        return Decision.Switch(
            lineIndex = nextIdx,
            attempt = attempts,
            total = lineCount,
        )
    }

    sealed interface Decision {
        data object Ignored : Decision
        data object Exhausted : Decision
        data class Switch(
            val lineIndex: Int,
            val attempt: Int,
            val total: Int,
        ) : Decision
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS = 1000L
    }
}
