package com.soywiz.dynarek

expect annotation class JvmField()
expect annotation class JsName(val name: String)

expect fun DFunction.generateDynarekResult(count: Int): DynarekResult

fun <TRet : Any> DFunction0<TRet>.generateDynarek(): () -> TRet =
	generateDynarekResult(0).func as () -> TRet

fun <TRet : Any, T0 : Any> DFunction1<TRet, T0>.generateDynarek(): (T0) -> TRet =
	generateDynarekResult(1).func as (T0) -> TRet

fun <TRet : Any, T0 : Any, T1 : Any> DFunction2<TRet, T0, T1>.generateDynarek(): (T0, T1) -> TRet =
	generateDynarekResult(2).func as (T0, T1) -> TRet

fun <TRet : Any, T0 : Any, T1 : Any, T2 : Any> DFunction3<TRet, T0, T1, T2>.generateDynarek(): (T0, T1, T2) -> TRet =
	generateDynarekResult(3).func as (T0, T1, T2) -> TRet

class InvalidCodeGenerated(val baseMessage: String, val data: ByteArray, val exception: Throwable) :
	RuntimeException("InvalidCodeGenerated: $baseMessage :: $exception")

class DynarekResult(val data: ByteArray, val func: Any)
