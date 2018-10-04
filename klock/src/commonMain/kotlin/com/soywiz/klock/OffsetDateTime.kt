package com.soywiz.klock

import kotlin.math.*

class OffsetDateTime private constructor(
	override val utc: UtcDateTime,
	override val offset: Int,
	val adjusted: DateTime = utc.addMinutes(offset.toDouble())
) : DateTime by adjusted {
	private val deltaTotalMinutesAbs: Int = abs(offset)
	val positive: Boolean get() = offset >= 0
	val deltaHoursAbs: Int get() = deltaTotalMinutesAbs / 60
	val deltaMinutesAbs: Int get() = deltaTotalMinutesAbs % 60

	companion object {
		//operator fun invoke(utc: DateTime, offset: Int) = OffsetDateTime(utc.utc, utc.offsetTotalMinutes + offset)
		operator fun invoke(utc: DateTime, offset: Int) = OffsetDateTime(utc.utc, offset)
	}

	override val timeZone: String = "GMT%s%02d%02d".format(
		if (positive) "+" else "-",
		deltaHoursAbs,
		deltaMinutesAbs
	)

	override fun add(deltaMonths: Int, deltaMilliseconds: Long): DateTime =
		OffsetDateTime(utc.add(deltaMonths, deltaMilliseconds), offset)

	override fun toUtc(): DateTime = utc

	override fun toString(): String = SimplerDateFormat.DEFAULT_FORMAT.format(this)
}
