package com.soywiz.korau.format.atrac3plus

/** Parameters of a group of sine waves  */
class WavesData {
	internal var pendEnv: WaveEnvelope ///< pending envelope from the previous frame
	internal var currEnv: WaveEnvelope ///< group envelope from the current frame
	internal var numWavs: Int = 0          ///< number of sine waves in the group
	internal var startIndex: Int = 0       ///< start index into global tones table for that subband

	init {
		pendEnv = WaveEnvelope()
		currEnv = WaveEnvelope()
	}

	fun clear() {
		pendEnv.clear()
		currEnv.clear()
		numWavs = 0
		startIndex = 0
	}

	fun copy(from: WavesData) {
		this.pendEnv.copy(from.pendEnv)
		this.currEnv.copy(from.currEnv)
		this.numWavs = from.numWavs
		this.startIndex = from.startIndex
	}
}
