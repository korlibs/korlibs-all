package com.soywiz.korau.format.util

object FloatDSP {
	fun vectorFmul(dst: FloatArray, dstOffset: Int, src0: FloatArray, src0Offset: Int, src1: FloatArray, src1Offset: Int, len: Int) {
		for (i in 0 until len) {
			dst[dstOffset + i] = src0[src0Offset + i] * src1[src1Offset + i]
		}
	}

	fun vectorFmacScalar(dst: FloatArray, dstOffset: Int, src: FloatArray, srcOffset: Int, mul: Float, len: Int) {
		for (i in 0 until len) {
			dst[dstOffset + i] += src[srcOffset + i] * mul
		}
	}

	fun vectorFmulScalar(dst: FloatArray, dstOffset: Int, src: FloatArray, srcOffset: Int, mul: Float, len: Int) {
		for (i in 0 until len) {
			dst[dstOffset + i] = src[srcOffset + i] * mul
		}
	}

	fun vectorDmulScalar(dst: DoubleArray, dstOffset: Int, src: DoubleArray, srcOffset: Int, mul: Double, len: Int) {
		for (i in 0 until len) {
			dst[dstOffset + i] = src[srcOffset + i] * mul
		}
	}

	fun vectorFmulWindow(dst: FloatArray, dstOffset: Int, src0: FloatArray, src0Offset: Int, src1: FloatArray, src1Offset: Int, win: FloatArray, winOffset: Int, len: Int) {
		var dstOffset = dstOffset
		var src0Offset = src0Offset
		var winOffset = winOffset
		dstOffset += len
		winOffset += len
		src0Offset += len
		var i = -len
		var j = len - 1
		while (i < 0) {
			val s0 = src0[src0Offset + i]
			val s1 = src1[src1Offset + j]
			val wi = win[winOffset + i]
			val wj = win[winOffset + j]
			dst[dstOffset + i] = s0 * wj - s1 * wi
			dst[dstOffset + j] = s0 * wi + s1 * wj
			i++
			j--
		}
	}

	fun vectorFmulAdd(dst: FloatArray, dstOffset: Int, src0: FloatArray, src0Offset: Int, src1: FloatArray, src1Offset: Int, src2: FloatArray, src2Offset: Int, len: Int) {
		for (i in 0 until len) {
			dst[dstOffset + i] = src0[src0Offset + i] * src1[src1Offset + i] + src2[src2Offset + i]
		}
	}

	fun vectorFmulReverse(dst: FloatArray, dstOffset: Int, src0: FloatArray, src0Offset: Int, src1: FloatArray, src1Offset: Int, len: Int) {
		for (i in 0 until len) {
			dst[dstOffset + i] = src0[src0Offset + i] * src1[src1Offset + len - 1 - i]
		}
	}

	fun butterflies(v1: FloatArray, v1Offset: Int, v2: FloatArray, v2Offset: Int, len: Int) {
		for (i in 0 until len) {
			val t = v1[v1Offset + i] - v2[v2Offset + i]
			v1[v1Offset + i] += v2[v2Offset + i]
			v2[v2Offset + i] = t
		}
	}

	fun scalarproduct(v1: FloatArray, v1Offset: Int, v2: FloatArray, v2Offset: Int, len: Int): Float {
		var p = 0f

		for (i in 0 until len) {
			p += v1[v1Offset + i] * v2[v2Offset + i]
		}

		return p
	}
}
