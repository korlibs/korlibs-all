package com.soywiz.dynarek

actual annotation class JvmField
actual typealias JsName = kotlin.js.JsName

actual fun DFunction.generateDynarekResult(name: String, count: Int): DynarekResult = DynarekResult(byteArrayOf(), _generateDynarek(name, count, this))
