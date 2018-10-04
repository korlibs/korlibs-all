package com.soywiz.klock


class SimplerDateFormat(val format: String) {
	companion object {
		private val rx = Regex("('[\\w]+'|[\\w]+\\B[^X]|[X]{1,3}|[\\w]+)")
		private val englishDaysOfWeek = listOf(
			"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"
		)
		private val englishMonths = listOf(
			"january", "february", "march", "april", "may", "june",
			"july", "august", "september", "october", "november", "december"
		)
		private val englishMonths3 = englishMonths.map { it.substr(0, 3) }

		val DEFAULT_FORMAT by lazy { SimplerDateFormat("EEE, dd MMM yyyy HH:mm:ss z") }
		val FORMAT1 by lazy { SimplerDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX") }

		val FORMATS = listOf(DEFAULT_FORMAT, FORMAT1)

		fun parse(str: String): DateTime {
			var lastError: Throwable? = null
			for (format in FORMATS) {
				try {
					return format.parseDate(str)
				} catch (e: Throwable) {
					lastError = e
				}
			}
			throw lastError!!
		}
	}

	private val parts = arrayListOf<String>()
	//val escapedFormat = Regex.escape(format)
	private val escapedFormat = Regex.escapeReplacement(format)

	private val rx2: Regex = Regex("^" + escapedFormat.replace(rx) { result ->
		val v = result.groupValues[0]
		parts += v
		if (v.startsWith("'")) {
			"(" + Regex.escapeReplacement(v.trim('\'')) + ")"
		} else if (v.startsWith("X")) {
			"([Z]|[+-]\\d\\d|[+-]\\d\\d\\d\\d|[+-]\\d\\d:\\d\\d)?"
		} else {
			"([\\w\\+\\-]+?[^Z^+^-])"
		}
	} + "$")

	private val parts2 = escapedFormat.splitKeep(rx)

	// EEE, dd MMM yyyy HH:mm:ss z -- > Sun, 06 Nov 1994 08:49:37 GMT
	// YYYY-MM-dd HH:mm:ss

	fun format(date: Long): String = format(DateTime.fromUnix(date))

	fun format(dd: DateTime): String {
		var out = ""
		for (name2 in parts2) {
			val name = name2.trim('\'')
			out += when (name) {
				"EEE" -> englishDaysOfWeek[dd.dayOfWeek.index].substr(0, 3).capitalize()
				"EEEE" -> englishDaysOfWeek[dd.dayOfWeek.index].capitalize()
				"EEEEE" -> englishDaysOfWeek[dd.dayOfWeek.index].substr(0, 1).capitalize()
				"EEEEEE" -> englishDaysOfWeek[dd.dayOfWeek.index].substr(0, 2).capitalize()
				"z", "zzz" -> dd.timeZone
				"d" -> "%d".format(dd.dayOfMonth)
				"dd" -> "%02d".format(dd.dayOfMonth)
				"M" -> "%d".format(dd.month1)
				"MM" -> "%02d".format(dd.month1)
				"MMM" -> englishMonths[dd.month0].substr(0, 3).capitalize()
				"MMMM" -> englishMonths[dd.month0].capitalize()
				"MMMMM" -> englishMonths[dd.month0].substr(0, 1).capitalize()
				"y","yyyy" -> "%04d".format(dd.year)
				"YYYY" -> "%04d".format(dd.year)
				"H" -> "%d".format(dd.hours)
				"HH" -> "%02d".format(dd.hours)
				"h" ->  "%d".format(((12+dd.hours)%12))
				"hh" ->  "%02d".format(((12+dd.hours)%12))
				"m" -> "%d".format(dd.minutes)
				"mm" -> "%02d".format(dd.minutes)
				"s" -> "%d".format(dd.seconds)
				"ss" -> "%02d".format(dd.seconds)
				"X", "XX", "XXX" -> {
					val p = if (dd.offset >= 0) "+" else "-"
					val hours = dd.offset / 60
					val minutes = dd.offset % 60
					when (name) {
						"X" -> "$p${"%02d".format(hours)}"
						"XX" -> "$p${"%02d".format(hours)}${"%02d".format(minutes)}"
						"XXX" -> "$p${"%02d".format(hours)}:${"%02d".format(minutes)}"
						else -> name
					}
				}
				"a" -> if (dd.hours<12) "am" else "pm"
				else -> name
			}
		}
		return out
	}

	fun parse(str: String): Long = parseDate(str).unix
	fun parseUtc(str: String): Long = parseDate(str).toUtc().unix

	fun parseOrNull(str: String?): Long? = try {
		str?.let { parse(str) }
	} catch (e: Throwable) {
		null
	}

	fun parseDate(str: String): DateTime {
		return tryParseDate(str) ?: throw RuntimeException("Not a valid format: '$str' for '$format'")
	}

	fun tryParseDate(str: String): DateTime? {
		var second = 0
		var minute = 0
		var hour = 0
		var day = 1
		var month = 1
		var fullYear = 1970
		var offset: Int? = null
		var isPm = false
		var is12HourFormat = false
		val result = rx2.find(str) ?: return null
		for ((name, value) in parts.zip(result.groupValues.drop(1))) {
			when (name) {
				"EEE", "EEEE" -> Unit // day of week (Sun | Sunday)
				"z", "zzz" -> Unit // timezone (GMT)
				"d", "dd" -> day = value.toInt()
				"M", "MM" -> month = value.toInt()
				"MMM" -> month = englishMonths3.indexOf(value.toLowerCase()) + 1
				"yyyy", "YYYY" -> fullYear = value.toInt()
				"HH" -> hour = value.toInt()
				"mm" -> minute = value.toInt()
				"ss" -> second = value.toInt()
				"X", "XX", "XXX" -> when {
					value.first() == 'Z' -> offset = 0
					else -> {
						val hours = value.drop(1).substringBefore(':').toInt()
						val minutes = value.substringAfter(':', "0").toInt()
						offset = (hours * 60) + minutes
						if (value.first() == '-') {
							offset = -offset
						}
					}
				}
				"MMMM" -> month = englishMonths.indexOf(value.toLowerCase()) + 1
				"MMMMM" -> throw RuntimeException("Not possible to get the month from one letter.")
				"y", "yyyy", "YYYY" -> fullYear = value.toInt()
				"H", "HH" -> hour = value.toInt()
				"h", "hh" -> {
					hour = value.toInt()
					is12HourFormat = true
				}
				"m", "mm" -> minute = value.toInt()
				"s", "ss" -> second = value.toInt()
				"a" -> isPm = value == "pm"
				else -> {
					// ...
				}
			}
		}
		//return DateTime.createClamped(fullYear, month, day, hour, minute, second)
		if (is12HourFormat and isPm) hour += 12
		val dateTime = DateTime.createAdjusted(fullYear, month, day, hour, minute, second)
		return when (offset) {
			null -> dateTime
			0 -> dateTime.toUtc()
			else -> dateTime.minus(TimeDistance(minutes = (offset).toDouble())).toOffset(offset)
		}
	}
}

