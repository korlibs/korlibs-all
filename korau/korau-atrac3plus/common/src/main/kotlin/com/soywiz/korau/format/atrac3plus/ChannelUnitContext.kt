package com.soywiz.korau.format.atrac3plus

import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.ATRAC3P_PQF_FIR_LEN
import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.ATRAC3P_SUBBANDS

/** Channel unit parameters  */
class ChannelUnitContext {
	// Channel unit variables
	var unitType: Int = 0                                     ///< unit type (mono/stereo)
	var numQuantUnits: Int = 0
	var numSubbands: Int = 0
	var usedQuantUnits: Int = 0                               ///< number of quant units with coded spectrum
	var numCodedSubbands: Int = 0                             ///< number of subbands with coded spectrum
	var muteFlag: Boolean = false                                 ///< mute flag
	var useFullTable: Boolean = false                             ///< 1 - full table list, 0 - restricted one
	var noisePresent: Boolean = false                             ///< 1 - global noise info present
	var noiseLevelIndex: Int = 0                              ///< global noise level index
	var noiseTableIndex: Int = 0                              ///< global noise RNG table index
	var swapChannels = BooleanArray(ATRAC3P_SUBBANDS) ///< 1 - perform subband-wise channel swapping
	var negateCoeffs = BooleanArray(ATRAC3P_SUBBANDS) ///< 1 - subband-wise IMDCT coefficients negation
	var channels = arrayOf(Channel(0), Channel(1))

	// Variables related to GHA tones
	var waveSynthHist = arrayOf(WaveSynthParams(), WaveSynthParams()) ///< waves synth history for two frames
	var wavesInfo: WaveSynthParams = waveSynthHist[0]
	var wavesInfoPrev: WaveSynthParams = waveSynthHist[1]

	var ipqfCtx = arrayOf(IPQFChannelContext(), IPQFChannelContext())
	var prevBuf = Array(2) { FloatArray(Atrac3plusDecoder.ATRAC3P_FRAME_SAMPLES) } ///< overlapping buffer

	class IPQFChannelContext {
		var buf1 = Array(ATRAC3P_PQF_FIR_LEN * 2) { FloatArray(8) }
		var buf2 = Array(ATRAC3P_PQF_FIR_LEN * 2) { FloatArray(8) }
		var pos: Int = 0
	}
}
