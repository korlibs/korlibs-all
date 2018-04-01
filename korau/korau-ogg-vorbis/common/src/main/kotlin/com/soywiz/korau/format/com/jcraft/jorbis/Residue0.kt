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

import com.soywiz.korau.format.com.jcraft.jogg.Buffer
import com.soywiz.korio.Synchronized
import com.soywiz.korio.math.rint
import kotlin.math.pow

internal open class Residue0 : FuncResidue() {
	override fun pack(vr: Any, opb: Buffer) {
		val info = vr as InfoResidue0
		var acc = 0
		opb.write(info.begin, 24)
		opb.write(info.end, 24)

		opb.write(info.grouping - 1, 24) /* residue vectors to group and
                           code with a partitioned book */
		opb.write(info.partitions - 1, 6) /* possible partition choices */
		opb.write(info.groupbook, 8) /* group huffman book */

		/* secondstages is a bitmask; as encoding progresses pass by pass, a
	   bitmask of one indicates this partition class has bits to write
	   this pass */
		for (j in 0..info.partitions - 1) {
			val i = info.secondstages[j]
			if (Util.ilog(i) > 3) {
				/* yes, this is a minor hack due to not thinking ahead */
				opb.write(i, 3)
				opb.write(1, 1)
				opb.write(i.ushr(3), 5)
			} else {
				opb.write(i, 4) /* trailing zero */
			}
			acc += Util.icount(i)
		}
		for (j in 0..acc - 1) {
			opb.write(info.booklist[j], 8)
		}
	}

	override fun unpack(vi: Info, opb: Buffer): Any? {
		var acc = 0
		val info = InfoResidue0()
		info.begin = opb.read(24)
		info.end = opb.read(24)
		info.grouping = opb.read(24) + 1
		info.partitions = opb.read(6) + 1
		info.groupbook = opb.read(8)

		for (j in 0..info.partitions - 1) {
			var cascade = opb.read(3)
			if (opb.read(1) != 0) {
				cascade = cascade or (opb.read(5) shl 3)
			}
			info.secondstages[j] = cascade
			acc += Util.icount(cascade)
		}

		for (j in 0..acc - 1) {
			info.booklist[j] = opb.read(8)
		}

		if (info.groupbook >= vi.books) {
			free_info(info)
			return null
		}

		for (j in 0..acc - 1) {
			if (info.booklist[j] >= vi.books) {
				free_info(info)
				return null
			}
		}
		return info
	}

	override fun look(vd: DspState, vm: InfoMode, vr: Any): Any {
		val info = vr as InfoResidue0
		val look = LookResidue0()
		var acc = 0
		val dim: Int
		var maxstage = 0
		look.info = info
		look.map = vm.mapping

		look.parts = info.partitions
		look.fullbooks = vd.fullbooks
		look.phrasebook = vd.fullbooks[info.groupbook]

		dim = look.phrasebook!!.dim

		look.partbooks = Array<IntArray>(look.parts) { intArrayOf() }

		for (j in 0..look.parts - 1) {
			val i = info.secondstages[j]
			val stages = Util.ilog(i)
			if (stages != 0) {
				if (stages > maxstage) maxstage = stages
				look.partbooks[j] = IntArray(stages)
				for (k in 0..stages - 1) {
					if (i and (1 shl k) != 0) {
						look.partbooks!![j][k] = info.booklist[acc++]
					}
				}
			}
		}

		look.partvals = rint(look.parts.toDouble().pow(dim.toDouble())).toInt()
		look.stages = maxstage
		look.decodemap = Array<IntArray>(look.partvals) { intArrayOf() }
		for (j in 0..look.partvals - 1) {
			var `val` = j
			var mult = look.partvals / look.parts
			look.decodemap[j] = IntArray(dim)

			for (k in 0..dim - 1) {
				val deco = `val` / mult
				`val` -= deco * mult
				mult /= look.parts
				look.decodemap!![j][k] = deco
			}
		}
		return look
	}

	override fun free_info(i: Any) {}

	override fun free_look(i: Any) {}

	override fun inverse(vb: Block, vl: Any, `in`: Array<FloatArray>, nonzero: IntArray, ch: Int): Int {
		var used = 0
		for (i in 0..ch - 1) {
			if (nonzero[i] != 0) {
				`in`[used++] = `in`[i]
			}
		}
		if (used != 0)
			return _01inverse(vb, vl, `in`, used, 0)
		else
			return 0
	}

	internal inner class LookResidue0 {
		var info: InfoResidue0? = null
		var map: Int = 0

		var parts: Int = 0
		var stages: Int = 0
		var fullbooks: Array<CodeBook> = arrayOf()
		var phrasebook: CodeBook? = null
		var partbooks: Array<IntArray> = arrayOf()

		var partvals: Int = 0
		var decodemap: Array<IntArray> = arrayOf()

		var postbits: Int = 0
		var phrasebits: Int = 0
		var frames: Int = 0
	}

