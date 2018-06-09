package com.soywiz.korma.geom

import com.soywiz.kds.*
import kotlin.math.*

// @TODO: inline class
data class Angle private constructor(val radians: Double) {
	val degrees get() = rad2deg(radians)

	val normalizedRadians get() = KdsExt { radians umod MAX_RADIANS }
	val normalizedDegrees get() = KdsExt { degrees umod MAX_DEGREES }

	fun shortDistanceTo(other: Angle): Angle = Angle(shortRadDistanceTo(this.radians, other.radians))

	operator fun times(scale: Double) = Angle(this.radians * scale)
	operator fun div(scale: Double) = Angle(this.radians / scale)
	operator fun plus(other: Angle) = Angle(this.radians + other.radians)
	operator fun minus(other: Angle) = shortDistanceTo(other)
	val absoluteValue: Angle get() = fromRadians(radians.absoluteValue)

	override fun toString(): String = "Angle($degrees)"

	companion object {
		fun fromRadians(rad: Double) = Angle(rad)
		fun fromDegrees(deg: Double) = Angle(deg2rad(deg))

		inline fun fromRadians(rad: Number) = fromRadians(rad.toDouble())
		inline fun fromDegrees(deg: Number) = fromDegrees(deg.toDouble())

		const val PI2 = PI * 2

		const val DEG2RAD = PI / 180.0
		const val RAD2DEG = 180.0 / PI

		const val MAX_DEGREES = 360.0
		const val MAX_RADIANS = PI2

		const val HALF_DEGREES = MAX_DEGREES / 2
		const val HALF_RADIANS = MAX_RADIANS / 2

		fun cos01(ratio: Double) = kotlin.math.cos(PI * 2.0 * ratio)
		fun sin01(ratio: Double) = kotlin.math.sin(PI * 2.0 * ratio)
		fun deg2rad(deg: Double) = deg * DEG2RAD
		fun rad2deg(rad: Double) = rad * RAD2DEG

		fun degreesToRadians(deg: Double) = deg * DEG2RAD
		fun radiansToDegrees(rad: Double) = rad * RAD2DEG

		fun toRadians(v: Double): Double = v / 180.0 * 3.141592653589793
		fun toDegrees(v: Double): Double = v * 180.0 / 3.141592653589793

		fun shortRadDistanceTo(fromRad: Double, toRad: Double): Double {
			val r0 = KdsExt { fromRad umod MAX_RADIANS }
			val r1 = KdsExt { toRad umod MAX_RADIANS }
			//if (abs(r0 - r1) > HALF_RADIANS) {
//
			//} else {
//
			//}

			val diff = (r1 - r0 + HALF_RADIANS) % MAX_RADIANS - HALF_RADIANS
			return if (diff < -HALF_RADIANS) diff + MAX_RADIANS else diff
		}
	}
}
