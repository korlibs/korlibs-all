package com.soywiz.korau.format.atrac3plus

import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.fill
import com.soywiz.kmem.signExtend
import com.soywiz.korau.format.atrac3plus.Atrac3plusData1.atrac3p_spectra_tabs
import com.soywiz.korau.format.atrac3plus.Atrac3plusData2.atrac3p_ct_restricted_to_full
import com.soywiz.korau.format.atrac3plus.Atrac3plusData2.atrac3p_qu_num_to_seg
import com.soywiz.korau.format.atrac3plus.Atrac3plusData2.atrac3p_qu_to_subband
import com.soywiz.korau.format.atrac3plus.Atrac3plusData2.atrac3p_sf_shapes
import com.soywiz.korau.format.atrac3plus.Atrac3plusData2.atrac3p_subband_to_num_powgrps
import com.soywiz.korau.format.atrac3plus.Atrac3plusData2.atrac3p_wl_shapes
import com.soywiz.korau.format.atrac3plus.Atrac3plusData2.atrac3p_wl_weights
import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.AT3P_ERROR
import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.ATRAC3P_FRAME_SAMPLES
import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.ATRAC3P_POWER_COMP_OFF
import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.ATRAC3P_SUBBANDS
import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.ATRAC3P_SUBBAND_SAMPLES
import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder.Companion.CH_UNIT_STEREO
import com.soywiz.korau.format.atrac3plus.Atrac3plusDsp.Companion.ff_atrac3p_mant_tab
import com.soywiz.korau.format.atrac3plus.Atrac3plusDsp.Companion.ff_atrac3p_qu_to_spec_pos
import com.soywiz.korau.format.atrac3plus.Atrac3plusDsp.Companion.ff_atrac3p_sf_tab
import com.soywiz.korau.format.util.*
import com.soywiz.korau.format.util.CodecUtils.avLog2
import com.soywiz.korio.lang.format
import kotlin.math.abs

/*
 * Based on the FFmpeg version from Maxim Poliakovski.
 * All credits go to him.
 * C to Java conversion by gid15 for the jpcsp project.
 * Java to Kotlin for kpspemu
 */
class ChannelUnit {
	var ctx = ChannelUnitContext()
	private lateinit var br: BitReader
	private var dsp: Atrac3plusDsp? = null
	private var numChannels: Int = 0

	/**
	 * Decode number of code table values.
	 *
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private val numCtValues: Int
		get() {
			if (!br.readBool()) {
				return ctx.usedQuantUnits
			}

			val numCodedVals = br.read(5)
			if (numCodedVals > ctx.usedQuantUnits) {
				log.error { "Invalid number of code table indexes: %d".format(numCodedVals) }
				return AT3P_ERROR
			}
			return numCodedVals
		}

	fun setBitReader(br: BitReader) {
		this.br = br
	}

	fun setDsp(dsp: Atrac3plusDsp) {
		this.dsp = dsp
	}

	fun setNumChannels(numChannels: Int) {
		this.numChannels = numChannels
	}

	fun decode(): Int {
		var ret: Int

		ctx.numQuantUnits = br.read(5) + 1
		if (ctx.numQuantUnits > 28 && ctx.numQuantUnits < 32) {
			log.error("Invalid number of quantization units: %d".format(ctx.numQuantUnits))
			return AT3P_ERROR
		}

		ctx.muteFlag = br.readBool()

		ret = decodeQuantWordlen()
		if (ret < 0) {
			return ret
		}

		ctx.numSubbands = atrac3p_qu_to_subband[ctx.numQuantUnits - 1] + 1
		ctx.numCodedSubbands = if (ctx.usedQuantUnits > 0) atrac3p_qu_to_subband[ctx.usedQuantUnits - 1] + 1 else 0

		ret = decodeScaleFactors()
		if (ret < 0) {
			return ret
		}

		ret = decodeCodeTableIndexes()
		if (ret < 0) {
			return ret
		}

		decodeSpectrum()

		if (numChannels == 2) {
			getSubbandFlags(ctx.swapChannels, ctx.numCodedSubbands)
			getSubbandFlags(ctx.negateCoeffs, ctx.numCodedSubbands)
		}

		decodeWindowShape()

		ret = decodeGaincData()
		if (ret < 0) {
			return ret
		}

		ret = decodeTonesInfo()
		if (ret < 0) {
			return ret
		}

		ctx.noisePresent = br.readBool()
		if (ctx.noisePresent) {
			ctx.noiseLevelIndex = br.read(4)
			ctx.noiseTableIndex = br.read(4)
		}

		return 0
	}

	/**
	 * Decode number of coded quantization units.
	 *
	 * @param[in,out] chan          ptr to the channel parameters
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun numCodedUnits(chan: Channel): Int {
		chan.fillMode = br.read(2)
		if (chan.fillMode == 0) {
			chan.numCodedVals = ctx.numQuantUnits
		} else {
			chan.numCodedVals = br.read(5)
			if (chan.numCodedVals > ctx.numQuantUnits) {
				log.error { "Invalid number of transmitted units" }
				return AT3P_ERROR
			}

			if (chan.fillMode == 3) {
				chan.splitPoint = br.read(2) + (chan.chNum shl 1) + 1
			}
		}

		return 0
	}

	private fun getDelta(deltaBits: Int): Int {
		return if (deltaBits <= 0) 0 else br.read(deltaBits)
	}

	/**
	 * Unpack vector quantization tables.
	 *
	 * @param startVal    start value for the unpacked table
	 * @param shapeVec    ptr to table to unpack
	 * @param dst          ptr to output array
	 * @param numValues   number of values to unpack
	 */
	private fun unpackVqShape(startVal: Int, shapeVec: IntArray, dst: IntArray, numValues: Int) {
		if (numValues > 0) {
			dst[0] = startVal
			dst[1] = startVal
			dst[2] = startVal
			for (i in 3 until numValues) {
				dst[i] = startVal - shapeVec[atrac3p_qu_num_to_seg[i] - 1]
			}
		}
	}

	private fun unpackSfVqShape(dst: IntArray, numValues: Int) {
		val startVal = br.read(6)
		unpackVqShape(startVal, atrac3p_sf_shapes[br.read(6)], dst, numValues)
	}

