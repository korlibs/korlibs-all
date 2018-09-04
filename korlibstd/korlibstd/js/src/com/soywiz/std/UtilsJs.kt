package com.soywiz.std

actual annotation class ThreadLocal actual constructor()

actual val isNative: Boolean = false
actual val isJs: Boolean = true
actual val isJvm: Boolean = false

//@UseExperimental(ExperimentalContracts::class)
actual inline fun <R> synchronized2(lock: Any, block: () -> R): R {
	//contract {
	//	callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	//}
	return block()
}
