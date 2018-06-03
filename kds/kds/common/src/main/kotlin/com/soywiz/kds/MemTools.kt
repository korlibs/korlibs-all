package com.soywiz.kds

object MemTools {
	fun <T> arraycopy(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, size: Int) {
		val overlapping = src === dst && dstPos > srcPos
		if (overlapping) {
			var n = size
			while (--n >= 0) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in 0 until size)dst[dstPos+n] = src[srcPos+n]
		}
	}

	fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, size: Int) {
		val overlapping = src === dst && dstPos > srcPos
		if (overlapping) {
			var n = size
			while (--n >= 0) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in 0 until size)dst[dstPos+n] = src[srcPos+n]
		}
	}

	fun arraycopy(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int) {
		val overlapping = src === dst && dstPos > srcPos
		if (overlapping) {
			var n = size
			while (--n >= 0) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in 0 until size)dst[dstPos+n] = src[srcPos+n]
		}
	}

	fun <T> fill(array: Array<T>, value: T) = run { for (n in 0 until array.size) array[n] = value }
	fun fill(array: IntArray, value: Int) = run { for (n in 0 until array.size) array[n] = value }
}