	/**
	 * Add weighting coefficients to the decoded word-length information.
	 *
	 * @param[in,out] chan          ptr to the channel parameters
	 * @param[in]     wtab_idx      index of the table of weights
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun addWordlenWeights(chan: Channel, weightIdx: Int): Int {
		val weigthsTab = atrac3p_wl_weights[chan.chNum * 3 + weightIdx - 1]

		for (i in 0 until ctx.numQuantUnits) {
			chan.quWordlen[i] += weigthsTab[i]
			if (chan.quWordlen[i] < 0 || chan.quWordlen[i] > 7) {
				log.error { "WL index out of range pos=%d, val=%d".format(i, chan.quWordlen[i]) }
				return AT3P_ERROR
			}
		}

		return 0
	}

	/**
	 * Decode word length for each quantization unit of a channel.
	 *
	 * @param[in]     chNum        channel to process
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun decodeChannelWordlen(chNum: Int): Int {
		val ret: Int
		val chan = ctx.channels[chNum]
		val refChan = ctx.channels[0]
		var weightIdx = 0

		chan.fillMode = 0

		when (br.read(2)) {
		// switch according to coding mode
			0 // coded using constant number of bits
			-> for (i in 0 until ctx.numQuantUnits) {
				chan.quWordlen[i] = br.read(3)
			}
			1 -> if (chNum > 0) {
				ret = numCodedUnits(chan)
				if (ret < 0) {
					return ret
				}

				if (chan.numCodedVals > 0) {
					val vlcTab = wl_vlc_tabs[br.read(2)]

					for (i in 0 until chan.numCodedVals) {
						val delta = vlcTab.getVLC2(br)
						chan.quWordlen[i] = refChan.quWordlen[i] + delta and 7
					}
				}
			} else {
				weightIdx = br.read(2)
				ret = numCodedUnits(chan)
				if (ret < 0) {
					return ret
				}

				if (chan.numCodedVals > 0) {
					val pos = br.read(5)
					if (pos > chan.numCodedVals) {
						log.error { "WL mode 1: invalid position %d".format(pos) }
						return AT3P_ERROR
					}

					val deltaBits = br.read(2)
					val minVal = br.read(3)

					for (i in 0 until pos) {
						chan.quWordlen[i] = br.read(3)
					}

					for (i in pos until chan.numCodedVals) {
						chan.quWordlen[i] = minVal + getDelta(deltaBits) and 7
					}
				}
			}
			2 -> {
				ret = numCodedUnits(chan)
				if (ret < 0) {
					return ret
				}

				if (chNum > 0 && chan.numCodedVals > 0) {
					val vlcTab = wl_vlc_tabs[br.read(2)]!!
					var delta = vlcTab.getVLC2(br)
					chan.quWordlen[0] = refChan.quWordlen[0] + delta and 7

					for (i in 1 until chan.numCodedVals) {
						val diff = refChan.quWordlen[i] - refChan.quWordlen[i - 1]
						delta = vlcTab.getVLC2(br)
						chan.quWordlen[i] = chan.quWordlen[i - 1] + diff + delta and 7
					}
				} else if (chan.numCodedVals > 0) {
					val flag = br.readBool()
					val vlcTab = wl_vlc_tabs[br.read(1)]!!

					val startVal = br.read(3)
					unpackVqShape(startVal, atrac3p_wl_shapes[startVal][br.read(4)], chan.quWordlen, chan.numCodedVals)

					if (!flag) {
						for (i in 0 until chan.numCodedVals) {
							val delta = vlcTab.getVLC2(br)
							chan.quWordlen[i] = chan.quWordlen[i] + delta and 7
						}
					} else {
						var i: Int
						i = 0
						while (i < chan.numCodedVals and -2) {
							if (!br.readBool()) {
								chan.quWordlen[i] = chan.quWordlen[i] + vlcTab.getVLC2(br) and 7
								chan.quWordlen[i + 1] = chan.quWordlen[i + 1] + vlcTab.getVLC2(br) and 7
							}
							i += 2
						}

						if (chan.numCodedVals and 1 != 0) {
							chan.quWordlen[i] = chan.quWordlen[i] + vlcTab.getVLC2(br) and 7
						}
					}
				}
			}
			3 -> {
				weightIdx = br.read(2)
				ret = numCodedUnits(chan)
				if (ret < 0) {
					return ret
				}

				if (chan.numCodedVals > 0) {
					val vlcTab = wl_vlc_tabs[br.read(2)]!!

					// first coefficient is coded directly
					chan.quWordlen[0] = br.read(3)

					for (i in 1 until chan.numCodedVals) {
						val delta = vlcTab.getVLC2(br)
						chan.quWordlen[i] = chan.quWordlen[i - 1] + delta and 7
					}
				}
			}
		}

		if (chan.fillMode == 2) {
			for (i in chan.numCodedVals until ctx.numQuantUnits) {
				chan.quWordlen[i] = if (chNum > 0) br.read1() else 1
			}
		} else if (chan.fillMode == 3) {
			val pos = if (chNum > 0) chan.numCodedVals + chan.splitPoint else ctx.numQuantUnits - chan.splitPoint
			for (i in chan.numCodedVals until pos) {
				chan.quWordlen[i] = 1
			}
		}

		return if (weightIdx != 0) {
			addWordlenWeights(chan, weightIdx)
		} else 0

	}

	/**
	 * Subtract weighting coefficients from decoded scalefactors.
	 *
	 * @param[in,out] chan          ptr to the channel parameters
	 * @param[in]     wtab_idx      index of table of weights
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun substractSfWeights(chan: Channel, wtabIdx: Int): Int {
		val weigthsTab = Atrac3plusData2.atrac3p_sf_weights[wtabIdx - 1]

		for (i in 0 until ctx.usedQuantUnits) {
			chan.quSfIdx[i] -= weigthsTab[i]
			if (chan.quSfIdx[i] < 0 || chan.quSfIdx[i] > 63) {
				log.error { "SF index out of range pos=%d, val=%d".format(i, chan.quSfIdx[i]) }
				return AT3P_ERROR
			}
		}

		return 0
	}

	/**
	 * Decode scale factor indexes for each quant unit of a channel.
	 *
	 * @param[in]     chNum        channel to process
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun decodeChannelSfIdx(chNum: Int): Int {
		val chan = ctx.channels[chNum]
		val refChan = ctx.channels[0]
		var weightIdx = 0

		chan.fillMode = 0

		when (br.read(2)) {
		// switch according to coding mode
			0 // coded using constant number of bits
			-> for (i in 0 until ctx.usedQuantUnits) {
				chan.quSfIdx[i] = br.read(6)
			}
			1 -> if (chNum > 0) {
				val vlcTab = sf_vlc_tabs[br.read(2)]!!

				for (i in 0 until ctx.usedQuantUnits) {
					val delta = vlcTab.getVLC2(br)
					chan.quSfIdx[i] = refChan.quSfIdx[i] + delta and 0x3F
				}
			} else {
				weightIdx = br.read(2)
				if (weightIdx == 3) {
					unpackSfVqShape(chan.quSfIdx, ctx.usedQuantUnits)

					val numLongVals = br.read(5)
					val deltaBits = br.read(2)
					val minVal = br.read(4) - 7

					for (i in 0 until numLongVals) {
						chan.quSfIdx[i] = chan.quSfIdx[i] + br.read(4) - 7 and 0x3F
					}

					// All others are: minVal + delta
					for (i in numLongVals until ctx.usedQuantUnits) {
						chan.quSfIdx[i] = chan.quSfIdx[i] + minVal + getDelta(deltaBits) and 0x3F
					}
				} else {
					val numLongVals = br.read(5)
					val deltaBits = br.read(3)
					val minVal = br.read(6)
					if (numLongVals > ctx.usedQuantUnits || deltaBits == 7) {
						log.error("SF mode 1: invalid parameters".format())
						return AT3P_ERROR
					}

					// Read full-precision SF indexes
					for (i in 0 until numLongVals) {
						chan.quSfIdx[i] = br.read(6)
					}

					// All others are: minVal + delta
					for (i in numLongVals until ctx.usedQuantUnits) {
						chan.quSfIdx[i] = minVal + getDelta(deltaBits) and 0x3F
					}
				}
			}
			2 -> if (chNum > 0) {
				val vlcTab = sf_vlc_tabs[br.read(2)]!!

				var delta = vlcTab.getVLC2(br)
				chan.quSfIdx[0] = refChan.quSfIdx[0] + delta and 0x3F

				for (i in 1 until ctx.usedQuantUnits) {
					val diff = refChan.quSfIdx[i] - refChan.quSfIdx[i - 1]
					delta = vlcTab.getVLC2(br)
					chan.quSfIdx[i] = chan.quSfIdx[i - 1] + diff + delta and 0x3F
				}
			} else if (chan.numCodedVals > 0) {
				val vlcTab = sf_vlc_tabs[br.read(2) + 4]

				unpackSfVqShape(chan.quSfIdx, ctx.usedQuantUnits)

				for (i in 0 until ctx.usedQuantUnits) {
					val delta = vlcTab!!.getVLC2(br)
					chan.quSfIdx[i] = chan.quSfIdx[i] + delta.signExtend(4) and 0x3F
				}
			}
			3 -> if (chNum > 0) {
				// Copy coefficients from reference channel
				for (i in 0 until ctx.usedQuantUnits) {
					chan.quSfIdx[i] = refChan.quSfIdx[i]
				}
			} else {
				weightIdx = br.read(2)
				val vlcSel = br.read(2)
				var vlcTab = sf_vlc_tabs[vlcSel]!!

				if (weightIdx == 3) {
					vlcTab = sf_vlc_tabs[vlcSel + 4]!!

					unpackSfVqShape(chan.quSfIdx, ctx.usedQuantUnits)

					var diff = br.read(4) + 56 and 0x3F
					chan.quSfIdx[0] = chan.quSfIdx[0] + diff and 0x3F

					for (i in 1 until ctx.usedQuantUnits) {
						val delta = vlcTab.getVLC2(br)
						diff = diff + delta.signExtend(4) and 0x3F
						chan.quSfIdx[i] = diff + chan.quSfIdx[i] and 0x3F
					}
				} else {
					// 1st coefficient is coded directly
					chan.quSfIdx[0] = br.read(6)

					for (i in 1 until ctx.usedQuantUnits) {
						val delta = vlcTab.getVLC2(br)
						chan.quSfIdx[i] = chan.quSfIdx[i - 1] + delta and 0x3F
					}
				}
			}
		}

		return if (weightIdx != 0 && weightIdx < 3) {
			substractSfWeights(chan, weightIdx)
		} else 0

	}

	/**
	 * Decode word length information for each channel.
	 *
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun decodeQuantWordlen(): Int {
		for (chNum in 0 until numChannels) {
			ctx.channels[chNum].quWordlen.fill(0)

			val ret = decodeChannelWordlen(chNum)
			if (ret < 0) {
				return ret
			}
		}

		/* scan for last non-zero coeff in both channels and
	     * set number of quant units having coded spectrum */
		var i: Int
		i = ctx.numQuantUnits - 1
		while (i >= 0) {
			if (ctx.channels[0].quWordlen[i] != 0 || numChannels == 2 && ctx.channels[1].quWordlen[i] != 0) {
				break
			}
			i--
		}
		ctx.usedQuantUnits = i + 1

