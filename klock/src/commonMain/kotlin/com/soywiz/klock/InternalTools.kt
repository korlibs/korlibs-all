package com.soywiz.klock

import kotlin.math.*

internal const val MILLIS_PER_SECOND = 1000
internal const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60
internal const val MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60
internal const val MILLIS_PER_DAY = MILLIS_PER_HOUR * 24
internal const val MILLIS_PER_WEEK = MILLIS_PER_DAY * 7

internal const val DAYS_PER_YEAR = 365
internal const val DAYS_PER_4_YEARS = DAYS_PER_YEAR * 4 + 1
internal const val DAYS_PER_100_YEARS = DAYS_PER_4_YEARS * 25 - 1
internal const val DAYS_PER_400_YEARS = DAYS_PER_100_YEARS * 4 + 1

internal val formatRegex = Regex("%([-]?\\d+)?(\\w)")

internal fun String.format(vararg params: Any): String {
	var paramIndex = 0
	return formatRegex.replace(this) { mr ->
		val param = params[paramIndex++]
		//println("param: $param")
		val size = mr.groupValues[1]
		val type = mr.groupValues[2]
		val str = when (type) {
			"d" -> (param as Number).toLong().toString()
			"X" -> (param as Number).toLong().toString(16).toUpperCase()
			"x" -> (param as Number).toLong().toString(16).toLowerCase()
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

internal val DIGITS_UPPER = "0123456789ABCDEF"

internal fun Long.toString(radix: Int): String {
	val isNegative = this < 0
	var temp = abs(this)
	if (temp == 0L) {
		return "0"
	} else {
		var out = ""
		while (temp != 0L) {
			val digit = temp % radix
			temp /= radix
			out += DIGITS_UPPER[digit.toInt()]
		}
		val rout = out.reversed()
		return if (isNegative) "-$rout" else rout
	}
}

internal fun String.substr(start: Int): String = this.substr(start, this.length)

internal fun String.substr(start: Int, length: Int): String {
	val low = (if (start >= 0) start else this.length + start).clamp(0, this.length)
	val high = (if (length >= 0) low + length else this.length + length).clamp(0, this.length)
	if (high < low) {
		return ""
	} else {
		return this.substring(low, high)
	}
}

internal fun Int.clamp(min: Int, max: Int): Int = if (this < min) min else if (this > max) max else this

internal fun Int.cycle(min: Int, max: Int): Int {
	return ((this - min) umod (max - min + 1)) + min
}

internal fun Int.cycleSteps(min: Int, max: Int): Int {
	return (this - min) / (max - min + 1)
}

internal fun String.splitKeep(regex: Regex): List<String> {
	val str = this
	val out = arrayListOf<String>()
	var lastPos = 0
	for (part in regex.findAll(this)) {
		val prange = part.range
		if (lastPos != prange.start) {
			out += str.substring(lastPos, prange.start)
		}
		out += str.substring(prange)
		lastPos = prange.endInclusive + 1
	}
	if (lastPos != str.length) {
		out += str.substring(lastPos)
	}
	return out
}

internal infix fun Int.umod(that: Int): Int {
	val remainder = this % that
	return when {
		remainder < 0 -> remainder + that
		else -> remainder
	}
}
