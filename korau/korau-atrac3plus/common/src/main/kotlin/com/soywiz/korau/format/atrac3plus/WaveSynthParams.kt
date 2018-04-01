package com.soywiz.korau.format.atrac3plus

import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.ATRAC3P_SUBBANDS

class WaveSynthParams {
	internal var tonesPresent: Boolean = false                                  ///< 1 - tones info present
	internal var amplitudeMode: Int = 0                                     ///< 1 - low range, 0 - high range
	internal var numToneBands: Int = 0                                      ///< number of PQF bands with tones
	internal var toneSharing = BooleanArray(ATRAC3P_SUBBANDS) ///< 1 - subband-wise tone sharing flags
	internal var toneMaster = BooleanArray(ATRAC3P_SUBBANDS)  ///< 1 - subband-wise tone channel swapping
	internal var phaseShift = BooleanArray(ATRAC3P_SUBBANDS)  ///< 1 - subband-wise 180 degrees phase shifting
	internal var tonesIndex: Int = 0                                        ///< total sum of tones in this unit
	internal var waves = arrayOfNulls<WaveParam>(48)

	init {
		for (i in waves.indices) {
			waves[i] = WaveParam()
		}
	}
}
