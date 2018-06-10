package com.soywiz.dynarek

import com.soywiz.dynarek.js.*

fun _generateDynarek(name: String, nargs: Int, func: DFunction): dynamic {
	val body = func.generateJsBody(strict = true)
	//println(body)

	// @TODO: Kotlin.JS: This produces syntax error!
	//val argNames = (0 until nargs).map { "p$it" }.toTypedArray()
	//return JsFunction(*argNames, sb.toString())

	val argsStr = (0 until nargs).map { "p$it" }.joinToString(", ")

	val rname = if (name.isEmpty()) "func" else name

	return eval("(function $rname($argsStr) { $body })")

	/*
	val f = when (nargs) {
		0 -> JsFunction(body)
		1 -> JsFunction("p0", body)
		2 -> JsFunction("p0", "p1", body)
		3 -> JsFunction("p0", "p1", "p2", body)
		4 -> JsFunction("p0", "p1", "p2", "p3", body)
		else -> TODO("Unsupported args $nargs")
	}
	val obj = jsObj()
	obj.value = name
	JsObject.defineProperty(f, "name", obj);
	return f
	*/
}

//external fun eval(str: String, vararg args: dynamic): dynamic

/*
fun jsObj() = js("({})")

@JsName("Function")
external class JsFunction(vararg args: dynamic)

@JsName("Object")
external object JsObject {
	fun defineProperty(obj: dynamic, name: String, value: dynamic)
}
*/