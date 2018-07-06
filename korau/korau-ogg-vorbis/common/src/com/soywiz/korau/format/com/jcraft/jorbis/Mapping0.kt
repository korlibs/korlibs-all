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

import com.soywiz.korau.format.com.jcraft.jogg.*
import com.soywiz.korio.*

internal class Mapping0 : FuncMapping() {

	override fun free_info(imap: Any) {}

	override fun free_look(imap: Any) {}

	override fun look(vd: DspState, vm: InfoMode, m: Any): Any {
		//System.err.println("Mapping0.look");
		val vi = vd.vi
		val look = LookMapping0()
		val info = m as InfoMapping0
		look.map = info
		look.mode = vm

		look.time_look = Array<Any>(info.submaps) { Unit }
		look.floor_look = Array<Any>(info.submaps) { Unit }
		look.residue_look = Array<Any>(info.submaps) { Unit }

		look.time_func = Array<FuncTime>(info.submaps) { Time0() }
		look.floor_func = Array<FuncFloor>(info.submaps) { Floor0() }
		look.residue_func = Array<FuncResidue>(info.submaps) { Residue0() }

		for (i in 0 until info.submaps) {
			val timenum = info.timesubmap[i]
			val floornum = info.floorsubmap[i]
			val resnum = info.residuesubmap[i]

			look.time_func[i] = vi.time_P[vi.time_type[timenum]]
			look.time_look[i] = look.time_func[i].look(vd, vm, vi.time_param[timenum])
			look.floor_func[i] = FuncFloor.floor_P[vi.floor_type[floornum]]
			look.floor_look[i] = look.floor_func[i].look(vd, vm, vi.floor_param[floornum]!!)
			look.residue_func[i] = vi.residue_P[vi.residue_type[resnum]]
			look.residue_look[i] = look.residue_func[i].look(vd, vm, vi.residue_param[resnum]!!)

		}

		if (vi.psys != 0 && vd.analysisp != 0) {
			// ??
		}

		look.ch = vi.channels

		return look
	}

	override fun pack(vi: Info, imap: Any, opb: Buffer) {
		val info = imap as InfoMapping0

		if (info.submaps > 1) {
			opb.write(1, 1)
			opb.write(info.submaps - 1, 4)
		} else {
			opb.write(0, 1)
		}

		if (info.coupling_steps > 0) {
			opb.write(1, 1)
			opb.write(info.coupling_steps - 1, 8)
			for (i in 0 until info.coupling_steps) {
				opb.write(info.coupling_mag[i], Util.ilog2(vi.channels))
				opb.write(info.coupling_ang[i], Util.ilog2(vi.channels))
			}
		} else {
			opb.write(0, 1)
		}

		opb.write(0, 2) /* 2,3:reserved */

		/* we don't write the channel submappings if we only have one... */
		if (info.submaps > 1) {
			for (i in 0 until vi.channels)
				opb.write(info.chmuxlist[i], 4)
		}
		for (i in 0 until info.submaps) {
			opb.write(info.timesubmap[i], 8)
			opb.write(info.floorsubmap[i], 8)
			opb.write(info.residuesubmap[i], 8)
		}
	}

	// also responsible for range checking
	override fun unpack(vi: Info, opb: Buffer): Any? {
		val info = InfoMapping0()

		if (opb.read(1) != 0) {
			info.submaps = opb.read(4) + 1
		} else {
			info.submaps = 1
		}

		if (opb.read(1) != 0) {
			info.coupling_steps = opb.read(8) + 1

			for (i in 0 until info.coupling_steps) {
				val testM = opb.read(Util.ilog2(vi.channels))
				info.coupling_mag[i] = testM
				val testA = opb.read(Util.ilog2(vi.channels))
				info.coupling_ang[i] = testA

				if (testM < 0 || testA < 0 || testM == testA || testM >= vi.channels || testA >= vi.channels) {
					//goto err_out;
					info.free()
					return null
				}
			}
		}

		if (opb.read(2) > 0) { /* 2,3:reserved */
			info.free()
			return null
		}

		if (info.submaps > 1) {
			for (i in 0 until vi.channels) {
				info.chmuxlist[i] = opb.read(4)
				if (info.chmuxlist[i] >= info.submaps) {
					info.free()
					return null
				}
			}
		}

		for (i in 0 until info.submaps) {
			info.timesubmap[i] = opb.read(8)
			if (info.timesubmap[i] >= vi.times) {
				info.free()
				return null
			}
			info.floorsubmap[i] = opb.read(8)
			if (info.floorsubmap[i] >= vi.floors) {
				info.free()
				return null
			}
			info.residuesubmap[i] = opb.read(8)
			if (info.residuesubmap[i] >= vi.residues) {
				info.free()
				return null
			}
		}
		return info
	}

	var pcmbundle: Array<FloatArray> = arrayOf()
	var zerobundle: IntArray = intArrayOf()
	var nonzero: IntArray = intArrayOf()
	var floormemo: Array<Any?> = arrayOf()

