package com.soywiz.dynarek

data class State(
	@JvmField var a: Int = 0,
	@JvmField var b: Int = 0
) {
	@JvmField
	var f0: Float = 0f
	@JvmField
	var f1: Float = 0f
	@JvmField
	var f2: Float = 0f

	@JvmField
	var _f3: Float = 3f
	@JvmField
	var _fm2: Float = -2f

	// Do not have @JvmField to use setter/getter
	var c: Int = 0

	private var _d: Int = 0

	// Custom setter/getter
	var d: Int get() = _d; set(value) = run { _d = value }

	val logList = arrayListOf<Int>()

	@JsName("mulAB")
	fun mulAB() {
		a *= b
	}

	@JsName("mulABArg")
	fun mulABArg(v: Int) {
		a *= b * v
	}

	@JsName("mulABArg2")
	fun mulABArg2(p0: Int, p1: Int) {
		a *= b * (p0 + p1)
	}

	@JsName("log")
	fun log(value: Int) {
		logList += value
	}
}
