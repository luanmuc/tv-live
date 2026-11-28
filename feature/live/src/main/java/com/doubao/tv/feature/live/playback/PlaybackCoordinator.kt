package com.doubao.tv.feature.live.playback

/**
 * 直播播放失败/卡顿后的换线策略。
 *
 * 只管「是否换、换到哪一条、还能换几次」，不持有 Channel/Compose 状态，
 * 方便单测，也避免把换线规则继续散落在 MainContentState 里。
 *
 * 选线优先看 [lineScore]（当前用线路探测延迟），同分再按频道内顺序。
 */
class PlaybackCoordinator(
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val stallCooldownMs: Long = DEFAULT_STALL_COOLDOWN_MS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var attempts = 0
    private var lastSwitchAt = now() - cooldownMs
    private var stallAttempts = 0
    private var lastStallSwitchAt = now() - stallCooldownMs

    /** 切换频道或播放成功后调用；只清空次数，不重置冷却时间。 */
    fun reset() {
        attempts = 0
        stallAttempts = 0
    }

    fun onError(
        lineCount: Int,
        currentLineIdx: Int,
        isCatchup: Boolean,
        lineScore: (Int) -> Long = { Long.MAX_VALUE },
        isLineAlive: (Int) -> Boolean,
    ): Decision {
        if (isCatchup || lineCount <= 1) return Decision.Ignored

        val timestamp = now()
        if (timestamp - lastSwitchAt < cooldownMs) return Decision.Ignored

        if (attempts >= lineCount - 1) {
            attempts = 0
            return Decision.Exhausted
        }

        val nextIdx = chooseLine(lineCount, currentLineIdx, isLineAlive, lineScore)
        lastSwitchAt = timestamp
        attempts += 1
        return Decision.Switch(
            lineIndex = nextIdx,
            attempt = attempts,
            total = lineCount,
        )
    }

    /**
     * 播放中连续缓冲时换线。
     *
     * 比错误换线更保守：每轮最多 2 次、两次之间至少 [stallCooldownMs]，
     * 避免在源站整体抖动时把整组线路快速轮一遍。
     */
    fun onStall(
        lineCount: Int,
        currentLineIdx: Int,
        isCatchup: Boolean,
        lineScore: (Int) -> Long = { Long.MAX_VALUE },
        isLineAlive: (Int) -> Boolean,
    ): Decision {
        if (isCatchup || lineCount <= 1) return Decision.Ignored

        val timestamp = now()
        if (timestamp - lastStallSwitchAt < stallCooldownMs) return Decision.Ignored

        if (stallAttempts >= MAX_STALL_ATTEMPTS) return Decision.Exhausted

        val nextIdx = chooseLine(lineCount, currentLineIdx, isLineAlive, lineScore)
        lastStallSwitchAt = timestamp
        stallAttempts += 1
        return Decision.Switch(
            lineIndex = nextIdx,
            attempt = stallAttempts,
            total = lineCount,
        )
    }

    /** 从当前线路之后找一条活线；都未知/失效时按原顺序兜底。 */
    private fun chooseLine(
        lineCount: Int,
        currentLineIdx: Int,
        isLineAlive: (Int) -> Boolean,
        lineScore: (Int) -> Long,
    ): Int {
        val safeIdx = ((currentLineIdx % lineCount) + lineCount) % lineCount
        val candidates = (1 until lineCount).map { (safeIdx + it) % lineCount }
        val alive = candidates.filter(isLineAlive)
        val pool = alive.ifEmpty { candidates }
        return pool.minByOrNull { lineScore(it) } ?: safeIdx
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
        const val DEFAULT_STALL_COOLDOWN_MS = 12_000L
        const val MAX_STALL_ATTEMPTS = 2
    }
}
