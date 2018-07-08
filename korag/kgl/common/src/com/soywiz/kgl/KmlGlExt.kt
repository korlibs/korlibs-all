package com.soywiz.kgl

import com.soywiz.kmem.*

class KmlGlException(message: String) : RuntimeException(message)

fun KmlGl.getShaderiv(shader: Int, type: Int): Int =
	kmlNativeBuffer(4) { getShaderiv(shader, type, it); it.getInt(0) }

fun KmlGl.getProgramiv(program: Int, type: Int): Int =
	kmlNativeBuffer(4) { getProgramiv(program, type, it); it.getInt(0) }

fun KmlGl.getBooleanv(pname: Int): Boolean = kmlNativeBuffer(4) { getBooleanv(pname, it); it[0] != 0 }
fun KmlGl.getFloatv(pname: Int): Float = kmlNativeBuffer(4) { getFloatv(pname, it); it.getFloat(0) }
fun KmlGl.getIntegerv(pname: Int): Int = kmlNativeBuffer(4) { getIntegerv(pname, it); it.getInt(0) }

private inline fun KmlGl.getInfoLog(
	obj: Int,
	getiv: (Int, Int) -> Int,
	getInfoLog: (Int, Int, KmlNativeBuffer, KmlNativeBuffer) -> Unit
): String {
	val size = getiv(obj, INFO_LOG_LENGTH)
	return kmlNativeBuffer(4 * 1) { sizev ->
		kmlNativeBuffer(size) { mbuffer ->
			getInfoLog(obj, size, sizev, mbuffer)
			mbuffer.toAsciiString()
		}
	}
}

fun KmlGl.getShaderInfoLog(shader: Int): String = getInfoLog(shader, ::getShaderiv, ::getShaderInfoLog)
fun KmlGl.getProgramInfoLog(shader: Int): String = getInfoLog(shader, ::getProgramiv, ::getProgramInfoLog)

fun KmlGl.compileShaderAndCheck(shader: Int) {
	compileShader(shader)
	if (getShaderiv(shader, COMPILE_STATUS) != TRUE) {
		throw KmlGlException(getShaderInfoLog(shader))
	}
}

fun KmlGl.linkProgramAndCheck(program: Int) {
	linkProgram(program)
	if (getProgramiv(program, LINK_STATUS) != TRUE) {
		throw KmlGlException(getProgramInfoLog(program))
	}
}

fun KmlGl.getErrorString(error: Int = getError()): String {
	return when (error) {
		NO_ERROR -> "NO_ERROR"
		INVALID_ENUM -> "INVALID_ENUM"
		INVALID_VALUE -> "INVALID_VALUE"
		INVALID_OPERATION -> "INVALID_OPERATION"
		OUT_OF_MEMORY -> "OUT_OF_MEMORY"
		else -> "UNKNOWN_ERROR$error"
	}
}
