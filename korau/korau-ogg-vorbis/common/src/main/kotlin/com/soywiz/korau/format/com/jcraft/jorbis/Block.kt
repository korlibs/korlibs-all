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
import com.soywiz.korau.format.com.jcraft.jogg.Packet

class Block(var vd: DspState) {
	var pcm = arrayOf<FloatArray>()
	var opb = Buffer()

	var lW: Int = 0
	var W: Int = 0
	var nW: Int = 0
	var pcmend: Int = 0
	var mode: Int = 0

	var eofflag: Int = 0
	var granulepos: Long = 0
	var sequence: Long = 0

	var glue_bits: Int = 0
	var time_bits: Int = 0
	var floor_bits: Int = 0
	var res_bits: Int = 0

	init {
		if (vd.analysisp != 0) opb.writeinit()
	}

	fun init(vd: DspState) {
		this.vd = vd
	}

	fun clear(): Int {
		if (vd.analysisp != 0) opb.writeclear()
		return 0
	}

	fun synthesis(op: Packet): Int {
		val vi = vd.vi
		opb.readinit(op.packet_base, op.packet, op.bytes)
		if (opb.read(1) != 0) return -1

		val _mode = opb.read(vd.modebits)
		if (_mode == -1) return -1

		mode = _mode
		W = vi.mode_param[mode].blockflag
		if (W != 0) {
			lW = opb.read(1)
			nW = opb.read(1)
			if (nW == -1) return -1
		} else {
			lW = 0
			nW = 0
		}

		granulepos = op.granulepos
		sequence = op.packetno - 3
		eofflag = op.e_o_s

		pcmend = vi.blocksizes[W]
		if (pcm.size < vi.channels) pcm = Array<FloatArray>(vi.channels) { floatArrayOf() }
		for (i in 0 until vi.channels) {
			if (pcm[i].size < pcmend) {
				pcm[i] = FloatArray(pcmend)
			} else {
				pcm[i].fill(0f)
			}
		}

		val type = vi.map_type[vi.mode_param[mode].mapping]
		return FuncMapping.mapping_P[type].inverse(this, vd.mode[mode])
	}
}
