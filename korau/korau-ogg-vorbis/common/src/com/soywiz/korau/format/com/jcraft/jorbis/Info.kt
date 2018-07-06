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
import com.soywiz.korio.lang.*

class Info {
	var time_P = arrayOf<FuncTime>(Time0())
	val mapping_P = arrayOf<FuncMapping>(Mapping0())
	var residue_P = arrayOf<FuncResidue>(Residue0(), Residue1(), Residue2())

	var version: Int = 0
	var channels: Int = 0
	var rate: Int = 0

	// The below bitrate declarations are *hints*.
	// Combinations of the three values carry the following implications:
	//
	// all three set to the same value:
	// implies a fixed rate bitstream
	// only nominal set:
	// implies a VBR stream that averages the nominal bitrate.  No hard
	// upper/lower limit
	// upper and or lower set:
	// implies a VBR bitstream that obeys the bitrate limits. nominal
	// may also be set to give a nominal rate.
	// none set:
	//  the coder does not care to speculate.

	var bitrate_upper: Int = 0
	var bitrate_nominal: Int = 0
	var bitrate_lower: Int = 0

	// Vorbis supports only short and long blocks, but allows the
	// encoder to choose the sizes

	var blocksizes = IntArray(2)

	// modes are the primary means of supporting on-the-fly different
	// blocksizes, different channel mappings (LR or mid-side),
	// different residue backends, etc.  Each mode consists of a
	// blocksize flag and a mapping (along with the mapping setup

	var modes: Int = 0
	var maps: Int = 0
	var times: Int = 0
	var floors: Int = 0
	var residues: Int = 0
	var books: Int = 0
	var psys: Int = 0 // encode only

	var mode_param: Array<InfoMode> = arrayOf()

	var map_type: IntArray = intArrayOf()
	var map_param: Array<Any?> = arrayOf()

	var time_type: IntArray = intArrayOf()
	var time_param: Array<Any> = arrayOf()

	var floor_type: IntArray = intArrayOf()
	var floor_param: Array<Any?> = arrayOf()

	var residue_type: IntArray = intArrayOf()
	var residue_param: Array<Any?> = arrayOf()

	var book_param: Array<StaticCodeBook> = arrayOf()

	var psy_param = Array<PsyInfo>(64) { PsyInfo() } // encode only

	// for block long/sort tuning; encode only
	var envelopesa: Int = 0
	var preecho_thresh: Float = 0.toFloat()
	var preecho_clamp: Float = 0.toFloat()

	// used by synthesis, which has a full, alloced vi
	fun init() {
		rate = 0
	}

	fun clear() {
		for (i in 0 until modes) {
			mode_param[i] = InfoMode()
		}
		mode_param = arrayOf()

		for (i in 0 until maps) { // unpack does the range checking
			mapping_P[map_type[i]].free_info(map_param[i]!!)
		}
		map_param = arrayOf()

		for (i in 0 until times) { // unpack does the range checking
			time_P[time_type[i]].free_info(time_param[i])
		}
		time_param = arrayOf()

		for (i in 0 until floors) { // unpack does the range checking
			FuncFloor.floor_P[floor_type[i]].free_info(floor_param[i]!!)
		}
		floor_param = arrayOf()

		for (i in 0 until residues) { // unpack does the range checking
			residue_P[residue_type[i]].free_info(residue_param[i]!!)
		}
		residue_param = arrayOf()

		// the static codebooks *are* freed if you call info_clear, because
		// decode side does alloc a 'static' codebook. Calling clear on the
		// full codebook does not clear the static codebook (that's our
		// responsibility)
		for (i in 0 until books) {
			// just in case the decoder pre-cleared to save space
			book_param[i].clear()
			book_param[i] = StaticCodeBook()
		}
		//if(vi->book_param)free(vi->book_param);
		book_param = arrayOf()

		for (i in 0 until psys) {
			psy_param[i].free()
		}

	}

	// Header packing/unpacking
	fun unpack_info(opb: Buffer): Int {
		version = opb.read(32)
		if (version != 0)
			return -1

		channels = opb.read(8)
		rate = opb.read(32)

		bitrate_upper = opb.read(32)
		bitrate_nominal = opb.read(32)
		bitrate_lower = opb.read(32)

		blocksizes[0] = 1 shl opb.read(4)
		blocksizes[1] = 1 shl opb.read(4)

		if (rate < 1 || channels < 1 || blocksizes[0] < 8 || blocksizes[1] < blocksizes[0]
			|| opb.read(1) != 1
		) {
			clear()
			return -1
		}
		return 0
	}

