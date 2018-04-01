package com.soywiz.dynarek

fun <TRet : Any> DFunction0<TRet>.generateInterpreted(): () -> TRet {
	val func = this
	return {
		val interpreter = DSlowInterpreter(listOf<Any>())
		interpreter.interpret(func)
		interpreter.retval as TRet
	}
}

fun <TRet : Any, T0 : Any> DFunction1<TRet, T0>.generateInterpreted(): (T0) -> TRet {
	val func = this
	return { p0 ->
		val interpreter = DSlowInterpreter(listOf<Any?>(p0))
		interpreter.interpret(func)
		interpreter.retval as TRet
	}
}

fun <TRet : Any, T0 : Any, T1 : Any> DFunction2<TRet, T0, T1>.generateInterpreted(): (T0, T1) -> TRet {
	val func = this
	return { p0, p1 ->
		val interpreter = DSlowInterpreter(listOf<Any?>(p0, p1))
		interpreter.interpret(func)
		interpreter.retval as TRet
	}
}
