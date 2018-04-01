package com.soywiz.dynarek

import com.soywiz.dynarek.js.generateJsBody

@JsName("Function")
external class JsFunction(vararg args: dynamic)

fun _generateDynarek(nargs: Int, func: DFunction): dynamic {
	val body = func.generateJsBody(strict = true)
	//println(body)

	// @TODO: Kotlin.JS: This produces syntax error!
	//val argNames = (0 until nargs).map { "p$it" }.toTypedArray()
	//return JsFunction(*argNames, sb.toString())

	return when (nargs) {
		0 -> JsFunction(body)
		1 -> JsFunction("p0", body)
		2 -> JsFunction("p0", "p1", body)
		else -> TODO("Unsupported args $nargs")
	}
}
