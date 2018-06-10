package com.soywiz.kmem

import kotlin.math.*


private val formatRegex = Regex("%([-]?\\d+)?(\\w)")

fun String.format(vararg params: Any): String {
	var paramIndex = 0
	return formatRegex.replace(this) { mr ->
		val param = params[paramIndex++]
		//println("param: $param")
		val size = mr.groupValues[1]
		val type = mr.groupValues[2]
		val str = when (type.toLowerCase()) {
			"d" -> (param as Number).toLong().toString()
			"x" -> {
				val res = when (param) {
					is Int -> param.toStringUnsigned(16)
					else -> (param as Number).toLong().toStringUnsigned(16)
				}
				if (type == "X") res.toUpperCase() else res.toLowerCase()
			}
			else -> "$param"
		}
		val prefix = if (size.startsWith('0')) '0' else ' '
		val asize = size.toIntOrNull()
		var str2 = str
		if (asize != null) {
			while (str2.length < asize) {
				str2 = prefix + str2
			}
		}
		str2
	}
}

fun Long.toString(radix: Int): String {
	val isNegative = this < 0
	var temp = abs(this)
	if (temp == 0L) {
		return "0"
	} else {
		var out = ""
		while (temp != 0L) {
			val digit = temp % radix
			temp /= radix
			out += Hex.DIGITS_UPPER[digit.toInt()]
		}
		val rout = out.reversed()
		return if (isNegative) "-$rout" else rout
	}
}

fun Int.toString(radix: Int): String {
	val isNegative = this < 0
	var temp = abs(this)
	if (temp == 0) {
		return "0"
	} else {
		var out = ""
		while (temp != 0) {
			val digit = temp % radix
			temp /= radix
			out += Hex.DIGITS_UPPER[digit.toInt()]
		}
		val rout = out.reversed()
		return if (isNegative) "-$rout" else rout
	}
}

fun Int.toStringUnsigned(radix: Int): String {
	var temp = this
	if (temp == 0) {
		return "0"
	} else {
		var out = ""
		while (temp != 0) {
			val digit = temp urem radix
			temp = temp udiv radix
			out += Hex.DIGITS_UPPER[digit]
		}
		val rout = out.reversed()
		return rout
	}
}

fun Long.toStringUnsigned(radix: Int): String {
	var temp = this
	if (temp == 0L) {
		return "0"
	} else {
		var out = ""
		while (temp != 0L) {
			val digit = temp urem radix.toLong()
			temp = temp udiv radix.toLong()
			out += Hex.DIGITS_UPPER[digit.toInt()]
		}
		val rout = out.reversed()
		return rout
	}
}

object Hex {
	val DIGITS = "0123456789ABCDEF"
	val DIGITS_UPPER = DIGITS.toUpperCase()
}
