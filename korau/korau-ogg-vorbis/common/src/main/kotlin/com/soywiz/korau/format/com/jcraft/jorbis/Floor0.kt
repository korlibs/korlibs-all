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

import com.soywiz.kmem.fill
import com.soywiz.korau.format.com.jcraft.jogg.Buffer
import kotlin.math.atan
import kotlin.math.floor

class Floor0 : FuncFloor() {
	override fun pack(i: Any, opb: Buffer) {
		val info = i as InfoFloor0
		opb.write(info.order, 8)
		opb.write(info.rate, 16)
		opb.write(info.barkmap, 16)
		opb.write(info.ampbits, 6)
		opb.write(info.ampdB, 8)
		opb.write(info.numbooks - 1, 4)
		for (j in 0..info.numbooks - 1) {
			opb.write(info.books[j], 8)
		}
	}

	override fun unpack(vi: Info, opb: Buffer): Any? {
		val info = InfoFloor0()
		info.order = opb.read(8)
		info.rate = opb.read(16)
		info.barkmap = opb.read(16)
		info.ampbits = opb.read(6)
		info.ampdB = opb.read(8)
		info.numbooks = opb.read(4) + 1

		if (info.order < 1 || info.rate < 1 || info.barkmap < 1 || info.numbooks < 1) {
			return null
		}

		for (j in 0 until info.numbooks) {
			info.books[j] = opb.read(8)
			if (info.books[j] < 0 || info.books[j] >= vi.books) {
				return null
			}
		}
		return info
	}

	override fun look(vd: DspState, mi: InfoMode, i: Any): Any {
		val scale: Float
		val vi = vd.vi
		val info = i as InfoFloor0
		val look = LookFloor0()
		look.m = info.order
		look.n = vi.blocksizes[mi.blockflag] / 2
		look.ln = info.barkmap
		look.vi = info
		look.lpclook.init(look.ln, look.m)

		// we choose a scaling constant so that:
		scale = look.ln / toBARK((info.rate / 2.0).toFloat())

		// the mapping from a linear scale to a smaller bark scale is
		// straightforward.  We do *not* make sure that the linear mapping
		// does not skip bark-scale bins; the decoder simply skips them and
		// the encoder may do what it wishes in filling them.  They're
		// necessary in some mapping combinations to keep the scale spacing
		// accurate
		look.linearmap = IntArray(look.n)
		for (j in 0 until look.n) {
			var `val` = floor((toBARK((info.rate / 2.0 / look.n * j).toFloat()) * scale).toDouble()).toInt() // bark numbers represent band edges
			if (`val` >= look.ln) {
				`val` = look.ln // guard against the approximation
			}
			look.linearmap[j] = `val`
		}
		return look
	}

	fun state(i: Any): Any {
		val state = EchstateFloor0()
		val info = i as InfoFloor0

		// a safe size if usually too big (dim==1)
		state.codewords = IntArray(info.order)
		state.curve = FloatArray(info.barkmap)
		state.frameno = -1
		return state
	}

	override fun free_info(i: Any) {}

	override fun free_look(i: Any) {}

	override fun free_state(vs: Any) {}

	override fun forward(vb: Block, i: Any, `in`: FloatArray, out: FloatArray, vs: Any): Int {
		return 0
	}

	var lsp: FloatArray = floatArrayOf()

