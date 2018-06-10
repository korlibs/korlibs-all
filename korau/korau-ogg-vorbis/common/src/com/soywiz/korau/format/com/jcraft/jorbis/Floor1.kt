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
import kotlin.math.*

class Floor1 : FuncFloor() {

	override fun pack(i: Any, opb: Buffer) {
		val info = i as InfoFloor1

		var count = 0
		val rangebits: Int
		val maxposit = info.postlist[1]
		var maxclass = -1

		/* save out partitions */
		opb.write(info.partitions, 5) /* only 0 to 31 legal */
		for (j in 0 until info.partitions) {
			opb.write(info.partitionclass[j], 4) /* only 0 to 15 legal */
			if (maxclass < info.partitionclass[j])
				maxclass = info.partitionclass[j]
		}

		/* save out partition classes */
		for (j in 0 until maxclass + 1) {
			opb.write(info.class_dim[j] - 1, 3) /* 1 to 8 */
			opb.write(info.class_subs[j], 2) /* 0 to 3 */
			if (info.class_subs[j] != 0) {
				opb.write(info.class_book[j], 8)
			}
			for (k in 0 until (1 shl info.class_subs[j])) {
				opb.write(info.class_subbook[j][k] + 1, 8)
			}
		}

		/* save out the post list */
		opb.write(info.mult - 1, 2) /* only 1,2,3,4 legal now */
		opb.write(Util.ilog2(maxposit), 4)
		rangebits = Util.ilog2(maxposit)

		var j = 0
		var k = 0
		while (j < info.partitions) {
			count += info.class_dim[info.partitionclass[j]]
			while (k < count) {
				opb.write(info.postlist[k + 2], rangebits)
				k++
			}
			j++
		}
	}

	override fun unpack(vi: Info, opb: Buffer): Any? {
		var count = 0
		var maxclass = -1
		val rangebits: Int
		val info = InfoFloor1()

		/* read partitions */
		info.partitions = opb.read(5) /* only 0 to 31 legal */
		for (j in 0 until info.partitions) {
			info.partitionclass[j] = opb.read(4) /* only 0 to 15 legal */
			if (maxclass < info.partitionclass[j])
				maxclass = info.partitionclass[j]
		}

		/* read partition classes */
		for (j in 0 until maxclass + 1) {
			info.class_dim[j] = opb.read(3) + 1 /* 1 to 8 */
			info.class_subs[j] = opb.read(2) /* 0,1,2,3 bits */
			if (info.class_subs[j] < 0) {
				info.free()
				return null
			}
			if (info.class_subs[j] != 0) {
				info.class_book[j] = opb.read(8)
			}
			if (info.class_book[j] < 0 || info.class_book[j] >= vi.books) {
				info.free()
				return null
			}
			for (k in 0 until (1 shl info.class_subs[j])) {
				info.class_subbook[j][k] = opb.read(8) - 1
				if (info.class_subbook[j][k] < -1 || info.class_subbook[j][k] >= vi.books) {
					info.free()
					return null
				}
			}
		}

		/* read the post list */
		info.mult = opb.read(2) + 1 /* only 1,2,3,4 legal now */
		rangebits = opb.read(4)

		var j = 0
		var k = 0
		while (j < info.partitions) {
			count += info.class_dim[info.partitionclass[j]]
			while (k < count) {
				val t = opb.read(rangebits)
				info.postlist[k + 2] = t
				if (t < 0 || t >= 1 shl rangebits) {
					info.free()
					return null
				}
				k++
			}
			j++
		}
		info.postlist[0] = 0
		info.postlist[1] = 1 shl rangebits

		return info
	}

