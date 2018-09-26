/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/* JOrbis
 * Copyright (C) 2000 ymnk, JCraft,Inc.
 *  
 * Written by: 2000 ymnk<ymnk@jcraft.com>
 *   
 * Many thanks to 
 *   Monty <monty@xiph.org> and 
 *   The XIPHOPHORUS Company http://www.xiph.org/ .
 * JOrbis has been based on their awesome works, Vorbis codec.
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.soywiz.korau.format.com.jcraft.jorbis

import com.soywiz.kmem.*
import com.soywiz.korio.error.*
import kotlin.math.*

class DspState() {

	var analysisp: Int = 0
	var vi: Info = Info()
	var modebits: Int = 0

	var pcm: Array<FloatArray> = arrayOf()
	var pcm_storage: Int = 0
	var pcm_current: Int = 0
	var pcm_returned: Int = 0

	var multipliers: FloatArray? = null
	var envelope_storage: Int = 0
	var envelope_current: Int = 0

	var eofflag: Int = 0

	var lW: Int = 0
	var W: Int = 0
	var nW: Int = 0
	var centerW: Int = 0

	var granulepos: Long = 0
	var sequence: Long = 0

	var glue_bits: Long = 0
	var time_bits: Long = 0
	var floor_bits: Long = 0
	var res_bits: Long = 0

	// local lookup storage
	var window: Array<Array<Array<Array<FloatArray>>>> // block, leadin, leadout, type
	var transform: Array<Array<Any>>
	var fullbooks: Array<CodeBook> = arrayOf()
	// backend lookups are tied to the mode, not the backend or naked mapping
	var mode: Array<Any> = arrayOf()

	// local storage, only used on the encoding side.  This way the
	// application does not need to worry about freeing some packets'
	// memory and not others'; packet storage is always tracked.
	// Cleared next call to a _dsp_ function
	var header: ByteArray? = null
	var header1: ByteArray? = null
	var header2: ByteArray? = null

	init {
		transform = Array<Array<Any>>(2) { arrayOf() }
		window = Array<Array<Array<Array<FloatArray>>>>(2) { arrayOf() }
		window[0] = Array<Array<Array<FloatArray>>>(2) { arrayOf() }
		window[0][0] = Array<Array<FloatArray>>(2) { arrayOf() }
		window[0][1] = Array<Array<FloatArray>>(2) { arrayOf() }
		window[0][0][0] = Array<FloatArray>(2) { floatArrayOf() }
		window[0][0][1] = Array<FloatArray>(2) { floatArrayOf() }
		window[0][1][0] = Array<FloatArray>(2) { floatArrayOf() }
		window[0][1][1] = Array<FloatArray>(2) { floatArrayOf() }
		window[1] = Array<Array<Array<FloatArray>>>(2) { arrayOf() }
		window[1][0] = Array<Array<FloatArray>>(2) { arrayOf() }
		window[1][1] = Array<Array<FloatArray>>(2) { arrayOf() }
		window[1][0][0] = Array<FloatArray>(2) { floatArrayOf() }
		window[1][0][1] = Array<FloatArray>(2) { floatArrayOf() }
		window[1][1][0] = Array<FloatArray>(2) { floatArrayOf() }
		window[1][1][1] = Array<FloatArray>(2) { floatArrayOf() }
	}

	// Analysis side code, but directly related to blocking.  Thus it's
	// here and not in analysis.c (which is for analysis transforms only).
	// The init is here because some of it is shared

	fun init(vi: Info, encp: Boolean): Int {
		this.vi = vi
		modebits = Util.ilog2(vi.modes)

		transform[0] = Array<Any>(VI_TRANSFORMB) { Unit }
		transform[1] = Array<Any>(VI_TRANSFORMB) { Unit }

		// MDCT is tranform 0

		transform[0][0] = Mdct()
		transform[1][0] = Mdct()
		(transform[0][0] as Mdct).init(vi.blocksizes[0])
		(transform[1][0] as Mdct).init(vi.blocksizes[1])

		window[0][0][0] = Array<FloatArray>(VI_WINDOWB) { floatArrayOf() }
		window[0][0][1] = window[0][0][0]
		window[0][1][0] = window[0][0][0]
		window[0][1][1] = window[0][0][0]
		window[1][0][0] = Array<FloatArray>(VI_WINDOWB) { floatArrayOf() }
		window[1][0][1] = Array<FloatArray>(VI_WINDOWB) { floatArrayOf() }
		window[1][1][0] = Array<FloatArray>(VI_WINDOWB) { floatArrayOf() }
		window[1][1][1] = Array<FloatArray>(VI_WINDOWB) { floatArrayOf() }

		for (i in 0 until VI_WINDOWB) {
			window[0][0][0][i] = window(i, vi.blocksizes[0], vi.blocksizes[0] / 2, vi.blocksizes[0] / 2)
			window[1][0][0][i] = window(i, vi.blocksizes[1], vi.blocksizes[0] / 2, vi.blocksizes[0] / 2)
			window[1][0][1][i] = window(i, vi.blocksizes[1], vi.blocksizes[0] / 2, vi.blocksizes[1] / 2)
			window[1][1][0][i] = window(i, vi.blocksizes[1], vi.blocksizes[1] / 2, vi.blocksizes[0] / 2)
			window[1][1][1][i] = window(i, vi.blocksizes[1], vi.blocksizes[1] / 2, vi.blocksizes[1] / 2)
		}

		fullbooks = Array<CodeBook>(vi.books) { CodeBook().apply { init_decode(vi.book_param[it]) } }

		// initialize the storage vectors to a decent size greater than the
		// minimum

		pcm_storage = 8192 // we'll assume later that we have
		// a minimum of twice the blocksize of
		// accumulated samples in analysis
		pcm = Array<FloatArray>(vi.channels) { floatArrayOf() }
		run {
			for (i in 0 until vi.channels) {
				pcm[i] = FloatArray(pcm_storage)
			}
		}

		// all 1 (large block) or 0 (small block)
		// explicitly set for the sake of clarity
		lW = 0 // previous window size
		W = 0 // current window size

		// all vector indexes; multiples of samples_per_envelope_step
		centerW = vi.blocksizes[1] / 2

		pcm_current = centerW

		// initialize all the mapping/backend lookups
		mode = Array<Any>(vi.modes) { Unit }
		for (i in 0 until vi.modes) {
			val mapnum = vi.mode_param[i].mapping
			val maptype = vi.map_type[mapnum]
			mode[i] = vi.mapping_P[maptype].look(this, vi.mode_param[i], vi.map_param[mapnum]!!)
		}
		return 0
	}

	fun synthesis_init(vi: Info): Int {
		init(vi, false)
		// Adjust centerW to allow an easier mechanism for determining output
		pcm_returned = centerW
		centerW -= vi.blocksizes[W] / 4 + vi.blocksizes[lW] / 4
		granulepos = -1
		sequence = -1
		return 0
	}

	constructor(vi: Info) : this() {
		init(vi, false)
		// Adjust centerW to allow an easier mechanism for determining output
		pcm_returned = centerW
		centerW -= vi.blocksizes[W] / 4 + vi.blocksizes[lW] / 4
		granulepos = -1
		sequence = -1
	}

	// Unike in analysis, the window is only partially applied for each
	// block.  The time domain envelope is not yet handled at the point of
	// calling (as it relies on the previous block).

	fun synthesis_blockin(vb: Block): Int {
		// Shift out any PCM/multipliers that we returned previously
		// centerW is currently the center of the last block added
		if (centerW > vi.blocksizes[1] / 2 && pcm_returned > 8192) {
			// don't shift too much; we need to have a minimum PCM buffer of
			// 1/2 long block

			var shiftPCM = centerW - vi.blocksizes[1] / 2
			shiftPCM = if (pcm_returned < shiftPCM) pcm_returned else shiftPCM

			pcm_current -= shiftPCM
			centerW -= shiftPCM
			pcm_returned -= shiftPCM
			if (shiftPCM != 0) {
				for (i in 0 until vi.channels) {
					arraycopy(pcm[i], shiftPCM, pcm[i], 0, pcm_current)
				}
			}
		}

		lW = W
		W = vb.W
		nW = -1

		glue_bits += vb.glue_bits.toLong()
		time_bits += vb.time_bits.toLong()
		floor_bits += vb.floor_bits.toLong()
		res_bits += vb.res_bits.toLong()

		if (sequence + 1 != vb.sequence)
			granulepos = -1 // out of sequence; lose count

		sequence = vb.sequence

		run {
			val sizeW = vi.blocksizes[W]
			var _centerW = centerW + vi.blocksizes[lW] / 4 + sizeW / 4
			val beginW = _centerW - sizeW / 2
			val endW = beginW + sizeW
			var beginSl = 0
			var endSl = 0

			// Do we have enough PCM/mult storage for the block?
			if (endW > pcm_storage) {
				// expand the storage
				pcm_storage = endW + vi.blocksizes[1]
				for (i in 0 until vi.channels) {
					val foo = FloatArray(pcm_storage)
					arraycopy(pcm[i], 0, foo, 0, pcm[i].size)
					pcm[i] = foo
				}
			}

			// overlap/add PCM
			when (W) {
				0 -> {
					beginSl = 0
					endSl = vi.blocksizes[0] / 2
				}
				1 -> {
					beginSl = vi.blocksizes[1] / 4 - vi.blocksizes[lW] / 4
					endSl = beginSl + vi.blocksizes[lW] / 2
				}
			}

			for (j in 0 until vi.channels) {
				val _pcm = beginW
				// the overlap/add section
				var i = beginSl
				while (i < endSl) {
					pcm[j][_pcm + i] += vb.pcm[j][i]
					i++
				}
				// the remaining section
				while (i < sizeW) {
					pcm[j][_pcm + i] = vb.pcm[j][i]
					i++
				}
			}

			// track the frame number... This is for convenience, but also
			// making sure our last packet doesn't end with added padding.  If
			// the last packet is partial, the number of samples we'll have to
			// return will be past the vb->granulepos.
			//
			// This is not foolproof!  It will be confused if we begin
			// decoding at the last page after a seek or hole.  In that case,
			// we don't have a starting point to judge where the last frame
			// is.  For this reason, vorbisfile will always try to make sure
			// it reads the last two marked pages in proper sequence

			if (granulepos == -1L) {
				granulepos = vb.granulepos
			} else {
				granulepos += (_centerW - centerW).toLong()
				if (vb.granulepos != -1L && granulepos != vb.granulepos) {
					if (granulepos > vb.granulepos && vb.eofflag != 0) {
						// partial last frame.  Strip the padding off
						_centerW -= (granulepos - vb.granulepos).toInt()
					}// else{ Shouldn't happen *unless* the bitstream is out of
					// spec.  Either way, believe the bitstream }
					granulepos = vb.granulepos
				}
			}

			// Update, cleanup

			centerW = _centerW
			pcm_current = endW
			if (vb.eofflag != 0)
				eofflag = 1
		}
		return 0
	}

	// pcm==NULL indicates we just want the pending samples, no more
	fun synthesis_pcmout(_pcm: Array<Array<FloatArray>>?, index: IntArray): Int {
		if (pcm_returned < centerW) {
			if (_pcm != null) {
				for (i in 0 until vi.channels) {
					index[i] = pcm_returned
				}
				_pcm[0] = pcm
			}
			return centerW - pcm_returned
		}
		return 0
	}

	fun synthesis_read(bytes: Int): Int {
		if (bytes != 0 && pcm_returned + bytes > centerW)
			return -1
		pcm_returned += bytes
		return 0
	}

	fun clear() {}

	companion object {
		val M_PI = 3.1415926539f
		val VI_TRANSFORMB = 1
		val VI_WINDOWB = 1

		fun window(type: Int, window: Int, left: Int, right: Int): FloatArray {
			val ret = FloatArray(window)
			when (type) {
				0 ->
					// The 'vorbis window' (window 0) is sin(sin(x)*sin(x)*2pi)
				{
					val leftbegin = window / 4 - left / 2
					val rightbegin = window - window / 4 - right / 2

					for (i in 0 until left) {
						var x = ((i + .5) / left * M_PI / 2.0).toFloat()
						x = sin(x.toDouble()).toFloat()
						x *= x
						x *= (M_PI / 2.0).toFloat()
						x = sin(x.toDouble()).toFloat()
						ret[i + leftbegin] = x
					}

					for (i in leftbegin + left until rightbegin) {
						ret[i] = 1f
					}

					for (i in 0 until right) {
						var x = ((right.toDouble() - i.toDouble() - .5) / right * M_PI / 2.0).toFloat()
						x = sin(x.toDouble()).toFloat()
						x *= x
						x *= (M_PI / 2.0).toFloat()
						x = sin(x.toDouble()).toFloat()
						ret[i + rightbegin] = x
					}
				}
				else ->
					//free(ret);
					//return null
					invalidOp("type != 0")
			}
			return ret
		}
	}
}