	@Synchronized
	override fun inverse(vb: Block, l: Any): Int {
		val vd = vb.vd
		val vi = vd.vi
		val look = l as LookMapping0
		val info = look.map
		val mode = look.mode
		val n = vi.blocksizes[vb.W]
		vb.pcmend = n

		val window = vd.window[vb.W][vb.lW][vb.nW][mode.windowtype]
		if (pcmbundle == null || pcmbundle.size < vi.channels) {
			pcmbundle = Array<FloatArray>(vi.channels) { floatArrayOf() }
			nonzero = IntArray(vi.channels)
			zerobundle = IntArray(vi.channels)
			floormemo = Array<Any?>(vi.channels) { Unit }
		}

		// time domain information decode (note that applying the
		// information would have to happen later; we'll probably add a
		// function entry to the harness for that later
		// NOT IMPLEMENTED

		// recover the spectral envelope; store it in the PCM vector for now
		for (i in 0 until vi.channels) {
			val pcm = vb.pcm[i]
			val submap = info.chmuxlist[i]

			floormemo[i] = look.floor_func[submap].inverse1(vb, look.floor_look[submap], floormemo[i])
			if (floormemo[i] != null) {
				nonzero[i] = 1
			} else {
				nonzero[i] = 0
			}
			for (j in 0 until n / 2) {
				pcm[j] = 0f
			}

		}

		for (i in 0 until info.coupling_steps) {
			if (nonzero[info.coupling_mag[i]] != 0 || nonzero[info.coupling_ang[i]] != 0) {
				nonzero[info.coupling_mag[i]] = 1
				nonzero[info.coupling_ang[i]] = 1
			}
		}

		// recover the residue, apply directly to the spectral envelope

		for (i in 0 until info.submaps) {
			var ch_in_bundle = 0
			for (j in 0 until vi.channels) {
				if (info.chmuxlist[j] == i) {
					if (nonzero[j] != 0) {
						zerobundle[ch_in_bundle] = 1
					} else {
						zerobundle[ch_in_bundle] = 0
					}
					pcmbundle[ch_in_bundle++] = vb.pcm[j]
				}
			}

			look.residue_func[i].inverse(vb, look.residue_look[i], pcmbundle, zerobundle, ch_in_bundle)
		}

		for (i in info.coupling_steps - 1 downTo 0) {
			val pcmM = vb.pcm[info.coupling_mag[i]]
			val pcmA = vb.pcm[info.coupling_ang[i]]

			for (j in 0 until n / 2) {
				val mag = pcmM[j]
				val ang = pcmA[j]

				if (mag > 0) {
					if (ang > 0) {
						pcmM[j] = mag
						pcmA[j] = mag - ang
					} else {
						pcmA[j] = mag
						pcmM[j] = mag + ang
					}
				} else {
					if (ang > 0) {
						pcmM[j] = mag
						pcmA[j] = mag + ang
					} else {
						pcmA[j] = mag
						pcmM[j] = mag - ang
					}
				}
			}
		}

		//    /* compute and apply spectral envelope */

		for (i in 0 until vi.channels) {
			val pcm = vb.pcm[i]
			val submap = info.chmuxlist[i]
			look.floor_func[submap].inverse2(
				vb, look.floor_look[submap],
				floormemo[i], pcm
			)
		}

		// transform the PCM data; takes PCM vector, vb; modifies PCM vector
		// only MDCT right now....

		for (i in 0 until vi.channels) {
			val pcm = vb.pcm[i]
			//_analysis_output("out",seq+i,pcm,n/2,0,0);
			(vd.transform[vb.W][0] as Mdct).backward(pcm, pcm)
		}

		// now apply the decoded pre-window time information
		// NOT IMPLEMENTED

		// window the data
		for (i in 0 until vi.channels) {
			val pcm = vb.pcm[i]
			if (nonzero[i] != 0) {
				for (j in 0 until n) {
					pcm[j] *= window[j]
				}
			} else {
				for (j in 0 until n) {
					pcm[j] = 0f
				}
			}
		}

		// now apply the decoded post-window time information
		// NOT IMPLEMENTED
		// all done!
		return 0
	}

	internal inner class InfoMapping0 {
		var submaps: Int = 0 // <= 16
		var chmuxlist: IntArray = IntArray(256) // up to 256 channels in a Vorbis stream

		var timesubmap: IntArray = IntArray(16) // [mux]
		var floorsubmap: IntArray = IntArray(16) // [mux] submap to floors
		var residuesubmap: IntArray = IntArray(16)// [mux] submap to residue
		var psysubmap: IntArray = IntArray(16) // [mux]; encode only

		var coupling_steps: Int = 0
		var coupling_mag: IntArray = IntArray(256)
		var coupling_ang: IntArray = IntArray(256)

		fun free() {
			chmuxlist = intArrayOf()
			timesubmap = intArrayOf()
			floorsubmap = intArrayOf()
			residuesubmap = intArrayOf()
			psysubmap = intArrayOf()

			coupling_mag = intArrayOf()
			coupling_ang = intArrayOf()
		}
	}

	internal inner class LookMapping0 {
		var mode: InfoMode = InfoMode()
		var map: InfoMapping0 = InfoMapping0()
		var time_look: Array<Any> = arrayOf()
		var floor_look: Array<Any> = arrayOf()
		var floor_state: Array<Any> = arrayOf()
		var residue_look: Array<Any> = arrayOf()
		var psy_look: Array<PsyLook> = arrayOf()

		var time_func: Array<FuncTime> = arrayOf()
		var floor_func: Array<FuncFloor> = arrayOf()
		var residue_func: Array<FuncResidue> = arrayOf()

		var ch: Int = 0
		var decay: Array<FloatArray>? = null
		var lastframe: Int = 0 // if a different mode is called, we need to
		// invalidate decay and floor state
	}
}