	override fun look(vd: DspState, mi: InfoMode, i: Any): Any {
		var _n = 0

		val sortpointer = IntArray(VIF_POSIT + 2)

		//    Info vi=vd.vi;

		val info = i as InfoFloor1
		val look = LookFloor1()
		look.vi = info
		look.n = info.postlist!![1]

		/* we drop each position value in-between already decoded values,
	 and use linear interpolation to predict each new value past the
	 edges.  The positions are read in the order of the position
	 list... we precompute the bounding positions in the lookup.  Of
	 course, the neighbors can change (if a position is declined), but
	 this is an initial mapping */

		for (j in 0 until info.partitions) {
			_n += info.class_dim[info.partitionclass[j]]
		}
		_n += 2
		look.posts = _n

		/* also store a sorted position index */
		for (j in 0 until _n) {
			sortpointer[j] = j
		}
		//    qsort(sortpointer,n,sizeof(int),icomp); // !!

		var foo: Int
		for (j in 0 until _n - 1) {
			for (k in j until _n) {
				if (info.postlist[sortpointer[j]] > info.postlist[sortpointer[k]]) {
					foo = sortpointer[k]
					sortpointer[k] = sortpointer[j]
					sortpointer[j] = foo
				}
			}
		}

		/* points from sort order back to range number */
		for (j in 0 until _n) {
			look.forward_index[j] = sortpointer[j]
		}
		/* points from range order to sorted position */
		for (j in 0 until _n) {
			look.reverse_index[look.forward_index[j]] = j
		}
		/* we actually need the post values too */
		for (j in 0 until _n) {
			look.sorted_index[j] = info.postlist[look.forward_index[j]]
		}

		/* quantize values to multiplier spec */
		when (info.mult) {
			1 /* 1024 -> 256 */ -> look.quant_q = 256
			2 /* 1024 -> 128 */ -> look.quant_q = 128
			3 /* 1024 -> 86 */ -> look.quant_q = 86
			4 /* 1024 -> 64 */ -> look.quant_q = 64
			else -> look.quant_q = -1
		}

		/* discover our neighbors for decode where we don't use fit flags
	   (that would push the neighbors outward) */
		for (j in 0 until _n - 2) {
			var lo = 0
			var hi = 1
			var lx = 0
			var hx = look.n
			val currentx = info.postlist[j + 2]
			for (k in 0 until j + 2) {
				val x = info.postlist[k]
				if (x in (lx + 1) until currentx) {
					lo = k
					lx = x
				}
				if (x in (currentx + 1) until hx) {
					hi = k
					hx = x
				}
			}
			look.loneighbor[j] = lo
			look.hineighbor[j] = hi
		}

		return look
	}

	override fun free_info(i: Any) {}

	override fun free_look(i: Any) {}

	override fun free_state(vs: Any) {}

	override fun forward(vb: Block, i: Any, `in`: FloatArray, out: FloatArray, vs: Any): Int {
		return 0
	}

	override fun inverse1(vb: Block, ii: Any, memo: Any?): Any? {
		val look = ii as LookFloor1
		val info = look.vi
		val books = vb.vd.fullbooks

		/* unpack wrapped/predicted values from stream */
		if (vb.opb.read(1) == 1) {
			var fit_value: IntArray = intArrayOf()
			if (memo is IntArray) {
				fit_value = memo
			}
			if (fit_value.size < look.posts) {
				fit_value = IntArray(look.posts)
			} else {
				fit_value.fill(0)
			}

			fit_value[0] = vb.opb.read(Util.ilog(look.quant_q - 1))
			fit_value[1] = vb.opb.read(Util.ilog(look.quant_q - 1))

			/* partition by partition */
			run {
				var i = 0
				var j = 2
				while (i < info!!.partitions) {
					val clss = info.partitionclass[i]
					val cdim = info.class_dim[clss]
					val csubbits = info.class_subs[clss]
					val csub = 1 shl csubbits
					var cval = 0

					/* decode the partition's first stage cascade value */
					if (csubbits != 0) {
						cval = books[info.class_book[clss]].decode(vb.opb)

						if (cval == -1) {
							return null
						}
					}

					for (k in 0 until cdim) {
						val book = info.class_subbook[clss][cval and csub - 1]
						cval = cval ushr csubbits
						if (book >= 0) {
							fit_value[j + k] = books[book].decode(vb.opb)
							if (fit_value[j + k] == -1) {
								return null
							}
						} else {
							fit_value[j + k] = 0
						}
					}
					j += cdim
					i++
				}
			}

			/* unwrap positive values and reconsitute via linear interpolation */
			for (i in 2 until look.posts) {
				val predicted = render_point(
					info!!.postlist[look.loneighbor[i - 2]],
					info.postlist[look.hineighbor[i - 2]],
					fit_value[look.loneighbor[i - 2]], fit_value[look.hineighbor[i - 2]],
					info.postlist[i]
				)
				val hiroom = look.quant_q - predicted
				val loroom = predicted
				val room = (if (hiroom < loroom) hiroom else loroom) shl 1
				var `val` = fit_value[i]

				if (`val` != 0) {
					if (`val` >= room) {
						if (hiroom > loroom) {
							`val` -= loroom
						} else {
							`val` = -1 - (`val` - hiroom)
						}
					} else {
						if (`val` and 1 != 0) {
							`val` = -(`val` + 1).ushr(1)
						} else {
							`val` = `val` shr 1
						}
					}

					fit_value[i] = `val` + predicted
					fit_value[look.loneighbor[i - 2]] = fit_value[look.loneighbor[i - 2]] and 0x7fff
					fit_value[look.hineighbor[i - 2]] = fit_value[look.hineighbor[i - 2]] and 0x7fff
				} else {
					fit_value[i] = predicted or 0x8000
				}
			}
			return fit_value
		}

		return null
	}

