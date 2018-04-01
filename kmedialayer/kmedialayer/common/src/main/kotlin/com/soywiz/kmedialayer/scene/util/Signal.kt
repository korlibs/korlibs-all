package com.soywiz.kmedialayer.scene.util

import com.soywiz.kmedialayer.*

class Signal<T> {
    private val handlers = arrayListOf<(T) -> Unit>()

    companion object {
        private val arrayPool = Pool<ArrayList<(Any) -> Unit>>({ clear() }, { arrayListOf() })
    }

    operator fun invoke(handler: (T) -> Unit): (T) -> Unit {
        synchronized(handlers) { handlers += handler }
        return handler
    }

    operator fun plusAssign(handler: (T) -> Unit) = synchronized(handlers) {
        handlers += handler
    }

    operator fun minusAssign(handler: (T) -> Unit) = synchronized(handlers) {
        handlers -= handler
    }

    @Suppress("UNCHECKED_CAST")
    operator fun invoke(value: T) {
        arrayPool.use { temp ->
            synchronized(handlers) {
                temp.addAll(handlers as ArrayList<(Any) -> Unit>)
            }
            for (handler in temp) {
                (handler as (T) -> Unit)(value)
            }
        }
    }
}

suspend fun <T> Signal<T>.awaitOne(): T = suspendCoroutineCancellable { c, cancel ->
    var handler: ((T) -> Unit)? = null
    var done = false
    cancel {
        if (!done) {
            done = true
            if (handler != null) this@awaitOne -= handler!!
            c.resumeWithException(it)
        }
    }
    handler = this {
        if (!done) {
            done = true
            if (handler != null) this@awaitOne -= handler!!
            c.resume(it)
        }
    }
}