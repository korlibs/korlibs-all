package com.soywiz.korau.format.atrac3plus

class WaveEnvelope {
	internal var hasStartPoint: Boolean = false ///< indicates start point within the GHA window
	internal var hasStopPoint: Boolean = false  ///< indicates stop point within the GHA window
	internal var startPos: Int = 0          ///< start position expressed in n*4 samples
	internal var stopPos: Int = 0           ///< stop  position expressed in n*4 samples

	fun clear() {
		hasStartPoint = false
		hasStopPoint = false
		startPos = 0
		stopPos = 0
	}

	fun copy(from: WaveEnvelope) {
		this.hasStartPoint = from.hasStartPoint
		this.hasStopPoint = from.hasStopPoint
		this.startPos = from.startPos
		this.stopPos = from.stopPos
	}
}