	override fun inverse2(vb: Block, i: Any, memo: Any?, out: FloatArray): Int {
		val look = i as LookFloor1
		val info = look.vi
		val n = vb.vd.vi.blocksizes[vb.mode] / 2

		if (memo != null) {
			/* render the lines */
			val fit_value = memo as IntArray?
			var hx = 0
			var lx = 0
			var ly = fit_value!![0] * info!!.mult
			for (j in 1 until look.posts) {
				val current = look.forward_index!![j]
				var hy = fit_value[current] and 0x7fff
				if (hy == fit_value[current]) {
					hy *= info.mult
					hx = info.postlist!![current]

					render_line(lx, hx, ly, hy, out)

					lx = hx
					ly = hy
				}
			}
			for (j in hx until n) {
				out[j] *= out[j - 1] /* be certain */
			}
			return 1
		}
		for (j in 0 until n) {
			out[j] = 0f
		}
		return 0
	}

	class InfoFloor1 {

		var partitions: Int = 0 /* 0 to 31 */
		var partitionclass: IntArray = IntArray(VIF_PARTS) /* 0 to 15 */
		var class_dim: IntArray = IntArray(VIF_CLASS) /* 1 to 8 */
		var class_subs: IntArray = IntArray(VIF_CLASS) /* 0,1,2,3 (bits: 1<<n poss) */
		var class_book: IntArray = IntArray(VIF_CLASS) /* subs ^ dim entries */
		var class_subbook: Array<IntArray> = Array(VIF_CLASS) { intArrayOf() } /* [VIF_CLASS][subs] */

		var mult: Int = 0 /* 1 2 3 or 4 */
		var postlist: IntArray = IntArray(VIF_POSIT + 2) /* first two implicit */

		/* encode side analysis parameters */
		var maxover: Float = 0.toFloat()
		var maxunder: Float = 0.toFloat()
		var maxerr: Float = 0.toFloat()

		var twofitminsize: Int = 0
		var twofitminused: Int = 0
		var twofitweight: Int = 0
		var twofitatten: Float = 0.toFloat()
		var unusedminsize: Int = 0
		var unusedmin_n: Int = 0

		var n: Int = 0

		init {
			for (i in class_subbook.indices) {
				class_subbook[i] = IntArray(8)
			}
		}

		fun free() {
			partitionclass = intArrayOf()
			class_dim = intArrayOf()
			class_subs = intArrayOf()
			class_book = intArrayOf()
			class_subbook = arrayOf()
			postlist = intArrayOf()
		}

		fun copy_info(): Any {
			val info = this
			val ret = InfoFloor1()

			ret.partitions = info.partitions
			arraycopy(info.partitionclass, 0, ret.partitionclass, 0, VIF_PARTS)
			arraycopy(info.class_dim, 0, ret.class_dim, 0, VIF_CLASS)
			arraycopy(info.class_subs, 0, ret.class_subs, 0, VIF_CLASS)
			arraycopy(info.class_book, 0, ret.class_book, 0, VIF_CLASS)

			for (j in 0 until VIF_CLASS) {
				arraycopy(info.class_subbook[j], 0, ret.class_subbook[j], 0, 8)
			}

			ret.mult = info.mult
			arraycopy(info.postlist, 0, ret.postlist, 0, VIF_POSIT + 2)

			ret.maxover = info.maxover
			ret.maxunder = info.maxunder
			ret.maxerr = info.maxerr

			ret.twofitminsize = info.twofitminsize
			ret.twofitminused = info.twofitminused
			ret.twofitweight = info.twofitweight
			ret.twofitatten = info.twofitatten
			ret.unusedminsize = info.unusedminsize
			ret.unusedmin_n = info.unusedmin_n

			ret.n = info.n

			return ret
		}

		companion object {
			val VIF_POSIT = 63
			val VIF_CLASS = 16
			val VIF_PARTS = 31
		}

	}

	class LookFloor1 {

		var sorted_index: IntArray = IntArray(VIF_POSIT + 2)
		var forward_index: IntArray = IntArray(VIF_POSIT + 2)
		var reverse_index: IntArray = IntArray(VIF_POSIT + 2)
		var hineighbor: IntArray = IntArray(VIF_POSIT)
		var loneighbor: IntArray = IntArray(VIF_POSIT)
		var posts: Int = 0

