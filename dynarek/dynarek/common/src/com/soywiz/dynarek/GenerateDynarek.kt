package com.soywiz.dynarek

expect annotation class JvmField()
expect annotation class JsName(val name: String)

expect fun DFunction.generateDynarekResult(name: String, count: Int): DynarekResult

fun <TRet : Any> DFunction0<TRet>.generateDynarek(name: String = "myfunc", interpreted: Boolean = false): () -> TRet =
	generateDynarekResultEx(name, 0, interpreted).func as () -> TRet

fun <TRet : Any, T0 : Any> DFunction1<TRet, T0>.generateDynarek(name: String = "myfunc", interpreted: Boolean = false): (T0) -> TRet =
	generateDynarekResultEx(name, 1, interpreted).func as (T0) -> TRet

fun <TRet : Any, T0 : Any, T1 : Any> DFunction2<TRet, T0, T1>.generateDynarek(name: String = "myfunc", interpreted: Boolean = false): (T0, T1) -> TRet =
	generateDynarekResultEx(name, 2, interpreted).func as (T0, T1) -> TRet

fun <TRet : Any, T0 : Any, T1 : Any, T2 : Any> DFunction3<TRet, T0, T1, T2>.generateDynarek(name: String = "myfunc", interpreted: Boolean = false): (T0, T1, T2) -> TRet =
	generateDynarekResultEx(name, 3, interpreted).func as (T0, T1, T2) -> TRet

fun DFunction.generateDynarekResultEx(name: String, count: Int, interpreted: Boolean): DynarekResult {
	return if (interpreted) generateDynarekResultInterpreted(name, count) else generateDynarekResult(name, count)
}

fun DFunction.generateDynarekResultInterpreted(name: String, count: Int): DynarekResult {
	val func = this

	return DynarekResult(byteArrayOf(), when (count) {
		0 -> run { { DSlowInterpreter(listOf<Any?>()).interpret(func) } }
		1 -> run { { v0: Any? -> DSlowInterpreter(listOf(v0)).interpret(func) } }
		2 -> run { { v0: Any?, v1: Any? -> DSlowInterpreter(listOf(v0, v1)).interpret(func) } }
		3 -> run { { v0: Any?, v1: Any?, v2: Any? -> DSlowInterpreter(listOf(v0, v1, v2)).interpret(func) } }
		4 -> run { { v0: Any?, v1: Any?, v2: Any?, v3: Any? -> DSlowInterpreter(listOf(v0, v1, v2, v3)).interpret(func) } }
		else -> TODO("Unsupported functions of arity $count")
	})
}

class InvalidCodeGenerated(val baseMessage: String, val data: ByteArray, val exception: Throwable) :
	RuntimeException("InvalidCodeGenerated: $baseMessage :: $exception")

class DynarekResult(val data: ByteArray, val func: Any)
