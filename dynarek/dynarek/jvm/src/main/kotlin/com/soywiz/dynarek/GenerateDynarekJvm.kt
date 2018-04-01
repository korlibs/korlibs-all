@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.soywiz.dynarek

import kotlin.jvm.functions.Function0
import kotlin.jvm.functions.Function1
import kotlin.jvm.functions.Function2

actual typealias JvmField = kotlin.jvm.JvmField
actual annotation class JsName(actual val name: String)

actual fun <TRet : Any> DFunction0<TRet>.generateDynarek(): () -> TRet = _generateDynarek(this, Function0::class.java) as (() -> TRet)
actual fun <TRet : Any, T0 : Any> DFunction1<TRet, T0>.generateDynarek(): (T0) -> TRet = _generateDynarek(this, Function1::class.java) as ((T0) -> TRet)
actual fun <TRet : Any, T0 : Any, T1 : Any> DFunction2<TRet, T0, T1>.generateDynarek(): (T0, T1) -> TRet = _generateDynarek(this, Function2::class.java) as ((T0, T1) -> TRet)
