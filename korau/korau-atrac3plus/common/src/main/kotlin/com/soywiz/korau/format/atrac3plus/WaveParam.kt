package com.soywiz.korau.format.atrac3plus

/** Parameters of a single sine wave  */
class WaveParam {
	internal var freqIndex: Int = 0  ///< wave frequency index
	internal var ampSf: Int = 0      ///< quantized amplitude scale factor
	internal var ampIndex: Int = 0   ///< quantized amplitude index
	internal var phaseIndex: Int = 0 ///< quantized phase index

	fun clear() {
		freqIndex = 0
		ampSf = 0
		ampIndex = 0
		phaseIndex = 0
	}
}