	// all of the real encoding details are here.  The modes, books,
	// everything
	fun unpack_books(opb: Buffer): Int {

		books = opb.read(8) + 1

		if (book_param.size != books)
			book_param = Array(books) { StaticCodeBook() }
		for (i in 0 until books) {
			book_param[i] = StaticCodeBook()
			if (book_param[i].unpack(opb) != 0) {
				clear()
				return -1
			}
		}

		// time backend settings
		times = opb.read(6) + 1
		if (time_type.size != times)
			time_type = IntArray(times)
		if (time_param.size != times)
			time_param = Array<Any>(times) { Unit }
		for (i in 0 until times) {
			time_type[i] = opb.read(16)
			if (time_type[i] < 0 || time_type[i] >= VI_TIMEB) {
				clear()
				return -1
			}
			time_param[i] = time_P[time_type[i]].unpack(this, opb)
		}

		// floor backend settings
		floors = opb.read(6) + 1
		if (floor_type.size != floors)
			floor_type = IntArray(floors)
		if (floor_param.size != floors)
			floor_param = Array<Any?>(floors) { Unit }

		for (i in 0 until floors) {
			floor_type[i] = opb.read(16)
			if (floor_type[i] < 0 || floor_type[i] >= VI_FLOORB) {
				clear()
				return -1
			}

			floor_param[i] = FuncFloor.floor_P[floor_type[i]].unpack(this, opb)
			if (floor_param[i] == null) {
				clear()
				return -1
			}
		}

		// residue backend settings
		residues = opb.read(6) + 1

		if (residue_type.size != residues)
			residue_type = IntArray(residues)

		if (residue_param.size != residues)
			residue_param = Array<Any?>(residues) { Unit }

		for (i in 0 until residues) {
			residue_type[i] = opb.read(16)
			if (residue_type[i] < 0 || residue_type[i] >= VI_RESB) {
				clear()
				return -1
			}
			residue_param[i] = residue_P[residue_type[i]].unpack(this, opb)
			if (residue_param[i] == null) {
				clear()
				return -1
			}
		}

		// map backend settings
		maps = opb.read(6) + 1
		if (map_type.size != maps)
			map_type = IntArray(maps)
		if (map_param.size != maps)
			map_param = Array<Any?>(maps) { Unit }
		for (i in 0 until maps) {
			map_type[i] = opb.read(16)
			if (map_type[i] < 0 || map_type[i] >= VI_MAPB) {
				clear()
				return -1
			}
			map_param[i] = mapping_P[map_type[i]].unpack(this, opb)
			if (map_param[i] == null) {
				clear()
				return -1
			}
		}

		// mode settings
		modes = opb.read(6) + 1
		if (mode_param.size != modes)
			mode_param = Array<InfoMode>(modes) { InfoMode() }
		for (i in 0 until modes) {
			mode_param[i] = InfoMode()
			mode_param[i].blockflag = opb.read(1)
			mode_param[i].windowtype = opb.read(16)
			mode_param[i].transformtype = opb.read(16)
			mode_param[i].mapping = opb.read(8)

			if (mode_param[i].windowtype >= VI_WINDOWB
				|| mode_param[i].transformtype >= VI_WINDOWB
				|| mode_param[i].mapping >= maps
			) {
				clear()
				return -1
			}
		}

		if (opb.read(1) != 1) {
			clear()
			return -1
		}

		return 0
	}

	// The Vorbis header is in three packets; the initial small packet in
	// the first page that identifies basic parameters, a second packet
	// with bitstream comments and a third packet that holds the
	// codebook.