	internal inner class InfoResidue0 {
		// block-partitioned VQ coded straight residue
		var begin: Int = 0
		var end: Int = 0

		// first stage (lossless partitioning)
		var grouping: Int = 0 // group n vectors per partition
		var partitions: Int = 0 // possible codebooks for a partition
		var groupbook: Int = 0 // huffbook for partitioning
		var secondstages = IntArray(64) // expanded out to pointers in lookup
		var booklist = IntArray(256) // list of second stage books

		// encode-only heuristic settings
		var entmax = FloatArray(64) // book entropy threshholds
		var ampmax = FloatArray(64) // book amp threshholds
		var subgrp = IntArray(64) // book heuristic subgroup size
		var blimit = IntArray(64) // subgroup position limits
	}

	companion object {

		private var _01inverse_partword = arrayOfNulls<Array<IntArray>>(2) // _01inverse is synchronized for

		// re-using partword
		@Synchronized
		fun _01inverse(vb: Block, vl: Any, `in`: Array<FloatArray>, ch: Int,
					   decodepart: Int): Int {
			var i: Int
			var j: Int
			var k: Int
			var l: Int
			var s: Int
			val look = vl as LookResidue0
			val info = look.info

			// move all this setup out later
			val samples_per_partition = info!!.grouping
			val partitions_per_word = look.phrasebook!!.dim
			val n = info.end - info.begin

			val partvals = n / samples_per_partition
			val partwords = (partvals + partitions_per_word - 1) / partitions_per_word

			if (_01inverse_partword.size < ch) {
				_01inverse_partword = arrayOfNulls<Array<IntArray>>(ch)
			}

			j = 0
			while (j < ch) {
				if (_01inverse_partword[j] == null || _01inverse_partword[j]!!.size < partwords) {
					_01inverse_partword[j] = Array<IntArray>(partwords) { intArrayOf() }
				}
				j++
			}

			s = 0
			while (s < look.stages) {
				// each loop decodes on partition codeword containing
				// partitions_pre_word partitions
				i = 0
				l = 0
				while (i < partvals) {
					if (s == 0) {
						// fetch the partition word for each channel
						j = 0
						while (j < ch) {
							val temp = look.phrasebook!!.decode(vb.opb)
							if (temp == -1) {
								return 0
							}
							_01inverse_partword!![j]!![l] = look.decodemap!![temp]
							if (_01inverse_partword!![j]!![l] == null) {
								return 0
							}
							j++
						}
					}

					// now we decode residual values for the partitions
					k = 0
					while (k < partitions_per_word && i < partvals) {

						j = 0
						while (j < ch) {
							val offset = info.begin + i * samples_per_partition
							val index = _01inverse_partword[j]!![l][k]
							if (info.secondstages[index] and (1 shl s) != 0) {
								val stagebook = look.fullbooks!![look.partbooks!![index][s]]
								if (stagebook != null) {
									if (decodepart == 0) {
										if (stagebook.decodevs_add(`in`[j], offset, vb.opb,
											samples_per_partition) == -1) {
											return 0
										}
									} else if (decodepart == 1) {
										if (stagebook.decodev_add(`in`[j], offset, vb.opb,
											samples_per_partition) == -1) {
											return 0
										}
									}
								}
							}
							j++
						}

						k++
						i++
					}
					l++
				}
				s++
			}
			return 0
		}

		var _2inverse_partword: Array<IntArray>? = null

		@Synchronized
		fun _2inverse(vb: Block, vl: Any, `in`: Array<FloatArray>, ch: Int): Int {
			var i: Int
			var k: Int
			var l: Int
			var s: Int
			val look = vl as LookResidue0
			val info = look.info

			// move all this setup out later
			val samples_per_partition = info!!.grouping
			val partitions_per_word = look.phrasebook!!.dim
			val n = info.end - info.begin

			val partvals = n / samples_per_partition
			val partwords = (partvals + partitions_per_word - 1) / partitions_per_word

			if (_2inverse_partword == null || _2inverse_partword!!.size < partwords) {
				_2inverse_partword = Array<IntArray>(partwords) { intArrayOf() }
			}
			s = 0
			while (s < look.stages) {
				i = 0
				l = 0
				while (i < partvals) {
					if (s == 0) {
						// fetch the partition word for each channel
						val temp = look.phrasebook!!.decode(vb.opb)
						if (temp == -1) {
							return 0
						}
						_2inverse_partword!![l] = look.decodemap!![temp]
						if (_2inverse_partword!![l] == null) {
							return 0
						}
					}

					// now we decode residual values for the partitions
					k = 0
					while (k < partitions_per_word && i < partvals) {
						val offset = info.begin + i * samples_per_partition
						val index = _2inverse_partword!![l][k]
						if (info.secondstages[index] and (1 shl s) != 0) {
							val stagebook = look.fullbooks!![look.partbooks!![index][s]]
							if (stagebook != null) {
								if (stagebook.decodevv_add(`in`, offset, ch, vb.opb,
									samples_per_partition) == -1) {
									return 0
								}
							}
						}
						k++
						i++
					}
					l++
				}
				s++
			}
			return 0
		}
	}

}
