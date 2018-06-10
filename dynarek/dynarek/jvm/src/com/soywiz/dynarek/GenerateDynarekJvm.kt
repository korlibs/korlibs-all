@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.soywiz.dynarek

import kotlin.jvm.functions.*

actual typealias JvmField = kotlin.jvm.JvmField

actual annotation class JsName(actual val name: String)

actual fun DFunction.generateDynarekResult(name: String, count: Int): DynarekResult = _generateDynarek(this, when (count) {
	0 -> Function0::class.java
	1 -> Function1::class.java
	2 -> Function2::class.java
	3 -> Function3::class.java
	else -> error("Unsuported arity $count")
})
