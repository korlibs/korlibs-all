@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.soywiz.dynarek

actual annotation class JvmField
actual annotation class JsName(actual val name: String)

actual fun DFunction.generateDynarekResult(name: String, count: Int): DynarekResult = generateDynarekResultInterpreted(name, count)
