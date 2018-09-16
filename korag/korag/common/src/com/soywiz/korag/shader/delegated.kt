package com.soywiz.korag.shader

import com.soywiz.korag.*
import com.soywiz.korma.*
import kotlin.reflect.*

class FloatDelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val onSet: (Float) -> Unit, default: Float) {
	init {
		values[index] = default
	}

	operator fun getValue(obj: Any, prop: KProperty<*>): Float {
		return values[index]
	}

	operator fun setValue(obj: Any, prop: KProperty<*>, value: Float) {
		values[index] = value
		onSet(value)
	}
}

class IntDelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val onSet: (Int) -> Unit, default: Int) {
	init {
		values[index] = default.toFloat()
	}

	operator fun getValue(obj: Any, prop: KProperty<*>): Int {
		return values[index].toInt()
	}

	operator fun setValue(obj: Any, prop: KProperty<*>, value: Int) {
		values[index] = value.toFloat()
		onSet(value)
	}
}

class UniformFloatStorage(val uniforms: AG.UniformValues, val uniform: Uniform, val array: FloatArray) {
	init {
		uniforms[uniform] = array
	}

	fun floatDelegate(index: Int, default: Float = 0f, onSet: (Float) -> Unit = {}) = FloatDelegatedUniform(uniform, array, index, onSet, default)
	fun floatDelegateX(default: Float = 0f, onSet: (Float) -> Unit = {}) = floatDelegate(0, default, onSet)
	fun floatDelegateY(default: Float = 0f, onSet: (Float) -> Unit = {}) = floatDelegate(1, default, onSet)

	fun intDelegate(index: Int, default: Int = 0, onSet: (Int) -> Unit = {}) = IntDelegatedUniform(uniform, array, index, onSet, default)
	fun intDelegateX(default: Int = 0, onSet: (Int) -> Unit = {}) = intDelegate(0, default, onSet)
	fun intDelegateY(default: Int = 0, onSet: (Int) -> Unit = {}) = intDelegate(1, default, onSet)
}

class UniformValueStorage<T : Any>(val uniforms: AG.UniformValues, val uniform: Uniform, val value: T) {
	init {
		uniforms[uniform] = value
	}

	fun delegate() = this

	operator fun getValue(obj: Any, prop: KProperty<*>): T = value
	operator fun setValue(obj: Any, prop: KProperty<*>, value: T) {
		uniforms[uniform] = value
	}
}

fun AG.UniformValues.storageFor(uniform: Uniform, array: FloatArray = FloatArray(4)) = UniformFloatStorage(this, uniform, array)
fun AG.UniformValues.storageForMatrix2(uniform: Uniform, matrix: Matrix2 = Matrix2()) = UniformValueStorage(this, uniform, matrix)
fun AG.UniformValues.storageForMatrix3(uniform: Uniform, matrix: Matrix3 = Matrix3()) = UniformValueStorage(this, uniform, matrix)
fun AG.UniformValues.storageForMatrix4(uniform: Uniform, matrix: Matrix4 = Matrix4()) = UniformValueStorage(this, uniform, matrix)