	fun synthesis_headerin(vc: Comment, op: Packet?): Int {
		val opb = Buffer()

		if (op != null) {
			opb.readinit(op.packet_base, op.packet, op.bytes)

			// Which of the three types of header is this?
			// Also verify header-ness, vorbis
			run {
				val buffer = ByteArray(6)
				val packtype = opb.read(8)
				opb.read(buffer, 6)
				if (buffer[0] != 'v'.toByte() || buffer[1] != 'o'.toByte() || buffer[2] != 'r'.toByte() || buffer[3] != 'b'.toByte() || buffer[4] != 'i'.toByte() || buffer[5] != 's'.toByte()) {
					// not a vorbis header
					return -1
				}
				when (packtype) {
					0x01 // least significant *bit* is read first
					-> {
						if (op.b_o_s == 0) {
							// Not the initial packet
							return -1
						}
						if (rate != 0) {
							// previously initialized info header
							return -1
						}
						return unpack_info(opb)
					}
					0x03 // least significant *bit* is read first
					-> {
						if (rate == 0) {
							// um... we didn't get the initial header
							return -1
						}
						return vc.unpack(opb)
					}
					0x05 // least significant *bit* is read first
					-> {
						if (rate == 0) {
							// um... we didn;t get the initial header or comments yet
							return -1
						}
						return unpack_books(opb)
					}
					else -> {
					}
				}// Not a valid vorbis header type
				//return(-1);
			}
		}
		return -1
	}

	// pack side
	fun pack_info(opb: Buffer): Int {
		// preamble
		opb.write(0x01, 8)
		opb.write(_vorbis)

		// basic information about the stream
		opb.write(0x00, 32)
		opb.write(channels, 8)
		opb.write(rate, 32)

		opb.write(bitrate_upper, 32)
		opb.write(bitrate_nominal, 32)
		opb.write(bitrate_lower, 32)

		opb.write(Util.ilog2(blocksizes[0]), 4)
		opb.write(Util.ilog2(blocksizes[1]), 4)
		opb.write(1, 1)
		return 0
	}

	fun pack_books(opb: Buffer): Int {
		opb.write(0x05, 8)
		opb.write(_vorbis)

		// books
		opb.write(books - 1, 8)
		for (i in 0 until books) {
			if (book_param[i].pack(opb) != 0) {
				//goto err_out;
				return -1
			}
		}

		// times
		opb.write(times - 1, 6)
		for (i in 0 until times) {
			opb.write(time_type[i], 16)
			time_P[time_type[i]].pack(this.time_param[i], opb)
		}

		// floors
		opb.write(floors - 1, 6)
		for (i in 0 until floors) {
			opb.write(floor_type[i], 16)
			FuncFloor.floor_P[floor_type[i]].pack(floor_param[i]!!, opb)
		}

		// residues
		opb.write(residues - 1, 6)
		for (i in 0 until residues) {
			opb.write(residue_type[i], 16)
			residue_P[residue_type[i]].pack(residue_param[i]!!, opb)
		}

		// maps
		opb.write(maps - 1, 6)
		for (i in 0 until maps) {
			opb.write(map_type[i], 16)
			mapping_P[map_type[i]].pack(this, map_param[i]!!, opb)
		}

		// modes
		opb.write(modes - 1, 6)
		for (i in 0 until modes) {
			opb.write(mode_param[i].blockflag, 1)
			opb.write(mode_param[i].windowtype, 16)
			opb.write(mode_param[i].transformtype, 16)
			opb.write(mode_param[i].mapping, 8)
		}
		opb.write(1, 1)
		return 0
	}

	fun blocksize(op: Packet): Int {
		//codec_setup_info
		val opb = Buffer()

		var mode: Int = 0

		opb.readinit(op.packet_base, op.packet, op.bytes)

		/* Check the packet type */
		if (opb.read(1) != 0) {
			/* Oops.  This is not an audio data packet */
			return OV_ENOTAUDIO
		}
		run {
			var modebits = 0
			var v = modes
			while (v > 1) {
				modebits++
				v = v ushr 1
			}

			/* read our mode and pre/post windowsize */
			mode = opb.read(modebits)
		}
		if (mode == -1)
			return OV_EBADPACKET
		return blocksizes[mode_param[mode].blockflag]
	}

	override fun toString(): String {
		return "version:$version, channels:$channels, rate:$rate, bitrate:$bitrate_upper,$bitrate_nominal,$bitrate_lower"
	}

	companion object {
		private val OV_EBADPACKET = -136
		private val OV_ENOTAUDIO = -135

		private val _vorbis = "vorbis".toByteArray(UTF8)
		private val VI_TIMEB = 1
		//  private static final int VI_FLOORB=1;
		private val VI_FLOORB = 2
		//  private static final int VI_RESB=1;
		private val VI_RESB = 3
		private val VI_MAPB = 1
		private val VI_WINDOWB = 1
	}
}
