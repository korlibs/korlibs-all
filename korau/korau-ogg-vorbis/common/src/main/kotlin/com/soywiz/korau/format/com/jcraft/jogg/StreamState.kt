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

package com.soywiz.korau.format.com.jcraft.jogg

import com.soywiz.kmem.*
import com.soywiz.korio.lang.*

class StreamState {
	var body_storage: Int = 16 * 1024
	var body_data: ByteArray = ByteArray(body_storage)
	var body_fill: Int = 0
	private var body_returned: Int = 0

	var lacing_storage: Int = 1024
	var lacing_vals: IntArray = IntArray(lacing_storage)
	var granule_vals: LongArray = LongArray(lacing_storage)
	var lacing_fill: Int = 0
	var lacing_packet: Int = 0
	var lacing_returned: Int = 0

	var header = ByteArray(282)
	var header_fill: Int = 0

	var e_o_s: Int = 0
	var b_o_s: Int = 0
	var serialno: Int = 0
	var pageno: Int = 0
	var packetno: Long = 0
	var granulepos: Long = 0

	fun init(serialno: Int) {
		for (i in body_data.indices) body_data[i] = 0
		for (i in lacing_vals.indices) lacing_vals[i] = 0
		for (i in granule_vals.indices) granule_vals[i] = 0
		this.serialno = serialno
	}

	fun clear() {
	}

	fun destroy() {
		clear()
	}

	fun body_expand(needed: Int) {
		if (body_storage <= body_fill + needed) {
			body_storage += needed + 1024
			val foo = ByteArray(body_storage)
			arraycopy(body_data, 0, foo, 0, body_data.size)
			body_data = foo
		}
	}

	fun lacing_expand(needed: Int) {
		if (lacing_storage <= lacing_fill + needed) {
			lacing_storage += needed + 32
			val foo = IntArray(lacing_storage)
			arraycopy(lacing_vals, 0, foo, 0, lacing_vals.size)
			lacing_vals = foo

			val bar = LongArray(lacing_storage)
			arraycopy(granule_vals, 0, bar, 0, granule_vals.size)
			granule_vals = bar
		}
	}

	fun packetout(op: Packet): Int {
		var ptr = lacing_returned

		if (lacing_packet <= ptr) return 0

		if (lacing_vals[ptr] and 0x400 != 0) {
			lacing_returned++
			packetno++
			return -1
		}

		run {
			var size = lacing_vals[ptr] and 0xff
			var bytes = 0

			op.packet_base = body_data
			op.packet = body_returned
			op.e_o_s = lacing_vals[ptr] and 0x200
			op.b_o_s = lacing_vals[ptr] and 0x100
			bytes += size

			while (size == 255) {
				val vall = lacing_vals[++ptr]
				size = vall and 0xff
				if (vall and 0x200 != 0) op.e_o_s = 0x200
				bytes += size
			}

			op.packetno = packetno
			op.granulepos = granule_vals[ptr]
			op.bytes = bytes

			body_returned += bytes

			lacing_returned = ptr + 1
		}
		packetno++
		return 1
	}

	fun pagein(og: Page): Int {
		val header_base = og.header_base
		val header = og.header
		val body_base = og.body_base
		var body = og.body
		var bodysize = og.body_len
		var segptr = 0

		val version = og.version()
		val continued = og.continued()
		var bos = og.bos()
		val eos = og.eos()
		val granulepos = og.granulepos()
		val _serialno = og.serialno()
		val _pageno = og.pageno()
		val segments = header_base[header + 26].toUnsigned()

		// clean up 'returned data'
		run {
			val lr = lacing_returned
			val br = body_returned

			// body data
			if (br != 0) {
				body_fill -= br
				if (body_fill != 0) {
					arraycopy(body_data, br, body_data, 0, body_fill)
				}
				body_returned = 0
			}

			if (lr != 0) {
				// segment table
				if (lacing_fill - lr != 0) {
					arraycopy(lacing_vals, lr, lacing_vals, 0, lacing_fill - lr)
					arraycopy(granule_vals, lr, granule_vals, 0, lacing_fill - lr)
				}
				lacing_fill -= lr
				lacing_packet -= lr
				lacing_returned = 0
			}
		}

		// check the serial number
		if (_serialno != serialno)
			return -1
		if (version > 0)
			return -1

		lacing_expand(segments + 1)

		// are we in sequence?
		if (_pageno != pageno) {
			var i: Int

			// unroll previous partial packet (if any)
			i = lacing_packet
			while (i < lacing_fill) {
				body_fill -= lacing_vals[i] and 0xff
				i++
				//System.out.println("??");
			}
			lacing_fill = lacing_packet

			// make a note of dropped data in segment table
			if (pageno != -1) {
				lacing_vals[lacing_fill++] = 0x400
				lacing_packet++
			}

			// are we a 'continued packet' page?  If so, we'll need to skip
			// some segments
			if (continued != 0) {
				bos = 0
				while (segptr < segments) {
					val vall = header_base[header + 27 + segptr].toUnsigned()
					body += vall
					bodysize -= vall
					if (vall < 255) {
						segptr++
						break
					}
					segptr++
				}
			}
		}

		if (bodysize != 0) {
			body_expand(bodysize)
			arraycopy(body_base, body, body_data, body_fill, bodysize)
			body_fill += bodysize
		}

		run {
			var saved = -1
			while (segptr < segments) {
				val vall = header_base[header + 27 + segptr].toUnsigned()
				lacing_vals[lacing_fill] = vall
				granule_vals[lacing_fill] = -1

				if (bos != 0) {
					lacing_vals[lacing_fill] = lacing_vals[lacing_fill] or 0x100
					bos = 0
				}

				if (vall < 255) {
					saved = lacing_fill
				}

				lacing_fill++
				segptr++

				if (vall < 255) {
					lacing_packet = lacing_fill
				}
			}

			/* set the granulepos on the last pcmval of the last full packet */
			if (saved != -1) {
				granule_vals[saved] = granulepos
			}
		}

		if (eos != 0) {
			e_o_s = 1
			if (lacing_fill > 0)
				lacing_vals[lacing_fill - 1] = lacing_vals[lacing_fill - 1] or 0x200
		}

		pageno = _pageno + 1
		return 0
	}

