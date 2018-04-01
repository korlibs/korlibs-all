package com.soywiz.korau.format.atrac3plus

import com.soywiz.klogger.Logger
import com.soywiz.korau.format.util.BitReader
import com.soywiz.korau.format.util.CodecUtils.writeOutput
import com.soywiz.korau.format.util.FFT
import com.soywiz.korau.format.util.IMemory
import com.soywiz.korio.lang.format
import com.soywiz.korio.stream.SyncStream

/*
 * Based on the FFmpeg version from Maxim Poliakovski.
 * All credits go to him.
 * C to Java conversion by gid15 for the jpcsp project.
 * Java to Kotlin for kpspemu
 */
class Atrac3plusDecoder {
	private var ctx: Context? = null

	val numberOfSamples: Int
		get() = ATRAC3P_FRAME_SAMPLES

	fun init(bytesPerFrame: Int, channels: Int, outputChannels: Int, codingMode: Int): Int {
		ctx = Context()
		ctx!!.outputChannels = outputChannels
		ctx!!.dsp = Atrac3plusDsp()
		for (i in 0 until ctx!!.numChannelBlocks) {
			ctx!!.channelUnits[i] = ChannelUnit()
			ctx!!.channelUnits[i]!!.setDsp(ctx!!.dsp!!)
		}

		// initialize IPQF
		ctx!!.ipqfDctCtx = FFT()
		ctx!!.ipqfDctCtx!!.mdctInit(5, true, 31.0 / 32768.9)

		ctx!!.mdctCtx = FFT()
		ctx!!.dsp!!.initImdct(ctx!!.mdctCtx!!)

		Atrac3plusDsp.initWaveSynth()

		ctx!!.gaincCtx = Atrac()
		ctx!!.gaincCtx!!.initGainCompensation(6, 2)

		return 0
	}

	fun decode(mem: IMemory, inputAddr: Int, inputLength: Int, output: SyncStream): Int {
		var ret: Int

		if (ctx == null) {
			return AT3P_ERROR
		}

		if (inputLength < 0) {
			return AT3P_ERROR
		}
		if (inputLength == 0) {
			return 0
		}

		ctx!!.br = BitReader(mem, inputAddr, inputLength)
		if (ctx!!.br!!.readBool()) {
			log.error { "Invalid start bit" }
			return AT3P_ERROR
		}

		var chBlock = 0
		var channelsToProcess = 0
		while (ctx!!.br!!.bitsLeft >= 2) {
			val chUnitId = ctx!!.br!!.read(2)
			if (chUnitId == CH_UNIT_TERMINATOR) {
				break
			}
			if (chUnitId == CH_UNIT_EXTENSION) {
				log.warn { "Non implemented channel unit extension" }
				return AT3P_ERROR
			}

			if (chBlock >= ctx!!.channelUnits.size) {
				log.error { "Too many channel blocks" }
				return AT3P_ERROR
			}

			if (ctx!!.channelUnits[chBlock] == null) {
				log.warn { "Null channelUnits block: $chBlock" }
				break
			}

			val channelUnit = ctx!!.channelUnits[chBlock]!!
			channelUnit.setBitReader(ctx!!.br!!)

			channelUnit.ctx.unitType = chUnitId
			channelsToProcess = chUnitId + 1
			channelUnit.setNumChannels(channelsToProcess)

			ret = channelUnit.decode()
			if (ret < 0) {
				return ret
			}

			channelUnit.decodeResidualSpectrum(ctx!!.samples)
			channelUnit.reconstructFrame(ctx!!)

			writeOutput(ctx!!.outpBuf, output, ATRAC3P_FRAME_SAMPLES, channelsToProcess, ctx!!.outputChannels)

			chBlock++
		}

		log.trace { "Bytes read 0x%X".format(ctx!!.br!!.bytesRead) }

		return ctx!!.br!!.bytesRead
	}

	companion object {
		var log = Logger("atrac3plus")
		val AT3P_ERROR = -1
		val CH_UNIT_MONO = 0        ///< unit containing one coded channel
		val CH_UNIT_STEREO = 1        ///< unit containing two jointly-coded channels
		val CH_UNIT_EXTENSION = 2        ///< unit containing extension information
		val CH_UNIT_TERMINATOR = 3        ///< unit sequence terminator
		val ATRAC3P_POWER_COMP_OFF = 15   ///< disable power compensation
		val ATRAC3P_SUBBANDS = 16         ///< number of PQF subbands
		val ATRAC3P_SUBBAND_SAMPLES = 128 ///< number of samples per subband
		val ATRAC3P_FRAME_SAMPLES = ATRAC3P_SUBBANDS * ATRAC3P_SUBBAND_SAMPLES
		val ATRAC3P_PQF_FIR_LEN = 12      ///< length of the prototype FIR of the PQF
	}
}