	fun inverse(vb: Block, i: Any, out: FloatArray): Int {
		//System.err.println("Floor0.inverse "+i.getClass()+"]");
		val look = i as LookFloor0
		val info = look.vi
		val ampraw = vb.opb.read(info!!.ampbits)
		if (ampraw > 0) { // also handles the -1 out of data case
			val maxval = (1 shl info.ampbits) - 1
			val amp = ampraw.toFloat() / maxval * info.ampdB
			val booknum = vb.opb.read(Util.ilog(info.numbooks))

			if (booknum != -1 && booknum < info.numbooks) {

				return synchronized(this) {
					if (lsp.size < look.m) {
						lsp = FloatArray(look.m)
					} else {
						lsp.fill(0f, 0, look.m)
					}

					val b = vb.vd.fullbooks[info.books[booknum]]
					var last = 0f

					for (j in 0..look.m - 1) {
						out[j] = 0.0f
					}

					run {
						var j = 0
						while (j < look.m) {
							if (b.decodevs(lsp, j, vb.opb, 1, -1) == -1) {
								for (k in 0..look.n - 1) {
									out[k] = 0.0f
								}
								return@synchronized 0
							}
							j += b.dim
						}
					}
					var j = 0
					while (j < look.m) {
						var k = 0
						while (k < b.dim) {
							lsp[j] += last
							k++
							j++
						}
						last = lsp[j - 1]
					}
					// take the coefficients back to a spectral envelope curve
					Lsp.lsp_to_curve(out, look.linearmap, look.n, look.ln, lsp, look.m, amp, info.ampdB.toFloat())
					return@synchronized 1
				}
			}
		}
		return 0
	}

	override fun inverse1(vb: Block, i: Any, memo: Any?): Any? {
		val look = i as LookFloor0
		val info = look.vi
		var lsp: FloatArray? = null
		if (memo is FloatArray) {
			lsp = memo
		}

		val ampraw = vb.opb.read(info!!.ampbits)
		if (ampraw > 0) { // also handles the -1 out of data case
			val maxval = (1 shl info.ampbits) - 1
			val amp = ampraw.toFloat() / maxval * info.ampdB
			val booknum = vb.opb.read(Util.ilog(info.numbooks))

			if (booknum != -1 && booknum < info.numbooks) {
				val b = vb.vd.fullbooks[info.books[booknum]]
				var last = 0f

				if (lsp == null || lsp.size < look.m + 1) {
					lsp = FloatArray(look.m + 1)
				} else {
					for (j in lsp.indices) {
						lsp[j] = 0f
					}
				}

				run {
					var j = 0
					while (j < look.m) {
						if (b.decodev_set(lsp!!, j, vb.opb, b.dim) == -1) {
							return null
						}
						j += b.dim
					}
				}

				var j = 0
				while (j < look.m) {
					var k = 0
					while (k < b.dim) {
						lsp[j] += last
						k++
						j++
					}
					last = lsp[j - 1]
				}
				lsp[look.m] = amp
				return lsp
			}
		}
		return null
	}

	override fun inverse2(vb: Block, i: Any, memo: Any?, out: FloatArray): Int {
		val look = i as LookFloor0
		val info = look.vi

		if (memo != null) {
			val lsp = memo as FloatArray?
			val amp = lsp!![look.m]

			Lsp.lsp_to_curve(out, look.linearmap, look.n, look.ln, lsp, look.m, amp, info!!.ampdB.toFloat())
			return 1
		}
		for (j in 0..look.n - 1) {
			out[j] = 0f
		}
		return 0
	}

	inner class InfoFloor0 {
		var order: Int = 0
		var rate: Int = 0
		var barkmap: Int = 0

		var ampbits: Int = 0
		var ampdB: Int = 0

		var numbooks: Int = 0 // <= 16
		var books = IntArray(16)
	}

	inner class LookFloor0 {
		var n: Int = 0
		var ln: Int = 0
		var m: Int = 0
		var linearmap: IntArray = intArrayOf()

		var vi: InfoFloor0? = null
		var lpclook = Lpc()
	}

	inner class EchstateFloor0 {
		var codewords: IntArray? = null
		var curve: FloatArray? = null
		var frameno: Long = 0
		var codes: Long = 0
	}

	companion object {
		fun toBARK(f: Float): Float = (13.1 * atan(.00074 * f) + 2.24 * atan(f.toDouble() * f.toDouble() * 1.85e-8) + 1e-4 * f).toFloat()
	}
}