	fun flush(og: Page): Int {

		var i: Int
		var vals = 0
		val maxvals = if (lacing_fill > 255) 255 else lacing_fill
		var bytes = 0
		var acc = 0
		var granule_pos = granule_vals[0]

		if (maxvals == 0) return 0

		if (b_o_s == 0) {
			granule_pos = 0
			vals = 0
			while (vals < maxvals) {
				if (lacing_vals[vals] and 0x0ff < 255) {
					vals++
					break
				}
				vals++
			}
		} else {
			vals = 0
			while (vals < maxvals) {
				if (acc > 4096) {
					break
				}
				acc += lacing_vals[vals] and 0x0ff
				granule_pos = granule_vals[vals]
				vals++
			}
		}

		arraycopy("OggS".toByteArray(), 0, header, 0, 4)
		header[4] = 0x00

		header[5] = 0x00
		if (lacing_vals[0] and 0x100 == 0) header[5] = (header[5].toInt() or 0x01).toByte()
		if (b_o_s == 0) header[5] = (header[5].toInt() or 0x02).toByte()
		if (e_o_s != 0 && lacing_fill == vals) header[5] = (header[5].toInt() or 0x04).toByte()
		b_o_s = 1

		i = 6
		while (i < 14) {
			header[i] = granule_pos.toByte()
			granule_pos = granule_pos ushr 8
			i++
		}

		run {
			var _serialno = serialno
			i = 14
			while (i < 18) {
				header[i] = _serialno.toByte()
				_serialno = _serialno ushr 8
				i++
			}
		}

		if (pageno == -1) pageno = 0

		run {
			var _pageno = pageno++
			i = 18
			while (i < 22) {
				header[i] = _pageno.toByte()
				_pageno = _pageno ushr 8
				i++
			}
		}

		header[22] = 0
		header[23] = 0
		header[24] = 0
		header[25] = 0

		header[26] = vals.toByte()
		i = 0
		while (i < vals) {
			header[i + 27] = lacing_vals[i].toByte()
			bytes += header[i + 27].toUnsigned()
			i++
		}

		og.header_base = header
		og.header = 0
		header_fill = vals + 27
		og.header_len = header_fill
		og.body_base = body_data
		og.body = body_returned
		og.body_len = bytes

		lacing_fill -= vals
		arraycopy(lacing_vals, vals, lacing_vals, 0, lacing_fill * 4)
		arraycopy(granule_vals, vals, granule_vals, 0, lacing_fill * 8)
		body_returned += bytes

		og.checksum()

		return 1
	}

	fun pageout(og: Page): Int {
		if (e_o_s != 0 && lacing_fill != 0 || /* 'were done, now flush' case */
			body_fill - body_returned > 4096 || /* 'page nominal size' case */
			lacing_fill >= 255 || /* 'segment table full' case */
			lacing_fill != 0 && b_o_s == 0
		) { /* 'initial header page' case */
			return flush(og)
		}
		return 0
	}

	fun eof(): Int {
		return e_o_s
	}

	fun reset(): Int {
		body_fill = 0
		body_returned = 0

		lacing_fill = 0
		lacing_packet = 0
		lacing_returned = 0

		header_fill = 0

		e_o_s = 0
		b_o_s = 0
		pageno = -1
		packetno = 0
		granulepos = 0
		return 0
	}
}
