package com.soywiz.std

import kotlin.native.*
import kotlin.native.concurrent.*

actual typealias ThreadLocal = kotlin.native.ThreadLocal

actual val isNative: Boolean = true
actual val isJs: Boolean = false
actual val isJvm: Boolean = false

//@UseExperimental(ExperimentalContracts::class)
actual inline fun <R> synchronized2(lock: Any, block: () -> R): R {
	//contract {
	//	callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	//}
	return block()
}
