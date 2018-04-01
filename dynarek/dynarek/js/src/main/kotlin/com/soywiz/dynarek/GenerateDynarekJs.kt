package com.soywiz.dynarek

actual annotation class JvmField
actual typealias JsName = kotlin.js.JsName
actual fun <TRet : Any> DFunction0<TRet>.generateDynarek(): () -> TRet = _generateDynarek(0, this)
actual fun <TRet : Any, T0 : Any> DFunction1<TRet, T0>.generateDynarek(): (T0) -> TRet = _generateDynarek(1, this)
actual fun <TRet : Any, T0 : Any, T1 : Any> DFunction2<TRet, T0, T1>.generateDynarek(): (T0, T1) -> TRet = _generateDynarek(2, this)
