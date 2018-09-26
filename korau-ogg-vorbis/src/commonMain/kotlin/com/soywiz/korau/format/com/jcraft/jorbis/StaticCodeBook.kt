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
import com.soywiz.korau.format.com.jcraft.jogg.*
import com.soywiz.korma.math.*
import kotlin.math.*

class StaticCodeBook
// map == 2: list of dim*entries quantized entry vals

constructor() {
	var dim: Int = 0 // codebook dimensions (elements per vector)
	var entries: Int = 0 // codebook entries
	var lengthlist: IntArray = intArrayOf() // codeword lengths in bits

	// mapping
	var maptype: Int = 0 // 0=none
	// 1=implicitly populated values from map column
	// 2=listed arbitrary values

	// The below does a linear, single monotonic sequence mapping.
	var q_min: Int = 0 // packed 32 bit float; quant value 0 maps to minval
	var q_delta: Int = 0 // packed 32 bit float; val 1 - val 0 == delta
	var q_quant: Int = 0 // bits: 0 < quant <= 16
	var q_sequencep: Int = 0 // bitflag

	// additional information for log (dB) mapping; the linear mapping
	// is assumed to actually be values in dB.  encodebias is used to
	// assign an error weight to 0 dB. We have two additional flags:
	// zeroflag indicates if entry zero is to represent -Inf dB; negflag
	// indicates if we're to represent negative linear values in a
	// mirror of the positive mapping.

	var quantlist: IntArray? = null // map == 1: (int)(entries/dim) element column map

	fun pack(opb: Buffer): Int {
		var i: Int
		var ordered = false

		opb.write(0x564342, 24)
		opb.write(dim, 16)
		opb.write(entries, 24)

		// pack the codewords.  There are two packings; length ordered and
		// length random.  Decide between the two now.

		i = 1
		while (i < entries) {
			if (lengthlist[i] < lengthlist[i - 1])
				break
			i++
		}
		if (i == entries)
			ordered = true

		if (ordered) {
			// length ordered.  We only need to say how many codewords of
			// each length.  The actual codewords are generated
			// deterministically

			var count = 0
			opb.write(1, 1) // ordered
			opb.write(lengthlist[0] - 1, 5) // 1 to 32

			i = 1
			while (i < entries) {
				val _this = lengthlist[i]
				val _last = lengthlist[i - 1]
				if (_this > _last) {
					for (j in _last until _this) {
						opb.write(i - count, Util.ilog(entries - count))
						count = i
					}
				}
				i++
			}
			opb.write(i - count, Util.ilog(entries - count))
		} else {
			// length random.  Again, we don't code the codeword itself, just
			// the length.  This time, though, we have to encode each length
			opb.write(0, 1) // unordered

			// algortihmic mapping has use for 'unused entries', which we tag
			// here.  The algorithmic mapping happens as usual, but the unused
			// entry has no codeword.
			i = 0
			while (i < entries) {
				if (lengthlist[i] == 0)
					break
				i++
			}

			if (i == entries) {
				opb.write(0, 1) // no unused entries
				i = 0
				while (i < entries) {
					opb.write(lengthlist[i] - 1, 5)
					i++
				}
			} else {
				opb.write(1, 1) // we have unused entries; thus we tag
				i = 0
				while (i < entries) {
					if (lengthlist[i] == 0) {
						opb.write(0, 1)
					} else {
						opb.write(1, 1)
						opb.write(lengthlist[i] - 1, 5)
					}
					i++
				}
			}
		}

		// is the entry number the desired return value, or do we have a
		// mapping? If we have a mapping, what type?
		opb.write(maptype, 4)
		when (maptype) {
			0 -> {
			}
			1, 2 -> {
				// implicitly populated value mapping
				// explicitly populated value mapping
				if (quantlist == null) {
					// no quantlist?  error
					return -1
				}

				// values that define the dequantization
				opb.write(q_min, 32)
				opb.write(q_delta, 32)
				opb.write(q_quant - 1, 4)
				opb.write(q_sequencep, 1)

				run {
					var quantvals = 0
					when (maptype) {
						1 ->
							// a single column of (c->entries/c->dim) quantized values for
							// building a full value list algorithmically (square lattice)
							quantvals = maptype1_quantvals()
						2 ->
							// every value (c->entries*c->dim total) specified explicitly
							quantvals = entries * dim
					}

					// quantized values
					i = 0
					while (i < quantvals) {
						opb.write(abs(quantlist!![i]), q_quant)
						i++
					}
				}
			}
			else ->
				// error case; we don't have any other map types now
				return -1
		}// no mapping
		return 0
	}

	// unpacks a codebook from the packet buffer into the codebook struct,
	// readies the codebook auxiliary structures for decode
	fun unpack(opb: Buffer): Int {
		var i: Int
		//memset(s,0,sizeof(static_codebook));

		// make sure alignment is correct
		if (opb.read(24) != 0x564342) {
			//    goto _eofout;
			clear()
			return -1
		}

		// first the basic parameters
		dim = opb.read(16)
		entries = opb.read(24)
		if (entries == -1) {
			//    goto _eofout;
			clear()
			return -1
		}

		// codeword ordering.... length ordered or unordered?
		when (opb.read(1)) {
			0 -> {
				// unordered
				lengthlist = IntArray(entries)

				// allocated but unused entries?
				if (opb.read(1) != 0) {
					// yes, unused entries

					i = 0
					while (i < entries) {
						if (opb.read(1) != 0) {
							val num = opb.read(5)
							if (num == -1) {
								//            goto _eofout;
								clear()
								return -1
							}
							lengthlist[i] = num + 1
						} else {
							lengthlist[i] = 0
						}
						i++
					}
				} else {
					// all entries used; no tagging
					i = 0
					while (i < entries) {
						val num = opb.read(5)
						if (num == -1) {
							//          goto _eofout;
							clear()
							return -1
						}
						lengthlist[i] = num + 1
						i++
					}
				}
			}
			1 ->
				// ordered
			{
				var length = opb.read(5) + 1
				lengthlist = IntArray(entries)

				i = 0
				while (i < entries) {
					val num = opb.read(Util.ilog(entries - i))
					if (num == -1) {
						//          goto _eofout;
						clear()
						return -1
					}
					var j = 0
					while (j < num) {
						lengthlist[i] = length
						j++
						i++
					}
					length++
				}
			}
			else ->
				// EOF
				return -1
		}

		// Do we have a mapping to unpack?
		maptype = opb.read(4)
		when (maptype) {
			0 -> {
			}
			1, 2 -> {
				// implicitly populated value mapping
				// explicitly populated value mapping
				q_min = opb.read(32)
				q_delta = opb.read(32)
				q_quant = opb.read(4) + 1
				q_sequencep = opb.read(1)

				run {
					var quantvals = 0
					when (maptype) {
						1 -> quantvals = maptype1_quantvals()
						2 -> quantvals = entries * dim
					}

					// quantized values
					quantlist = IntArray(quantvals)
					i = 0
					while (i < quantvals) {
						quantlist!![i] = opb.read(q_quant)
						i++
					}
					if (quantlist!![quantvals - 1] == -1) {
						//        goto _eofout;
						clear()
						return -1
					}
				}
			}
			else -> {
				//    goto _eofout;
				clear()
				return -1
			}
		}// no mapping
		// all set
		return 0
		//    _errout:
		//    _eofout:
		//    vorbis_staticbook_clear(s);
		//    return(-1);
	}

	// there might be a straightforward one-line way to do the below
	// that's portable and totally safe against roundoff, but I haven't
	// thought of it.  Therefore, we opt on the side of caution
	private fun maptype1_quantvals(): Int {
		var vals = floor(entries.toDouble().pow(1.0 / dim)).toInt()

		// the above *should* be reliable, but we'll not assume that FP is
		// ever reliable when bitstream sync is at stake; verify via integer
		// means that vals really is the greatest value of dim for which
		// vals^b->bim <= b->entries
		// treat the above as an initial guess
		while (true) {
			var acc = 1
			var acc1 = 1
			for (i in 0 until dim) {
				acc *= vals
				acc1 *= vals + 1
			}
			if (entries in acc until acc1) {
				return vals
			} else {
				if (acc > entries) {
					vals--
				} else {
					vals++
				}
			}
		}
	}

	fun clear() {}

	// unpack the quantized list of values for encode/decode
	// we need to deal with two map types: in map type 1, the values are
	// generated algorithmically (each column of the vector counts through
	// the values in the quant vector). in map type 2, all the values came
	// in in an explicit list.  Both value lists must be unpacked
	fun unquantize(): FloatArray? {

		if (maptype == 1 || maptype == 2) {
			val quantvals: Int
			val mindel = float32_unpack(q_min)
			val delta = float32_unpack(q_delta)
			val r = FloatArray(entries * dim)

			// maptype 1 and 2 both use a quantized value vector, but
			// different sizes
			when (maptype) {
				1 -> {
					// most of the time, entries%dimensions == 0, but we need to be
					// well defined.  We define that the possible vales at each
					// scalar is values == entries/dim.  If entries%dim != 0, we'll
					// have 'too few' values (values*dim<entries), which means that
					// we'll have 'left over' entries; left over entries use zeroed
					// values (and are wasted).  So don't generate codebooks like that
					quantvals = maptype1_quantvals()
					for (j in 0 until entries) {
						var last = 0f
						var indexdiv = 1
						for (k in 0 until dim) {
							val index = j / indexdiv % quantvals
							var `val` = quantlist!![index].toFloat()
							`val` = abs(`val`) * delta + mindel + last
							if (q_sequencep != 0)
								last = `val`
							r[j * dim + k] = `val`
							indexdiv *= quantvals
						}
					}
				}
				2 -> for (j in 0 until entries) {
					var last = 0f
					for (k in 0 until dim) {
						var `val` = quantlist!![j * dim + k].toFloat()
						//if((j*dim+k)==0){System.err.println(" | 0 -> "+val+" | ");}
						`val` = abs(`val`) * delta + mindel + last
						if (q_sequencep != 0)
							last = `val`
						r[j * dim + k] = `val`
						//if((j*dim+k)==0){System.err.println(" $ r[0] -> "+r[0]+" | ");}
					}
				}
			}//System.err.println("\nr[0]="+r[0]);
			return r
		}
		return null
	}

	companion object {

		// 32 bit float (not IEEE; nonnormalized mantissa +
		// biased exponent) : neeeeeee eeemmmmm mmmmmmmm mmmmmmmm
		// Why not IEEE?  It's just not that important here.

		val VQ_FEXP = 10
		val VQ_FMAN = 21
		val VQ_FEXP_BIAS = 768 // bias toward values smaller than 1.

		// doesn't currently guard under/overflow
		fun float32_pack(`val`: Float): Long {
			var `val` = `val`
			var sign = 0
			var exp: Int
			val mant: Int
			if (`val` < 0) {
				sign = 0x80000000.toInt()
				`val` = -`val`
			}
			exp = floor(log(`val`.toDouble(), 2.0)).toInt()
			mant = rint(`val`.toDouble().pow((VQ_FMAN - 1 - exp).toDouble())).toInt()
			exp = exp + VQ_FEXP_BIAS shl VQ_FMAN
			return (sign or exp or mant).toLong()
		}

		fun float32_unpack(`val`: Int): Float {
			var mant = (`val` and 0x1fffff).toFloat()
			val exp = (`val` and 0x7fe00000).ushr(VQ_FMAN).toFloat()
			if (`val` and 0x80000000.toInt() != 0)
				mant = -mant
			return ldexp(mant, exp.toInt() - (VQ_FMAN - 1) - VQ_FEXP_BIAS)
		}

		fun ldexp(foo: Float, e: Int): Float {
			return (foo * 2.0.pow(e.toDouble())).toFloat()
		}
	}
}
