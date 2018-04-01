package com.soywiz.korau.format.atrac3plus

import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.ATRAC3P_FRAME_SAMPLES
import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.ATRAC3P_SUBBAND_SAMPLES
import com.soywiz.korau.format.util.BitReader
import com.soywiz.korau.format.util.FFT

class Context {
	var br: BitReader? = null
	var dsp: Atrac3plusDsp? = null

	var channelUnits = arrayOfNulls<ChannelUnit>(16) ///< global channel units
	var numChannelBlocks = 2                         ///< number of channel blocks
	var outputChannels: Int = 0

	var gaincCtx: Atrac? = null ///< gain compensation context
	var mdctCtx: FFT? = null
	var ipqfDctCtx: FFT? = null ///< IDCT context used by IPQF

	var samples = Array(2) { FloatArray(ATRAC3P_FRAME_SAMPLES) } ///< quantized MDCT sprectrum
	var mdctBuf = Array(2) { FloatArray(ATRAC3P_FRAME_SAMPLES + ATRAC3P_SUBBAND_SAMPLES) } ///< output of the IMDCT
	var timeBuf = Array(2) { FloatArray(ATRAC3P_FRAME_SAMPLES) } ///< output of the gain compensation
	var outpBuf = Array(2) { FloatArray(ATRAC3P_FRAME_SAMPLES) }
}
