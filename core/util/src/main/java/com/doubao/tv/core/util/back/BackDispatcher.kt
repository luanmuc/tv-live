package com.doubao.tv.core.util.back

/**
 * 统一的返回键分发栈。
 *
 * 后注册的处理器优先；处理器返回 false 时继续向下冒泡。
 * push 返回的 lambda 用于在 Composable 离开时注销。
 */
class BackDispatcher {
    private val handlers = mutableListOf<() -> Boolean>()

    fun push(handler: () -> Boolean): () -> Unit {
        handlers += handler
        return { handlers.remove(handler) }
    }

    fun dispatch(): Boolean {
        for (index in handlers.indices.reversed()) {
            if (handlers[index]()) return true
        }
        return false
    }

    fun clear() {
        handlers.clear()
    }
}
