package com.soywiz.korma

interface Vector3 {
	val x: Double
	val y: Double
	val z: Double

	companion object {
		inline operator fun invoke(x: Number, y: Number, z: Number): Vector3 = IVector3(x.toDouble(), y.toDouble(), z.toDouble())
	}

	abstract class Base : Vector3 {
		override fun equals(other: Any?): Boolean = if (other is Vector3) this.x == other.x && this.y == other.y else false
		override fun hashCode(): Int = x.hashCode() + (y.hashCode() shl 3) + (z.hashCode() shl 7)
		override fun toString(): String = KormaStr { "(${x.niceStr}, ${y.niceStr}, ${z.niceStr})" }
	}
}

@PublishedApi
internal class IVector3(
	override val x: Double,
	override val y: Double,
	override val z: Double
) : Vector3.Base()

class MVector3(
	override var x: Double,
	override var y: Double,
	override var z: Double
) : Vector3.Base()
