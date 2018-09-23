package com.soywiz.korma.geom

import com.soywiz.kds.*
import kotlin.math.*

inline class Angle(val radians: Double) {
	override fun toString(): String = "Angle($degrees)"

	companion object {
		fun fromRadians(rad: Double): Angle = Angle(rad)
		fun fromDegrees(deg: Double): Angle = Angle(deg2rad(deg))

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

		fun betweenRad(x0: Double, y0: Double, x1: Double, y1: Double): Double {
			//val angle = atan2(other.y, other.x) - atan2(this.y, this.x);
			val angle = atan2(y1 - y0, x1 - x0)
			return if (angle < 0) angle + 2 * PI else angle
		}

		fun between(x0: Double, y0: Double, x1: Double, y1: Double): Angle =
			Angle.fromRadians(betweenRad(x0, y0, x1, y1))

		fun betweenRad(p0: Point2d, p1: Point2d): Double = betweenRad(p0.x, p0.y, p1.x, p1.y)
		fun between(p0: Point2d, p1: Point2d): Angle = Angle.fromRadians(betweenRad(p0, p1))
	}
}

val Angle.degrees get() = Angle.rad2deg(radians)

//val normalizedRadians get() = KdsExt { radians umod Angle.MAX_RADIANS }
//val normalizedDegrees get() = KdsExt { degrees umod Angle.MAX_DEGREES }
val Angle.absoluteValue: Angle get() = Angle.fromRadians(radians.absoluteValue)
fun Angle.shortDistanceTo(other: Angle): Angle = Angle(Angle.shortRadDistanceTo(this.radians, other.radians))
inline operator fun Angle.times(scale: Number): Angle = Angle(this.radians * scale.toDouble())
inline operator fun Angle.div(scale: Number): Angle = Angle(this.radians / scale.toDouble())
inline operator fun Angle.plus(other: Angle): Angle = Angle(this.radians + other.radians)
inline operator fun Angle.minus(other: Angle): Angle = shortDistanceTo(other)
inline operator fun Angle.unaryMinus(): Angle = Angle(-radians)
inline operator fun Angle.unaryPlus(): Angle = Angle(+radians)

val Angle.normalizedRadians get() = KdsExt { radians umod Angle.MAX_RADIANS }
val Angle.normalizedDegrees get() = KdsExt { degrees umod Angle.MAX_DEGREES }
val Angle.normalized get() = KdsExt { Angle(radians umod Angle.MAX_RADIANS) }

inline val Number.degrees get() = Angle.fromDegrees(this)
inline val Number.radians get() = Angle.fromRadians(this)
