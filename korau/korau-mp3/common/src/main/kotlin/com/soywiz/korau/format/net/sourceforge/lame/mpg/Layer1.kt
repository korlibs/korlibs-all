/**
 * Mpeg Layer-1 audio decoder
 *
 *
 * Copyright (C) 1999-2010 The L.A.M.E. project
 *
 *
 * Initially written by Michael Hipp, see also AUTHORS and README.
 *
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the GNU
 * Library General Public License for more details.
 *
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.

 * @author Ken H�ndel
 */
package com.soywiz.korau.format.net.sourceforge.lame.mpg

import com.soywiz.korio.lang.*

class Layer1(private val common: Common, private val decode: Decode) {

	private fun I_step_one(mp: MPGLib.mpstr_tag, balloc: IntArray, scale_index: IntArray, fr: Frame) {
		var ba = 0
		var sca = 0

		assert(fr.stereo == 1 || fr.stereo == 2)
		if (fr.stereo == 2) {
			var i: Int
			val jsbound = fr.jsbound
			i = 0
			while (i < jsbound) {
				balloc[ba++] = common.getbits(mp, 4)
				balloc[ba++] = common.getbits(mp, 4)
				i++
			}
			i = jsbound
			while (i < MPG123.SBLIMIT) {
				balloc[ba++] = common.getbits(mp, 4)
				i++
			}

			ba = 0

			i = 0
			while (i < jsbound) {
				if (balloc[ba]++ != 0) scale_index[sca++] = common.getbits(mp, 6)
				if (balloc[ba++] != 0) scale_index[sca++] = common.getbits(mp, 6)
				i++
			}
			i = jsbound
			while (i < MPG123.SBLIMIT) {
				if (balloc[ba++] != 0) {
					scale_index[sca++] = common.getbits(mp, 6)
					scale_index[sca++] = common.getbits(mp, 6)
				}
				i++
			}
		} else {
			var i: Int
			i = 0
			while (i < MPG123.SBLIMIT) {
				balloc[ba++] = common.getbits(mp, 4)
				i++
			}
			ba = 0
			i = 0
			while (i < MPG123.SBLIMIT) {
				if (balloc[ba++] != 0) scale_index[sca++] = common.getbits(mp, 6)
				i++
			}
		}
	}

	private fun I_step_two(
		mp: MPGLib.mpstr_tag,
		fraction: Array<FloatArray>,
		balloc: IntArray,
		scale_index: IntArray,
		fr: Frame
	) {
		var i: Int
		var n: Int
		val smpb = IntArray(2 * MPG123.SBLIMIT) /* values: 0-65535 */
		var sample: Int
		var ba = 0
		var sca = 0

		assert(fr.stereo == 1 || fr.stereo == 2)
		if (fr.stereo == 2) {
			val jsbound = fr.jsbound
			var f0 = 0
			var f1 = 0
			ba = 0
			sample = 0
			i = 0
			while (i < jsbound) {
				n = balloc[ba++]
				if (n != 0) smpb[sample++] = common.getbits(mp, n + 1)
				n = balloc[ba++]
				if (n != 0) smpb[sample++] = common.getbits(mp, n + 1)
				i++
			}
			i = jsbound
			while (i < MPG123.SBLIMIT) {
				n = balloc[ba++]
				if (n != 0) smpb[sample++] = common.getbits(mp, n + 1)
				i++
			}
			ba = 0
			sample = 0
			i = 0
			while (i < jsbound) {
				n = balloc[ba++]
				if (n != 0) {
					fraction[0][f0++] = ((-1 shl n) + smpb[sample++] + 1).toFloat() *
							common.muls[n + 1][scale_index[sca++]]
				} else {
					fraction[0][f0++] = 0.0f
				}
				n = balloc[ba++]
				if (n != 0) {
					fraction[1][f1++] = ((-1 shl n) + smpb[sample++] + 1).toFloat() *
							common.muls[n + 1][scale_index[sca++]]
				} else {
					fraction[1][f1++] = 0.0f
				}
				i++
			}
			i = jsbound
			while (i < MPG123.SBLIMIT) {
				n = balloc[ba++]
				if (n != 0) {
					val samp = ((-1 shl n) + smpb[sample++] + 1).toFloat()
					fraction[0][f0++] = samp * common.muls[n + 1][scale_index[sca++]]
					fraction[1][f1++] = samp * common.muls[n + 1][scale_index[sca++]]
				} else {
					fraction[0][f0++] = 0.0f
					fraction[1][f1++] = 0.0f
				}
				i++
			}
			i = fr.down_sample_sblimit
			while (i < 32) {
				fraction[0][i] = 0.0f
				fraction[1][i] = 0.0f
				i++
			}
		} else {
			var f0 = 0
			ba = 0
			sample = 0
			i = 0
			while (i < MPG123.SBLIMIT) {
				n = balloc[ba++]
				if (n != 0) smpb[sample++] = common.getbits(mp, n + 1)
				i++
			}
			ba = 0
			sample = 0
			i = 0
			while (i < MPG123.SBLIMIT) {
				n = balloc[ba++]
				if (n != 0) {
					fraction[0][f0++] = ((-1 shl n) + smpb[sample++] + 1).toFloat() *
							common.muls[n + 1][scale_index[sca++]]
				} else {
					fraction[0][f0++] = 0.0f
				}
				i++
			}
			i = fr.down_sample_sblimit
			while (i < 32) {
				fraction[0][i] = 0.0f
				i++
			}
		}
	}

	fun do_layer1(mp: MPGLib.mpstr_tag, pcm_sample: FloatArray, pcm_point: MPGLib.ProcessedBytes): Int {
		var clip = 0
		val balloc = IntArray(2 * MPG123.SBLIMIT)
		val scale_index = IntArray(2 * MPG123.SBLIMIT)
		val fraction = Array(2) { FloatArray(MPG123.SBLIMIT) }
		val fr = mp.fr
		val stereo = fr.stereo
		var single = fr.single

		fr.jsbound = if (fr.mode == MPG123.MPG_MD_JOINT_STEREO) (fr.mode_ext shl 2) + 4 else 32

		if (stereo == 1 || single == 3) single = 0

		I_step_one(mp, balloc, scale_index, fr)

		var i = 0
		while (i < MPG123.SCALE_BLOCK) {
			I_step_two(mp, fraction, balloc, scale_index, fr)

			if (single >= 0) {
				clip += decode.synth1to1mono(mp, fraction[single], 0, pcm_sample, pcm_point)
			} else {
				val p1 = MPGLib.ProcessedBytes()
				p1.pb = pcm_point.pb
				clip += decode.synth_1to1(mp, fraction[0], 0, 0, pcm_sample, p1)
				clip += decode.synth_1to1(mp, fraction[1], 0, 1, pcm_sample, pcm_point)
			}
			i++
		}

		return clip
	}
}