		var n: Int = 0
		var quant_q: Int = 0
		var vi: InfoFloor1? = null

		var phrasebits: Int = 0
		var postbits: Int = 0
		var frames: Int = 0

		fun free() {
			sorted_index = intArrayOf()
			forward_index = intArrayOf()
			reverse_index = intArrayOf()
			hineighbor = intArrayOf()
			loneighbor = intArrayOf()
		}

		companion object {
			val VIF_POSIT = 63
		}
	}

	class Lsfit_acc {
		var x0: Long = 0
		var x1: Long = 0

		var xa: Long = 0
		var ya: Long = 0
		var x2a: Long = 0
		var y2a: Long = 0
		var xya: Long = 0
		var n: Long = 0
		var an: Long = 0
		var un: Long = 0
		var edgey0: Long = 0
		var edgey1: Long = 0
	}

	class EchstateFloor1 {
		var codewords: IntArray? = null
		var curve: FloatArray? = null
		var frameno: Long = 0
		var codes: Long = 0
	}

	companion object {
		val floor1_rangedb = 140
		val VIF_POSIT = 63

		private fun render_point(x0: Int, x1: Int, y0: Int, y1: Int, x: Int): Int {
			var y0 = y0
			var y1 = y1
			y0 = y0 and 0x7fff /* mask off flag */
			y1 = y1 and 0x7fff

			run {
				val dy = y1 - y0
				val adx = x1 - x0
				val ady = abs(dy)
				val err = ady * (x - x0)

				val off = (err / adx).toInt()
				if (dy < 0)
					return y0 - off
				return y0 + off
			}
		}

		private val FLOOR_fromdB_LOOKUP = floatArrayOf(
			1.0649863e-07f,
			1.1341951e-07f,
			1.2079015e-07f,
			1.2863978e-07f,
			1.3699951e-07f,
			1.4590251e-07f,
			1.5538408e-07f,
			1.6548181e-07f,
			1.7623575e-07f,
			1.8768855e-07f,
			1.9988561e-07f,
			2.128753e-07f,
			2.2670913e-07f,
			2.4144197e-07f,
			2.5713223e-07f,
			2.7384213e-07f,
			2.9163793e-07f,
			3.1059021e-07f,
			3.3077411e-07f,
			3.5226968e-07f,
			3.7516214e-07f,
			3.9954229e-07f,
			4.2550680e-07f,
			4.5315863e-07f,
			4.8260743e-07f,
			5.1396998e-07f,
			5.4737065e-07f,
			5.8294187e-07f,
			6.2082472e-07f,
			6.6116941e-07f,
			7.0413592e-07f,
			7.4989464e-07f,
			7.9862701e-07f,
			8.5052630e-07f,
			9.0579828e-07f,
			9.6466216e-07f,
			1.0273513e-06f,
			1.0941144e-06f,
			1.1652161e-06f,
			1.2409384e-06f,
			1.3215816e-06f,
			1.4074654e-06f,
			1.4989305e-06f,
			1.5963394e-06f,
			1.7000785e-06f,
			1.8105592e-06f,
			1.9282195e-06f,
			2.0535261e-06f,
			2.1869758e-06f,
			2.3290978e-06f,
			2.4804557e-06f,
			2.6416497e-06f,
			2.8133190e-06f,
			2.9961443e-06f,
			3.1908506e-06f,
			3.3982101e-06f,
			3.6190449e-06f,
			3.8542308e-06f,
			4.1047004e-06f,
			4.3714470e-06f,
			4.6555282e-06f,
			4.9580707e-06f,
			5.2802740e-06f,
			5.6234160e-06f,
			5.9888572e-06f,
			6.3780469e-06f,
			6.7925283e-06f,
			7.2339451e-06f,
			7.7040476e-06f,
			8.2047000e-06f,
			8.7378876e-06f,
			9.3057248e-06f,
			9.9104632e-06f,
			1.0554501e-05f,
			1.1240392e-05f,
			1.1970856e-05f,
			1.2748789e-05f,
			1.3577278e-05f,
			1.4459606e-05f,
			1.5399272e-05f,
			1.6400004e-05f,
			1.7465768e-05f,
			1.8600792e-05f,
			1.9809576e-05f,
			2.1096914e-05f,
			2.2467911e-05f,
			2.3928002e-05f,
			2.5482978e-05f,
			2.7139006e-05f,
			2.8902651e-05f,
			3.0780908e-05f,
			3.2781225e-05f,
			3.4911534e-05f,
			3.7180282e-05f,
			3.9596466e-05f,
			4.2169667e-05f,
			4.4910090e-05f,
			4.7828601e-05f,
			5.0936773e-05f,
			5.4246931e-05f,
			5.7772202e-05f,
			6.1526565e-05f,
			6.5524908e-05f,
			6.9783085e-05f,
			7.4317983e-05f,
			7.9147585e-05f,
			8.4291040e-05f,
			8.9768747e-05f,
			9.5602426e-05f,
			0.00010181521f,
			0.00010843174f,
			0.00011547824f,
			0.00012298267f,
			0.00013097477f,
			0.00013948625f,
			0.00014855085f,
			0.00015820453f,
			0.00016848555f,
			0.00017943469f,
			0.00019109536f,
			0.00020351382f,
			0.00021673929f,
			0.00023082423f,
			0.00024582449f,
			0.00026179955f,
			0.00027881276f,
			0.00029693158f,
			0.00031622787f,
			0.00033677814f,
			0.00035866388f,
			0.00038197188f,
			0.00040679456f,
			0.00043323036f,
			0.00046138411f,
			0.00049136745f,
			0.00052329927f,
			0.00055730621f,
			0.00059352311f,
			0.00063209358f,
			0.00067317058f,
			0.00071691700f,
			0.00076350630f,
			0.00081312324f,
			0.00086596457f,
			0.00092223983f,
			0.00098217216f,
			0.0010459992f,
			0.0011139742f,
			0.0011863665f,
			0.0012634633f,
			0.0013455702f,
			0.0014330129f,
			0.0015261382f,
			0.0016253153f,
			0.0017309374f,
			0.0018434235f,
			0.0019632195f,
			0.0020908006f,
			0.0022266726f,
			0.0023713743f,
			0.0025254795f,
			0.0026895994f,
			0.0028643847f,
			0.0030505286f,
			0.0032487691f,
			0.0034598925f,
			0.0036847358f,
			0.0039241906f,
			0.0041792066f,
			0.0044507950f,
			0.0047400328f,
			0.0050480668f,
			0.0053761186f,
			0.0057254891f,
			0.0060975636f,
			0.0064938176f,
			0.0069158225f,
			0.0073652516f,
			0.0078438871f,
			0.0083536271f,
			0.0088964928f,
			0.009474637f,
			0.010090352f,
			0.010746080f,
			0.011444421f,
			0.012188144f,
			0.012980198f,
			0.013823725f,
			0.014722068f,
			0.015678791f,
			0.016697687f,
			0.017782797f,
			0.018938423f,
			0.020169149f,
			0.021479854f,
			0.022875735f,
			0.024362330f,
			0.025945531f,
			0.027631618f,
			0.029427276f,
			0.031339626f,
			0.033376252f,
			0.035545228f,
			0.037855157f,
			0.040315199f,
			0.042935108f,
			0.045725273f,
			0.048696758f,
			0.051861348f,
			0.055231591f,
			0.058820850f,
			0.062643361f,
			0.066714279f,
			0.071049749f,
			0.075666962f,
			0.080584227f,
			0.085821044f,
			0.091398179f,
			0.097337747f,
			0.10366330f,
			0.11039993f,
			0.11757434f,
			0.12521498f,
			0.13335215f,
			0.14201813f,
			0.15124727f,
			0.16107617f,
			0.17154380f,
			0.18269168f,
			0.19456402f,
			0.20720788f,
			0.22067342f,
			0.23501402f,
			0.25028656f,
			0.26655159f,
			0.28387361f,
			0.30232132f,
			0.32196786f,
			0.34289114f,
			0.36517414f,
			0.38890521f,
			0.41417847f,
			0.44109412f,
			0.46975890f,
			0.50028648f,
			0.53279791f,
			0.56742212f,
			0.60429640f,
			0.64356699f,
			0.68538959f,
			0.72993007f,
			0.77736504f,
			0.82788260f,
			0.88168307f,
			0.9389798f,
			1f
		)

		private fun render_line(x0: Int, x1: Int, y0: Int, y1: Int, d: FloatArray) {
			val dy = y1 - y0
			val adx = x1 - x0
			var ady = abs(dy)
			val base = dy / adx
			val sy = if (dy < 0) base - 1 else base + 1
			var x = x0
			var y = y0
			var err = 0

			ady -= abs(base * adx)

			d[x] *= FLOOR_fromdB_LOOKUP[y]
			while (++x < x1) {
				err = err + ady
				if (err >= adx) {
					err -= adx
					y += sy
				} else {
					y += base
				}
				d[x] *= FLOOR_fromdB_LOOKUP[y]
			}
		}
	}
}
