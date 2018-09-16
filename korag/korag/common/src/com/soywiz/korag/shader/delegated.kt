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

class UniformMatrixStorage(val uniforms: AG.UniformValues, val uniform: Uniform, val matrix: Matrix4) {
	init {
		uniforms[uniform] = matrix
	}

	fun delegate() = this

	operator fun getValue(obj: Any, prop: KProperty<*>): Matrix4 = matrix
	operator fun setValue(obj: Any, prop: KProperty<*>, matrix: Matrix4) {
		uniforms[uniform] = matrix
	}

}

fun AG.UniformValues.storageFor(uniform: Uniform, array: FloatArray = FloatArray(4)) = UniformFloatStorage(this, uniform, array)
fun AG.UniformValues.storageForMatrix(uniform: Uniform, matrix: Matrix4 = Matrix4()) = UniformMatrixStorage(this, uniform, matrix)
