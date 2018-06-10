package com.soywiz.dynarek

expect annotation class JvmField()
expect annotation class JsName(val name: String)

expect fun DFunction.generateDynarekResult(name: String, count: Int): DynarekResult

fun <TRet : Any> DFunction0<TRet>.generateDynarek(name: String = "myfunc"): () -> TRet =
	generateDynarekResult(name, 0).func as () -> TRet

fun <TRet : Any, T0 : Any> DFunction1<TRet, T0>.generateDynarek(name: String = "myfunc"): (T0) -> TRet =
	generateDynarekResult(name, 1).func as (T0) -> TRet

fun <TRet : Any, T0 : Any, T1 : Any> DFunction2<TRet, T0, T1>.generateDynarek(name: String = "myfunc"): (T0, T1) -> TRet =
	generateDynarekResult(name, 2).func as (T0, T1) -> TRet

fun <TRet : Any, T0 : Any, T1 : Any, T2 : Any> DFunction3<TRet, T0, T1, T2>.generateDynarek(name: String = "myfunc"): (T0, T1, T2) -> TRet =
	generateDynarekResult(name, 3).func as (T0, T1, T2) -> TRet

class InvalidCodeGenerated(val baseMessage: String, val data: ByteArray, val exception: Throwable) :
	RuntimeException("InvalidCodeGenerated: $baseMessage :: $exception")

class DynarekResult(val data: ByteArray, val func: Any)
