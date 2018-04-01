package com.soywiz.korte

data class TeFunction(val name: String, val eval: suspend (args: List<Any?>, context: Template.EvalContext) -> Any?) {
	suspend fun eval(args: List<Any?>, context: Template.EvalContext) = eval.invoke(args, context)
}
