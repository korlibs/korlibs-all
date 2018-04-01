package com.soywiz.korau.format.atrac3plus

import com.soywiz.kmem.arraycopy

/**
 * Gain control parameters for one subband.
 */
class AtracGainInfo {
	var numPoints: Int = 0             ///< number of gain control points
	var levCode = IntArray(7) ///< level at corresponding control point
	var locCode = IntArray(7) ///< location of gain control points

	fun clear() {
		numPoints = 0
		for (i in 0..6) {
			levCode[i] = 0
			locCode[i] = 0
		}
	}

	fun copy(from: AtracGainInfo) {
		this.numPoints = from.numPoints
		arraycopy(from.levCode, 0, this.levCode, 0, levCode.size)
		arraycopy(from.locCode, 0, this.locCode, 0, locCode.size)
	}
}
