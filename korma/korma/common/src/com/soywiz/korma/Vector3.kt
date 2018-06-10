package com.soywiz.korma

interface IVector3 {
	val x: Double
	val y: Double
	val z: Double
}

data class Vector3(override var x: Double, override var y: Double, override var z: Double) : IVector3 {
	data class Immutable(override val x: Double, override val y: Double, override val z: Double) : IVector3
}

inline fun Vector3(x: Number, y: Number, z: Number) = Vector3(x.toDouble(), y.toDouble(), z.toDouble())
inline fun IVector3(x: Number, y: Number, z: Number) = Vector3.Immutable(x.toDouble(), y.toDouble(), z.toDouble())