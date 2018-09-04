@file:Suppress("USELESS_CAST")

package com.soywiz.std

actual annotation class ThreadLocal actual constructor()

actual val isNative: Boolean = false
actual val isJs: Boolean = false
actual val isJvm: Boolean = true


//@UseExperimental(ExperimentalContracts::class)
actual inline fun <R> synchronized2(lock: Any, block: () -> R): R = synchronized(lock) { block() }