		return 0
	}

	private fun decodeScaleFactors(): Int {
		if (ctx.usedQuantUnits == 0) {
			return 0
		}

		for (chNum in 0 until numChannels) {
			ctx.channels[chNum].quSfIdx.fill(0)

			val ret = decodeChannelSfIdx(chNum)
			if (ret < 0) {
				return ret
			}
		}

		return 0
	}

	/**
	 * Decode code table indexes for each quant unit of a channel.
	 *
	 * @param[in]     chNum        channel to process
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun decodeChannelCodeTab(chNum: Int): Int {
		val vlcTab: VLC
		val numVals: Int
		val mask = if (ctx.useFullTable) 7 else 3 // mask for modular arithmetic
		val chan = ctx.channels[chNum]
		val refChan = ctx.channels[0]

		chan.tableType = br.read(1)

		when (br.read(2)) {
		// switch according to coding mode
			0 // directly coded
			-> {
				val numBits = if (ctx.useFullTable) 3 else 2
				numVals = numCtValues
				if (numVals < 0) {
					return numVals
				}
				for (i in 0 until numVals) {
					if (chan.quWordlen[i] != 0) {
						chan.quTabIdx[i] = br.read(numBits)
					} else if (chNum > 0 && refChan.quWordlen[i] != 0) {
						// get clone master flag
						chan.quTabIdx[i] = br.read1()
					}
				}
			}
			1 // entropy-coded
			-> {
				vlcTab = if (ctx.useFullTable) ct_vlc_tabs[1]!! else ct_vlc_tabs[0]!!
				numVals = numCtValues
				if (numVals < 0) {
					return numVals
				}
				for (i in 0 until numVals) {
					if (chan.quWordlen[i] != 0) {
						chan.quTabIdx[i] = vlcTab.getVLC2(br)
					} else if (chNum > 0 && refChan.quWordlen[i] != 0) {
						// get clone master flag
						chan.quTabIdx[i] = br.read1()
					}
				}
			}
			2 // entropy-coded delta
			-> {
				val deltaVlc: VLC
				if (ctx.useFullTable) {
					vlcTab = ct_vlc_tabs[1]!!
					deltaVlc = ct_vlc_tabs[2]!!
				} else {
					vlcTab = ct_vlc_tabs[0]!!
					deltaVlc = ct_vlc_tabs[0]!!
				}
				var pred = 0
				numVals = numCtValues
				if (numVals < 0) {
					return numVals
				}
				for (i in 0 until numVals) {
					if (chan.quWordlen[i] != 0) {
						chan.quTabIdx[i] = if (i == 0) vlcTab.getVLC2(br) else pred + deltaVlc.getVLC2(br) and mask
						pred = chan.quTabIdx[i]
					} else if (chNum > 0 && refChan.quWordlen[i] != 0) {
						// get clone master flag
						chan.quTabIdx[i] = br.read1()
					}
				}
			}
			3 // entropy-coded difference to master
			-> if (chNum > 0) {
				vlcTab = if (ctx.useFullTable) ct_vlc_tabs[3] else ct_vlc_tabs[0]
				numVals = numCtValues
				if (numVals < 0) {
					return numVals
				}
				for (i in 0 until numVals) {
					if (chan.quWordlen[i] != 0) {
						chan.quTabIdx[i] = refChan.quTabIdx[i] + vlcTab.getVLC2(br) and mask
					} else if (chNum > 0 && refChan.quWordlen[i] != 0) {
						// get clone master flag
						chan.quTabIdx[i] = br.read1()
					}
				}
			}
		}

		return 0
	}

	/**
	 * Decode code table indexes for each channel.
	 *
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun decodeCodeTableIndexes(): Int {
		if (ctx.usedQuantUnits == 0) {
			return 0
		}

		ctx.useFullTable = br.readBool()

		for (chNum in 0 until numChannels) {
			ctx.channels[chNum].quTabIdx.fill(0)

			val ret = decodeChannelCodeTab(chNum)
			if (ret < 0) {
				return ret
			}
		}

		return 0
	}

	private fun decodeQuSpectra(tab: Atrac3plusData1.Atrac3pSpecCodeTab, vlcTab: VLC, out: IntArray, outOffset: Int, numSpecs: Int) {
		val groupSize = tab.groupSize
		val numCoeffs = tab.numCoeffs
		val bits = tab.bits
		val isSigned = tab.isSigned
		val mask = (1 shl bits) - 1

		var pos = 0
		while (pos < numSpecs) {
			if (groupSize == 1 || br.readBool()) {
				for (j in 0 until groupSize) {
					var `val` = vlcTab.getVLC2(br)

					for (i in 0 until numCoeffs) {
						var cf = `val` and mask
						if (isSigned) {
							cf = cf.signExtend(bits)
						} else if (cf != 0 && br.readBool()) {
							cf = -cf
						}

						out[outOffset + pos] = cf
						pos++
						`val` = `val` shr bits
					}
				}
			} else {
				// Group skipped
				pos += groupSize * numCoeffs
			}
		}
	}

	private fun decodeSpectrum() {
		for (chNum in 0 until numChannels) {
			val chan = ctx.channels[chNum]

			chan.spectrum.fill(0)

			chan.powerLevs.fill(ATRAC3P_POWER_COMP_OFF)

			for (qu in 0 until ctx.usedQuantUnits) {
				val numSpecs = ff_atrac3p_qu_to_spec_pos[qu + 1] - ff_atrac3p_qu_to_spec_pos[qu]
				val wordlen = chan.quWordlen[qu]
				var codetab = chan.quTabIdx[qu]
				if (wordlen > 0) {
					if (!ctx.useFullTable) {
						codetab = atrac3p_ct_restricted_to_full[chan.tableType][wordlen - 1][codetab]
					}

					var tabIndex = (chan.tableType * 8 + codetab) * 7 + wordlen - 1
					val tab = atrac3p_spectra_tabs[tabIndex]

					if (tab.redirect >= 0) {
						tabIndex = tab.redirect
					}

					decodeQuSpectra(tab, spec_vlc_tabs[tabIndex]!!, chan.spectrum, ff_atrac3p_qu_to_spec_pos[qu], numSpecs)
				} else if (chNum > 0 && ctx.channels[0].quWordlen[qu] != 0 && codetab == 0) {
					// Copy coefficients from master
					arraycopy(ctx.channels[0].spectrum, ff_atrac3p_qu_to_spec_pos[qu], chan.spectrum, ff_atrac3p_qu_to_spec_pos[qu], numSpecs)
					chan.quWordlen[qu] = ctx.channels[0].quWordlen[qu]
				}
			}

			/* Power compensation levels only present in the bitstream
	         * if there are more than 2 quant units. The lowest two units
	         * correspond to the frequencies 0...351 Hz, whose shouldn't
	         * be affected by the power compensation. */
			if (ctx.usedQuantUnits > 2) {
				val numSpecs = atrac3p_subband_to_num_powgrps[ctx.numCodedSubbands - 1]
				for (i in 0 until numSpecs) {
					chan.powerLevs[i] = br.read(4)
				}
			}
		}
	}

	private fun getSubbandFlags(out: BooleanArray, numFlags: Int): Boolean {
		val result = br.readBool()
		if (result) {
			if (br.readBool()) {
				for (i in 0 until numFlags) {
					out[i] = br.readBool()
				}
			} else {
				for (i in 0 until numFlags) {
					out[i] = true
				}
			}
		} else {
			for (i in 0 until numFlags) {
				out[i] = false
			}
		}

		return result
	}

	/**
	 * Decode mdct window shape flags for all channels.
	 *
	 */
	private fun decodeWindowShape() {
		for (i in 0 until numChannels) {
			getSubbandFlags(ctx.channels[i].wndShape, ctx.numSubbands)
		}
	}

	private fun decodeGaincNPoints(chNum: Int, codedSubbands: Int): Int {
		val chan = ctx.channels[chNum]
		val refChan = ctx.channels[0]

		when (br.read(2)) {
		// switch according to coding mode
			0 // fixed-length coding
			-> for (i in 0 until codedSubbands) {
				chan.gainData[i].numPoints = br.read(3)
			}
			1 // variable-length coding
			-> for (i in 0 until codedSubbands) {
				chan.gainData[i].numPoints = gain_vlc_tabs[0].getVLC2(br)
			}
			2 -> if (chNum > 0) { // VLC modulo delta to master channel
				for (i in 0 until codedSubbands) {
					val delta = gain_vlc_tabs[1].getVLC2(br)
					chan.gainData[i].numPoints = refChan.gainData[i].numPoints + delta and 7
				}
			} else { // VLC modulo delta to previous
				chan.gainData[0].numPoints = gain_vlc_tabs[0].getVLC2(br)

				for (i in 1 until codedSubbands) {
					val delta = gain_vlc_tabs[1].getVLC2(br)
					chan.gainData[i].numPoints = chan.gainData[i - 1].numPoints + delta and 7
				}
			}
			3 -> if (chNum > 0) { // copy data from master channel
				for (i in 0 until codedSubbands) {
					chan.gainData[i].numPoints = refChan.gainData[i].numPoints
				}
			} else { // shorter delta to min
				val deltaBits = br.read(2)
				val minVal = br.read(3)

				for (i in 0 until codedSubbands) {
					chan.gainData[i].numPoints = minVal + getDelta(deltaBits)
					if (chan.gainData[i].numPoints > 7) {
						return AT3P_ERROR
					}
				}
			}
		}

		return 0
	}

	/**
	 * Implements coding mode 1 (master) for gain compensation levels.
	 *
	 * @param[out]    dst    ptr to the output array
	 */
	private fun gaincLevelMode1m(dst: AtracGainInfo) {
		if (dst.numPoints > 0) {
			dst.levCode[0] = gain_vlc_tabs[2].getVLC2(br)
		}

		for (i in 1 until dst.numPoints) {
			val delta = gain_vlc_tabs[3].getVLC2(br)
			dst.levCode[i] = dst.levCode[i - 1] + delta and 0xF
		}
	}

	/**
	 * Implements coding mode 3 (slave) for gain compensation levels.
	 *
	 * @param[out]   dst   ptr to the output array
	 * @param[in]    ref   ptr to the reference channel
	 */
	private fun gaincLevelMode3s(dst: AtracGainInfo, ref: AtracGainInfo) {
		for (i in 0 until dst.numPoints) {
			dst.levCode[i] = if (i >= ref.numPoints) 7 else ref.levCode[i]
		}
	}

	/**
	 * Decode level code for each gain control point.
	 *
	 * @param[in]     ch_num          channel to process
	 * @param[in]     coded_subbands  number of subbands to process
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun decodeGaincLevels(chNum: Int, codedSubbands: Int): Int {
		val chan = ctx.channels[chNum]
		val refChan = ctx.channels[0]

		when (br.read(2)) {
		// switch according to coding mode
			0 // fixed-length coding
			-> for (sb in 0 until codedSubbands) {
				for (i in 0 until chan.gainData[sb].numPoints) {
					chan.gainData[sb].levCode[i] = br.read(4)
				}
			}
			1 -> if (chNum > 0) { // VLC module delta to master channel
				for (sb in 0 until codedSubbands) {
					for (i in 0 until chan.gainData[sb].numPoints) {
						val delta = gain_vlc_tabs[5].getVLC2(br)
						val pred = if (i >= refChan.gainData[sb].numPoints) 7 else refChan.gainData[sb].levCode[i]
						chan.gainData[sb].levCode[i] = pred + delta and 0xF
					}
				}
			} else { // VLC module delta to previous
				for (sb in 0 until codedSubbands) {
					gaincLevelMode1m(chan.gainData[sb])
				}
			}
			2 -> if (chNum > 0) { // VLC modulo delta to previous or clone master
				for (sb in 0 until codedSubbands) {
					if (chan.gainData[sb].numPoints > 0) {
						if (br.readBool()) {
							gaincLevelMode1m(chan.gainData[sb])
						} else {
							gaincLevelMode3s(chan.gainData[sb], refChan.gainData[sb])
						}
					}
				}
			} else { // VLC modulo delta to lev_codes of previous subband
				if (chan.gainData[0].numPoints > 0) {
					gaincLevelMode1m(chan.gainData[0])
				}

				for (sb in 1 until codedSubbands) {
					for (i in 0 until chan.gainData[sb].numPoints) {
						val delta = gain_vlc_tabs[4].getVLC2(br)
						val pred = if (i >= chan.gainData[sb - 1].numPoints) 7 else chan.gainData[sb - 1].levCode[i]
						chan.gainData[sb].levCode[i] = pred + delta and 0xF
					}
				}
			}
			3 -> if (chNum > 0) { // clone master
				for (sb in 0 until codedSubbands) {
					gaincLevelMode3s(chan.gainData[sb], refChan.gainData[sb])
				}
			} else { // shorter delta to min
				val deltaBits = br.read(2)
				val minVal = br.read(4)

				for (sb in 0 until codedSubbands) {
					for (i in 0 until chan.gainData[sb].numPoints) {
						chan.gainData[sb].levCode[i] = minVal + getDelta(deltaBits)
						if (chan.gainData[sb].levCode[i] > 15) {
							return AT3P_ERROR
						}
					}
				}
			}
		}

		return 0
	}

	/**
	 * Implements coding mode 0 for gain compensation locations.
	 *
	 * @param[out]    dst    ptr to the output array
	 * @param[in]     pos    position of the value to be processed
	 */
	private fun gaincLocMode0(dst: AtracGainInfo, pos: Int) {
		if (pos == 0 || dst.locCode[pos - 1] < 15) {
			dst.locCode[pos] = br.read(5)
		} else if (dst.locCode[pos - 1] >= 30) {
			dst.locCode[pos] = 31
		} else {
			val deltaBits = avLog2(30 - dst.locCode[pos - 1]) + 1
			dst.locCode[pos] = dst.locCode[pos - 1] + br.read(deltaBits) + 1
		}
	}

	/**
	 * Implements coding mode 1 for gain compensation locations.
	 *
	 * @param[out]    dst    ptr to the output array
	 */
	private fun gaincLocMode1(dst: AtracGainInfo) {
		if (dst.numPoints > 0) {
			// 1st coefficient is stored directly
			dst.locCode[0] = br.read(5)

			for (i in 1 until dst.numPoints) {
				// Switch VLC according to the curve direction
				// (ascending/descending)
				val tab = if (dst.levCode[i] <= dst.levCode[i - 1]) gain_vlc_tabs[7] else gain_vlc_tabs[9]
				dst.locCode[i] = dst.locCode[i - 1] + tab.getVLC2(br)
			}
		}
	}

	/**
	 * Decode location code for each gain control point.
	 *
	 * @param[in]     chNum          channel to process
	 * @param[in]     codedSubbands  number of subbands to process
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun decodeGaincLocCodes(chNum: Int, codedSubbands: Int): Int {
		val chan = ctx.channels[chNum]
		val refChan = ctx.channels[0]

		val codingMode = br.read(2)
		when (codingMode) {
		// switch according to coding mode
			0 // sequence of numbers in ascending order
			-> for (sb in 0 until codedSubbands) {
				for (i in 0 until chan.gainData[sb].numPoints) {
					gaincLocMode0(chan.gainData[sb], i)
				}
			}
			1 -> if (chNum > 0) {
				for (sb in 0 until codedSubbands) {
					if (chan.gainData[sb].numPoints <= 0) {
						continue
					}
					val dst = chan.gainData[sb]
					val ref = refChan.gainData[sb]

					// 1st value is vlc-coded modulo delta to master
					var delta = gain_vlc_tabs[10].getVLC2(br)
					val pred = if (ref.numPoints > 0) ref.locCode[0] else 0
					dst.locCode[0] = pred + delta and 0x1F

					for (i in 1 until dst.numPoints) {
						val moreThanRef = i >= ref.numPoints
						if (dst.levCode[i] > dst.levCode[i - 1]) {
							// ascending curve
							if (moreThanRef) {
								delta = gain_vlc_tabs[9].getVLC2(br)
								dst.locCode[i] = dst.locCode[i - 1] + delta
							} else {
								if (br.readBool()) {
									gaincLocMode0(dst, i) // direct coding
								} else {
									dst.locCode[i] = ref.locCode[i] // clone master
								}
							}
						} else { // descending curve
							val tab = if (moreThanRef) gain_vlc_tabs[7]!! else gain_vlc_tabs[10]!!
							delta = tab.getVLC2(br)
							if (moreThanRef) {
								dst.locCode[i] = dst.locCode[i - 1] + delta
							} else {
								dst.locCode[i] = ref.locCode[i] + delta and 0x1F
							}
						}
					}
				}
			} else { // VLC delta to previous
				for (sb in 0 until codedSubbands) {
					gaincLocMode1(chan.gainData[sb])
				}
			}
			2 -> if (chNum > 0) {
				for (sb in 0 until codedSubbands) {
					if (chan.gainData[sb].numPoints <= 0) {
						continue
					}
					val dst = chan.gainData[sb]
					val ref = refChan.gainData[sb]
					if (dst.numPoints > ref.numPoints || br.readBool()) {
						gaincLocMode1(dst)
					} else { // clone master for the whole subband
						for (i in 0 until chan.gainData[sb].numPoints) {
							dst.locCode[i] = ref.locCode[i]
						}
					}
				}
			} else {
				// data for the first subband is coded directly
				for (i in 0 until chan.gainData[0].numPoints) {
					gaincLocMode0(chan.gainData[0], i)
				}

				for (sb in 1 until codedSubbands) {
					if (chan.gainData[sb].numPoints <= 0) {
						continue
					}
					val dst = chan.gainData[sb]

					// 1st value is vlc-coded modulo delta to the corresponding
					// value of the previous subband if any or zero
					var delta = gain_vlc_tabs[6].getVLC2(br)
					val pred = if (chan.gainData[sb - 1].numPoints > 0) chan.gainData[sb - 1].locCode[0] else 0
					dst.locCode[0] = pred + delta and 0x1F

					for (i in 1 until dst.numPoints) {
						val moreThanRef = i >= chan.gainData[sb - 1].numPoints
						// Select VLC table according to curve direction and
						// presence of prediction
						val tab = gain_vlc_tabs[(if (dst.levCode[i] > dst.levCode[i - 1]) 2 else 0) + (if (moreThanRef) 1 else 0) + 6]!!
						delta = tab.getVLC2(br)
						if (moreThanRef) {
							dst.locCode[i] = dst.locCode[i - 1] + delta
						} else {
							dst.locCode[i] = chan.gainData[sb - 1].locCode[i] + delta and 0x1F
						}
					}
				}
			}
			3 -> if (chNum > 0) { // clone master or direct or direct coding
				for (sb in 0 until codedSubbands) {
					for (i in 0 until chan.gainData[sb].numPoints) {
						if (i >= refChan.gainData[sb].numPoints) {
							gaincLocMode0(chan.gainData[sb], i)
						} else {
							chan.gainData[sb].locCode[i] = refChan.gainData[sb].locCode[i]
						}
					}
				}
			} else { // shorter delta to min
				val deltaBits = br.read(2) + 1
				val minVal = br.read(5)

				for (sb in 0 until codedSubbands) {
					for (i in 0 until chan.gainData[sb].numPoints) {
						chan.gainData[sb].locCode[i] = minVal + i + br.read(deltaBits)
					}
				}
			}
		}

		// Validate decoded information
		for (sb in 0 until codedSubbands) {
			val dst = chan.gainData[sb]
			for (i in 0 until chan.gainData[sb].numPoints) {
				if (dst.locCode[i] < 0 || dst.locCode[i] > 31 || i > 0 && dst.locCode[i] <= dst.locCode[i - 1]) {
					log.error("Invalid gain location: ch=%d, sb=%d, pos=%d, val=%d".format(chNum, sb, i, dst.locCode[i]))
					return AT3P_ERROR
				}
			}
		}

		return 0
	}

	/**
	 * Decode gain control data for all channels.
	 *
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun decodeGaincData(): Int {
		var ret: Int

		for (chNum in 0 until numChannels) {
			for (i in 0 until ATRAC3P_SUBBANDS) {
				ctx.channels[chNum].gainData[i].clear()
			}

			if (br.readBool()) { // gain control data present?
				val codedSubbands = br.read(4) + 1
				if (br.readBool()) { // is high band gain data replication on?
					ctx.channels[chNum].numGainSubbands = br.read(4) + 1
				} else {
					ctx.channels[chNum].numGainSubbands = codedSubbands
				}

				ret = decodeGaincNPoints(chNum, codedSubbands)
				if (ret < 0) {
					return ret
				}
				ret = decodeGaincLevels(chNum, codedSubbands)
				if (ret < 0) {
					return ret
				}
				ret = decodeGaincLocCodes(chNum, codedSubbands)
				if (ret < 0) {
					return ret
				}

				if (codedSubbands > 0) { // propagate gain data if requested
					for (sb in codedSubbands until ctx.channels[chNum].numGainSubbands) {
						ctx.channels[chNum].gainData[sb].copy(ctx.channels[chNum].gainData[sb - 1])
					}
				}
			} else {
				ctx.channels[chNum].numGainSubbands = 0
			}
		}

		return 0
	}

	/**
	 * Decode envelope for all tones of a channel.
	 *
	 * @param[in]     chNum           channel to process
	 * @param[in]     bandHasTones    ptr to an array of per-band-flags:
	 * 1 - tone data present
	 */
	private fun decodeTonesEnvelope(chNum: Int, bandHasTones: BooleanArray) {
		val dst = ctx.channels[chNum].tonesInfo
		val ref = ctx.channels[0].tonesInfo

		if (chNum == 0 || !br.readBool()) { // mode 0: fixed-length coding
			for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (!bandHasTones[sb]) {
					continue
				}
				dst[sb].pendEnv.hasStartPoint = br.readBool()
				dst[sb].pendEnv.startPos = if (dst[sb].pendEnv.hasStartPoint) br.read(5) else -1
				dst[sb].pendEnv.hasStopPoint = br.readBool()
				dst[sb].pendEnv.stopPos = if (dst[sb].pendEnv.hasStopPoint) br.read(5) else 32
			}
		} else { // mode 1(slave only): copy master
			for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (!bandHasTones[sb]) {
					continue
				}
				dst[sb].pendEnv.copy(ref[sb].pendEnv)
			}
		}
	}

	/**
	 * Decode number of tones for each subband of a channel.
	 *
	 * @param[in]     chNum           channel to process
	 * @param[in]     bandHasTones    ptr to an array of per-band-flags:
	 * 1 - tone data present
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun decodeBandNumwavs(chNum: Int, bandHasTones: BooleanArray): Int {
		val dst = ctx.channels[chNum].tonesInfo
		val ref = ctx.channels[0].tonesInfo

		val mode = br.read(chNum + 1)
		when (mode) {
			0 // fixed-length coding
			-> for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (bandHasTones[sb]) {
					dst[sb].numWavs = br.read(4)
				}
			}
			1 // variable-length coding
			-> for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (bandHasTones[sb]) {
					dst[sb].numWavs = tone_vlc_tabs[1].getVLC2(br)
				}
			}
			2 // VLC modulo delta to master (slave only)
			-> for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (bandHasTones[sb]) {
					var delta = tone_vlc_tabs[2].getVLC2(br)
					delta = delta.signExtend(3)
					dst[sb].numWavs = ref[sb].numWavs + delta and 0xF
				}
			}
			3 // copy master (slave only)
			-> for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (bandHasTones[sb]) {
					dst[sb].numWavs = ref[sb].numWavs
				}
			}
		}

		// initialize start tone index for each subband
		for (sb in 0 until ctx.wavesInfo.numToneBands) {
			if (bandHasTones[sb]) {
				if (ctx.wavesInfo.tonesIndex + dst[sb].numWavs > 48) {
					log.error("Too many tones: %d (max. 48)".format(ctx.wavesInfo.tonesIndex + dst[sb].numWavs))
					return AT3P_ERROR
				}
				dst[sb].startIndex = ctx.wavesInfo.tonesIndex
				ctx.wavesInfo.tonesIndex += dst[sb].numWavs
			}
		}

		return 0
	}

	/**
	 * Decode frequency information for each subband of a channel.
	 *
	 * @param[in]     chNum           channel to process
	 * @param[in]     bandHasTones    ptr to an array of per-band-flags:
	 * 1 - tone data present
	 */
	private fun decodeTonesFrequency(chNum: Int, bandHasTones: BooleanArray) {
		val dst = ctx.channels[chNum].tonesInfo
		val ref = ctx.channels[0].tonesInfo

		if (chNum == 0 || !br.readBool()) { // mode 0: fixed-length coding
			for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
					continue
				}
				val iwav = dst[sb].startIndex
				val direction = if (dst[sb].numWavs > 1) br.readBool() else false
				if (direction) { // packed numbers in descending order
					if (dst[sb].numWavs > 0) {
						ctx.wavesInfo.waves[iwav + dst[sb].numWavs - 1]!!.freqIndex = br.read(10)
					}
					for (i in dst[sb].numWavs - 2 downTo 0) {
						val nbits = avLog2(ctx.wavesInfo.waves[iwav + i + 1]!!.freqIndex) + 1
						ctx.wavesInfo.waves[iwav + i]!!.freqIndex = br.read(nbits)
					}
				} else { // packed numbers in ascending order
					for (i in 0 until dst[sb].numWavs) {
						if (i == 0 || ctx.wavesInfo.waves[iwav + i - 1]!!.freqIndex < 512) {
							ctx.wavesInfo.waves[iwav + i]!!.freqIndex = br.read(10)
						} else {
							val nbits = avLog2(1023 - ctx.wavesInfo.waves[iwav + i - 1]!!.freqIndex) + 1
							ctx.wavesInfo.waves[iwav + i]!!.freqIndex = br.read(nbits) + 1024 - (1 shl nbits)
						}
					}
				}
			}
		} else { // mode 1: VLC module delta to master (slave only)
			for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
					continue
				}
				val iwav = ref[sb].startIndex
				val owav = dst[sb].startIndex
				for (i in 0 until dst[sb].numWavs) {
					var delta = tone_vlc_tabs[6]!!.getVLC2(br)
					delta = delta.signExtend(8)
					val pred = if (i < ref[sb].numWavs) ctx.wavesInfo.waves[iwav + i]!!.freqIndex else if (ref[sb].numWavs > 0) ctx.wavesInfo.waves[iwav + ref[sb].numWavs - 1]!!.freqIndex else 0
					ctx.wavesInfo.waves[owav + i]!!.freqIndex = pred + delta and 0x3FF
				}
			}
		}
	}

	/**
	 * Decode amplitude information for each subband of a channel.
	 *
	 * @param[in]     chNum           channel to process
	 * @param[in]     bandHasTones    ptr to an array of per-band-flags:
	 * 1 - tone data present
	 */
	private fun decodeTonesAmplitude(chNum: Int, bandHasTones: BooleanArray) {
		val dst = ctx.channels[chNum].tonesInfo
		val ref = ctx.channels[0].tonesInfo
		val refwaves = IntArray(48)

		if (chNum > 0) {
			for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
					continue
				}
				val wsrc = dst[sb].startIndex
				val wref = ref[sb].startIndex
				for (j in 0 until dst[sb].numWavs) {
					var fi = 0
					var maxdiff = 1024
					for (i in 0 until ref[sb].numWavs) {
						val diff = abs(ctx.wavesInfo.waves[wsrc + j]!!.freqIndex - ctx.wavesInfo.waves[wref + i]!!.freqIndex)
						if (diff < maxdiff) {
							maxdiff = diff
							fi = i
						}
					}

					if (maxdiff < 8) {
						refwaves[dst[sb].startIndex + j] = fi + ref[sb].startIndex
					} else if (j < ref[sb].numWavs) {
						refwaves[dst[sb].startIndex + j] = j + ref[sb].startIndex
					} else {
						refwaves[dst[sb].startIndex + j] = -1
					}
				}
			}
		}

		val mode = br.read(chNum + 1)

		when (mode) {
			0 // fixed-length coding
			-> for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
					continue
				}
				if (ctx.wavesInfo.amplitudeMode != 0) {
					for (i in 0 until dst[sb].numWavs) {
						ctx.wavesInfo.waves[dst[sb].startIndex + i]!!.ampSf = br.read(6)
					}
				} else {
					ctx.wavesInfo.waves[dst[sb].startIndex]!!.ampSf = br.read(6)
				}
			}
			1 // min + VLC delta
			-> for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
					continue
				}
				if (ctx.wavesInfo.amplitudeMode != 0) {
					for (i in 0 until dst[sb].numWavs) {
						ctx.wavesInfo.waves[dst[sb].startIndex + i]!!.ampSf = tone_vlc_tabs[3]!!.getVLC2(br) + 20
					}
				} else {
					ctx.wavesInfo.waves[dst[sb].startIndex]!!.ampSf = tone_vlc_tabs[4]!!.getVLC2(br) + 24
				}
			}
			2 // VLC module delta to master (slave only)
			-> for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
					continue
				}
				for (i in 0 until dst[sb].numWavs) {
					var delta = tone_vlc_tabs[5]!!.getVLC2(br)
					delta = delta.signExtend(5)
					val pred = if (refwaves[dst[sb].startIndex + i] >= 0) ctx.wavesInfo.waves[refwaves[dst[sb].startIndex + i]]!!.ampSf else 34
					ctx.wavesInfo.waves[dst[sb].startIndex + i]!!.ampSf = pred + delta and 0x3F
				}
			}
			3 // clone master (slave only)
			-> for (sb in 0 until ctx.wavesInfo.numToneBands) {
				if (!bandHasTones[sb]) {
					continue
				}
				for (i in 0 until dst[sb].numWavs) {
					ctx.wavesInfo.waves[dst[sb].startIndex + i]!!.ampSf = if (refwaves[dst[sb].startIndex + i] >= 0) ctx.wavesInfo.waves[refwaves[dst[sb].startIndex + i]]!!.ampSf else 32
				}
			}
		}
	}

	/**
	 * Decode phase information for each subband of a channel.
	 *
	 * @param     chNnum          channel to process
	 * @param     bandHasTones    ptr to an array of per-band-flags:
	 * 1 - tone data present
	 */
	private fun decodeTonesPhase(chNum: Int, bandHasTones: BooleanArray) {
		val dst = ctx.channels[chNum].tonesInfo

		for (sb in 0 until ctx.wavesInfo.numToneBands) {
			if (!bandHasTones[sb]) {
				continue
			}
			val wparam = dst[sb].startIndex
			for (i in 0 until dst[sb].numWavs) {
				ctx.wavesInfo.waves[wparam + i]!!.phaseIndex = br.read(5)
			}
		}
	}

	/**
	 * Decode tones info for all channels.
	 *
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private fun decodeTonesInfo(): Int {
		for (chNum in 0 until numChannels) {
			for (i in 0 until ATRAC3P_SUBBANDS) {
				ctx.channels[chNum].tonesInfo[i].clear()
			}
		}

		ctx.wavesInfo.tonesPresent = br.readBool()
		if (!ctx.wavesInfo.tonesPresent) {
			return 0
		}

		for (i in ctx.wavesInfo.waves.indices) {
			ctx.wavesInfo.waves[i]!!.clear()
		}

		ctx.wavesInfo.amplitudeMode = br.read1()
		if (ctx.wavesInfo.amplitudeMode == 0) {
			log.error("GHA amplitude mode 0")
			return AT3P_ERROR
		}

		ctx.wavesInfo.numToneBands = tone_vlc_tabs[0].getVLC2(br) + 1

		if (numChannels == 2) {
			getSubbandFlags(ctx.wavesInfo.toneSharing, ctx.wavesInfo.numToneBands)
			getSubbandFlags(ctx.wavesInfo.toneMaster, ctx.wavesInfo.numToneBands)
			if (getSubbandFlags(ctx.wavesInfo.phaseShift, ctx.wavesInfo.numToneBands)) {
				log.warn("GHA Phase shifting")
			}
		}

		ctx.wavesInfo.tonesIndex = 0

		for (chNum in 0 until numChannels) {
			val bandHasTones = BooleanArray(16)
			for (i in 0 until ctx.wavesInfo.numToneBands) {
				bandHasTones[i] = if (chNum == 0) true else !ctx.wavesInfo.toneSharing[i]
			}

			decodeTonesEnvelope(chNum, bandHasTones)
			val ret = decodeBandNumwavs(chNum, bandHasTones)
			if (ret < 0) {
				return ret
			}

			decodeTonesFrequency(chNum, bandHasTones)
			decodeTonesAmplitude(chNum, bandHasTones)
			decodeTonesPhase(chNum, bandHasTones)
		}

		if (numChannels == 2) {
			for (i in 0 until ctx.wavesInfo.numToneBands) {
				if (ctx.wavesInfo.toneSharing[i]) {
					ctx.channels[1].tonesInfo[i].copy(ctx.channels[0].tonesInfo[i])
				}

				if (ctx.wavesInfo.toneMaster[i]) {
					// Swap channels 0 and 1
					val tmp = WavesData()
					tmp.copy(ctx.channels[0].tonesInfo[i])
					ctx.channels[0].tonesInfo[i].copy(ctx.channels[1].tonesInfo[i])
					ctx.channels[1].tonesInfo[i].copy(tmp)
				}
			}
		}

		return 0
	}

	fun decodeResidualSpectrum(out: Array<FloatArray>) {
		val sbRNGindex = IntArray(ATRAC3P_SUBBANDS)

		if (ctx.muteFlag) {
			for (ch in 0 until numChannels) {
				out[ch].fill(0f)
			}
			return
		}

		var RNGindex = 0
		for (qu in 0 until ctx.usedQuantUnits) {
			RNGindex += ctx.channels[0].quSfIdx[qu] + ctx.channels[1].quSfIdx[qu]
		}

		run {
			var sb = 0
			while (sb < ctx.numCodedSubbands) {
				sbRNGindex[sb] = RNGindex and 0x3FC
				sb++
				RNGindex += 128
			}
		}

		// inverse quant and power compensation
		for (ch in 0 until numChannels) {
			// clear channel's residual spectrum
			out[ch].fill(0f, 0, ATRAC3P_FRAME_SAMPLES)

			for (qu in 0 until ctx.usedQuantUnits) {
				val src = ff_atrac3p_qu_to_spec_pos[qu]
				val dst = ff_atrac3p_qu_to_spec_pos[qu]
				val nspeclines = ff_atrac3p_qu_to_spec_pos[qu + 1] - ff_atrac3p_qu_to_spec_pos[qu]

				if (ctx.channels[ch].quWordlen[qu] > 0) {
					val q = ff_atrac3p_sf_tab[ctx.channels[ch].quSfIdx[qu]] * ff_atrac3p_mant_tab[ctx.channels[ch].quWordlen[qu]]
					for (i in 0 until nspeclines) {
						out[ch][dst + i] = ctx.channels[ch].spectrum[src + i] * q
					}
				}
			}

			for (sb in 0 until ctx.numCodedSubbands) {
				dsp!!.powerCompensation(ctx, ch, out[ch], sbRNGindex[sb], sb)
			}
		}

		if (ctx.unitType == CH_UNIT_STEREO) {
			val tmp = FloatArray(ATRAC3P_SUBBAND_SAMPLES)
			for (sb in 0 until ctx.numCodedSubbands) {
				if (ctx.swapChannels[sb]) {
					// Swap both channels
					arraycopy(out[0], sb * ATRAC3P_SUBBAND_SAMPLES, tmp, 0, ATRAC3P_SUBBAND_SAMPLES)
					arraycopy(out[1], sb * ATRAC3P_SUBBAND_SAMPLES, out[0], sb * ATRAC3P_SUBBAND_SAMPLES, ATRAC3P_SUBBAND_SAMPLES)
					arraycopy(tmp, 0, out[1], sb * ATRAC3P_SUBBAND_SAMPLES, ATRAC3P_SUBBAND_SAMPLES)
				}

				// flip coefficients' sign if requested
				if (ctx.negateCoeffs[sb]) {
					for (i in 0 until ATRAC3P_SUBBAND_SAMPLES) {
						out[1][sb * ATRAC3P_SUBBAND_SAMPLES + i] = -out[1][sb * ATRAC3P_SUBBAND_SAMPLES + i]
					}
				}
			}
		}
	}

	fun reconstructFrame(at3pContext: Context) {
		for (ch in 0 until numChannels) {
			for (sb in 0 until ctx.numSubbands) {
				// inverse transform and windowing
				dsp!!.imdct(at3pContext.mdctCtx!!, at3pContext.samples[ch], sb * ATRAC3P_SUBBAND_SAMPLES, at3pContext.mdctBuf[ch], sb * ATRAC3P_SUBBAND_SAMPLES, (if (ctx.channels[ch].wndShapePrev[sb]) 2 else 0) + if (ctx.channels[ch].wndShape[sb]) 1 else 0, sb)

				// gain compensation and overlapping
				at3pContext.gaincCtx!!.gainCompensation(at3pContext.mdctBuf[ch], sb * ATRAC3P_SUBBAND_SAMPLES, ctx.prevBuf[ch], sb * ATRAC3P_SUBBAND_SAMPLES, ctx.channels[ch].gainDataPrev[sb], ctx.channels[ch].gainData[sb], ATRAC3P_SUBBAND_SAMPLES, at3pContext.timeBuf[ch], sb * ATRAC3P_SUBBAND_SAMPLES)
			}

			// zero unused subbands in both output and overlapping buffers
			ctx.prevBuf[ch].fill(0f, ctx.numSubbands * ATRAC3P_SUBBAND_SAMPLES, ctx.prevBuf[ch].size)
			at3pContext.timeBuf[ch].fill(0f, ctx.numSubbands * ATRAC3P_SUBBAND_SAMPLES, at3pContext.timeBuf[ch].size)

			// resynthesize and add tonal signal
			if (ctx.wavesInfo.tonesPresent || ctx.wavesInfoPrev.tonesPresent) {
				for (sb in 0 until ctx.numSubbands) {
					if (ctx.channels[ch].tonesInfo[sb].numWavs > 0 || ctx.channels[ch].tonesInfoPrev[sb].numWavs > 0) {
						dsp!!.generateTones(ctx, ch, sb, at3pContext.timeBuf[ch], sb * 128)
					}
				}
			}

			// subband synthesis and acoustic signal output
			dsp!!.ipqf(at3pContext.ipqfDctCtx!!, ctx.ipqfCtx[ch], at3pContext.timeBuf[ch], at3pContext.outpBuf[ch])
		}

		// swap window shape and gain control buffers
		for (ch in 0 until numChannels) {
			val tmp1 = ctx.channels[ch].wndShape
			ctx.channels[ch].wndShape = ctx.channels[ch].wndShapePrev
			ctx.channels[ch].wndShapePrev = tmp1

			val tmp2 = ctx.channels[ch].gainData
			ctx.channels[ch].gainData = ctx.channels[ch].gainDataPrev
			ctx.channels[ch].gainDataPrev = tmp2

			val tmp3 = ctx.channels[ch].tonesInfo
			ctx.channels[ch].tonesInfo = ctx.channels[ch].tonesInfoPrev
			ctx.channels[ch].tonesInfoPrev = tmp3
		}

		val tmp = ctx.wavesInfo
		ctx.wavesInfo = ctx.wavesInfoPrev
		ctx.wavesInfoPrev = tmp
	}

	companion object {
		private val log = Atrac3plusDecoder.log

		/* build huffman tables for gain data decoding */
		private val gain_cbs = arrayOf<IntArray>(Atrac3plusData2.atrac3p_huff_gain_npoints1_cb, Atrac3plusData2.atrac3p_huff_gain_npoints1_cb, Atrac3plusData2.atrac3p_huff_gain_lev1_cb, Atrac3plusData2.atrac3p_huff_gain_lev2_cb, Atrac3plusData2.atrac3p_huff_gain_lev3_cb, Atrac3plusData2.atrac3p_huff_gain_lev4_cb, Atrac3plusData2.atrac3p_huff_gain_loc3_cb, Atrac3plusData2.atrac3p_huff_gain_loc1_cb, Atrac3plusData2.atrac3p_huff_gain_loc4_cb, Atrac3plusData2.atrac3p_huff_gain_loc2_cb, Atrac3plusData2.atrac3p_huff_gain_loc5_cb)
		private val gain_xlats = arrayOf<IntArray?>(null, Atrac3plusData2.atrac3p_huff_gain_npoints2_xlat, Atrac3plusData2.atrac3p_huff_gain_lev1_xlat, Atrac3plusData2.atrac3p_huff_gain_lev2_xlat, Atrac3plusData2.atrac3p_huff_gain_lev3_xlat, Atrac3plusData2.atrac3p_huff_gain_lev4_xlat, Atrac3plusData2.atrac3p_huff_gain_loc3_xlat, Atrac3plusData2.atrac3p_huff_gain_loc1_xlat, Atrac3plusData2.atrac3p_huff_gain_loc4_xlat, Atrac3plusData2.atrac3p_huff_gain_loc2_xlat, Atrac3plusData2.atrac3p_huff_gain_loc5_xlat)
		private val gain_vlc_tabs = Array<VLC>(11) { i ->
			val vlc = VLC()
			buildCanonicalHuff(gain_cbs[i], gain_xlats[i], vlc)
			vlc
		}

		/* build huffman tables for tone decoding */
		private val tone_cbs = arrayOf<IntArray>(Atrac3plusData2.atrac3p_huff_tonebands_cb, Atrac3plusData2.atrac3p_huff_numwavs1_cb, Atrac3plusData2.atrac3p_huff_numwavs2_cb, Atrac3plusData2.atrac3p_huff_wav_ampsf1_cb, Atrac3plusData2.atrac3p_huff_wav_ampsf2_cb, Atrac3plusData2.atrac3p_huff_wav_ampsf3_cb, Atrac3plusData2.atrac3p_huff_freq_cb)
		private val tone_xlats = arrayOf<IntArray?>(null, null, Atrac3plusData2.atrac3p_huff_numwavs2_xlat, Atrac3plusData2.atrac3p_huff_wav_ampsf1_xlat, Atrac3plusData2.atrac3p_huff_wav_ampsf2_xlat, Atrac3plusData2.atrac3p_huff_wav_ampsf3_xlat, Atrac3plusData2.atrac3p_huff_freq_xlat)
		private val tone_vlc_tabs = Array<VLC>(7) { i->
			val vlc = VLC()
			buildCanonicalHuff(tone_cbs[i], tone_xlats[i], vlc)
			vlc
		}

		private val wl_nb_bits = intArrayOf(2, 3, 5, 5)
		private val wl_nb_codes = intArrayOf(3, 5, 8, 8)
		private val wl_bits = arrayOf<IntArray>(Atrac3plusData2.atrac3p_wl_huff_bits1, Atrac3plusData2.atrac3p_wl_huff_bits2, Atrac3plusData2.atrac3p_wl_huff_bits3, Atrac3plusData2.atrac3p_wl_huff_bits4)
		private val wl_codes = arrayOf<IntArray>(Atrac3plusData2.atrac3p_wl_huff_code1, Atrac3plusData2.atrac3p_wl_huff_code2, Atrac3plusData2.atrac3p_wl_huff_code3, Atrac3plusData2.atrac3p_wl_huff_code4)
		private val wl_xlats = arrayOf<IntArray?>(Atrac3plusData2.atrac3p_wl_huff_xlat1, Atrac3plusData2.atrac3p_wl_huff_xlat2, null, null)

		private val ct_nb_bits = intArrayOf(3, 4, 4, 4)
		private val ct_nb_codes = intArrayOf(4, 8, 8, 8)
		private val ct_bits = arrayOf<IntArray>(Atrac3plusData2.atrac3p_ct_huff_bits1, Atrac3plusData2.atrac3p_ct_huff_bits2, Atrac3plusData2.atrac3p_ct_huff_bits2, Atrac3plusData2.atrac3p_ct_huff_bits3)
		private val ct_codes = arrayOf<IntArray>(Atrac3plusData2.atrac3p_ct_huff_code1, Atrac3plusData2.atrac3p_ct_huff_code2, Atrac3plusData2.atrac3p_ct_huff_code2, Atrac3plusData2.atrac3p_ct_huff_code3)
		private val ct_xlats = arrayOf<IntArray?>(null, null, Atrac3plusData2.atrac3p_ct_huff_xlat1, null)

		private val sf_nb_bits = intArrayOf(9, 9, 9, 9, 6, 6, 7, 7)
		private val sf_nb_codes = intArrayOf(64, 64, 64, 64, 16, 16, 16, 16)
		private val sf_bits = arrayOf<IntArray>(Atrac3plusData2.atrac3p_sf_huff_bits1, Atrac3plusData2.atrac3p_sf_huff_bits1, Atrac3plusData2.atrac3p_sf_huff_bits2, Atrac3plusData2.atrac3p_sf_huff_bits3, Atrac3plusData2.atrac3p_sf_huff_bits4, Atrac3plusData2.atrac3p_sf_huff_bits4, Atrac3plusData2.atrac3p_sf_huff_bits5, Atrac3plusData2.atrac3p_sf_huff_bits6)
		private val sf_codes = arrayOf<IntArray>(Atrac3plusData2.atrac3p_sf_huff_code1, Atrac3plusData2.atrac3p_sf_huff_code1, Atrac3plusData2.atrac3p_sf_huff_code2, Atrac3plusData2.atrac3p_sf_huff_code3, Atrac3plusData2.atrac3p_sf_huff_code4, Atrac3plusData2.atrac3p_sf_huff_code4, Atrac3plusData2.atrac3p_sf_huff_code5, Atrac3plusData2.atrac3p_sf_huff_code6)
		private val sf_xlats = arrayOf<IntArray?>(Atrac3plusData2.atrac3p_sf_huff_xlat1, Atrac3plusData2.atrac3p_sf_huff_xlat2, null, null, Atrac3plusData2.atrac3p_sf_huff_xlat4, Atrac3plusData2.atrac3p_sf_huff_xlat5, null, null)

		private val wl_vlc_tabs = Array<VLC>(4) { i ->
			VLC().apply { initVLCSparse(wl_nb_bits[i], wl_nb_codes[i], wl_bits[i], wl_codes[i], wl_xlats[i]) }
		}
		private val sf_vlc_tabs = Array<VLC>(8) { i ->
			VLC().apply { initVLCSparse(sf_nb_bits[i], sf_nb_codes[i], sf_bits[i], sf_codes[i], sf_xlats[i]) }
		}
		private val ct_vlc_tabs = Array<VLC>(4) { i->
			VLC().apply { initVLCSparse(ct_nb_bits[i], ct_nb_codes[i], ct_bits[i], ct_codes[i], ct_xlats[i]) }
		}

		/* build huffman tables for spectrum decoding */
		private val spec_vlc_tabs = Array<VLC?>(112) { i ->
			val atrac3pSpecCodeTab = atrac3p_spectra_tabs[i]
			if (atrac3pSpecCodeTab.cb != null) {
				val vlc = VLC()
				buildCanonicalHuff(atrac3pSpecCodeTab.cb!!, atrac3pSpecCodeTab.xlat, vlc)
				vlc
			} else {
				null
			}
		}


		private fun buildCanonicalHuff(cb: IntArray, xlat: IntArray?, vlc: VLC): Int {
			val codes = IntArray(256)
			val bits = IntArray(256)
			var cbIndex = 0
			var index = 0
			var code = 0
			val minLen = cb[cbIndex++] // get shortest codeword length
			val maxLen = cb[cbIndex++] // get longest  codeword length

			for (b in minLen..maxLen) {
				for (i in cb[cbIndex++] downTo 1) {
					bits[index] = b
					codes[index] = code++
					index++
				}
				code = code shl 1
			}

			return vlc.initVLCSparse(maxLen, index, bits, codes, xlat)
		}
	}
}
