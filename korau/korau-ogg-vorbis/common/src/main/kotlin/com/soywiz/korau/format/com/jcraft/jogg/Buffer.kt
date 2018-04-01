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

class Buffer {
	var ptr = 0
	var buffer: ByteArray = byteArrayOf()
	var endbit = 0
	var endbyte = 0
	var storage = 0

	fun writeinit() {
		buffer = ByteArray(BUFFER_INCREMENT)
		ptr = 0
		buffer[0] = '\u0000'.toByte()
		storage = BUFFER_INCREMENT
	}

	fun write(s: ByteArray) {
		for (i in s.indices) {
			if (s[i].toInt() == 0)
				break
			write(s[i].toInt(), 8)
		}
	}

	fun read(s: ByteArray, bytes: Int) {
		var bytes = bytes
		var i = 0
		while (bytes-- != 0) {
			s[i++] = read(8).toByte()
		}
	}

	fun writeclear() {
		buffer = byteArrayOf()
	}

	fun readinit(buf: ByteArray, start: Int, bytes: Int) {
		ptr = start
		buffer = buf
		endbit = 0
		endbyte = 0
		storage = bytes
	}

	fun write(value: Int, bits: Int) {
		var value = value
		var bits = bits
		if (endbyte + 4 >= storage) {
			val foo = ByteArray(storage + BUFFER_INCREMENT)
			arraycopy(buffer, 0, foo, 0, storage)
			buffer = foo
			storage += BUFFER_INCREMENT
		}

		value = value and mask[bits]
		bits += endbit
		buffer[ptr] = (buffer[ptr].toUnsigned() or (value shl endbit)).toByte()

		if (bits >= 8) {
			buffer[ptr + 1] = value.ushr(8 - endbit).toByte()
			if (bits >= 16) {
				buffer[ptr + 2] = value.ushr(16 - endbit).toByte()
				if (bits >= 24) {
					buffer[ptr + 3] = value.ushr(24 - endbit).toByte()
					if (bits >= 32) {
						if (endbit > 0) {
							buffer[ptr + 4] = value.ushr(32 - endbit).toByte()
						} else {
							buffer[ptr + 4] = 0
						}
					}
				}
			}
		}

		endbyte += bits / 8
		ptr += bits / 8
		endbit = bits and 7
	}

	fun look(bits: Int): Int {
		var bits = bits
		var ret: Int
		val m = mask[bits]

		bits += endbit

		if (endbyte + 4 >= storage) {
			if (endbyte + (bits - 1) / 8 >= storage)
				return -1
		}

		ret = (buffer[ptr].toUnsigned()).ushr(endbit)
		if (bits > 8) {
			ret = ret or (buffer[ptr + 1].toUnsigned() shl 8 - endbit)
			if (bits > 16) {
				ret = ret or (buffer[ptr + 2].toUnsigned() shl 16 - endbit)
				if (bits > 24) {
					ret = ret or (buffer[ptr + 3].toUnsigned() shl 24 - endbit)
					if (bits > 32 && endbit != 0) {
						ret = ret or (buffer[ptr + 4].toUnsigned() shl 32 - endbit)
					}
				}
			}
		}
		return m and ret
	}

	fun adv(bits: Int) {
		var bits = bits
		bits += endbit
		ptr += bits / 8
		endbyte += bits / 8
		endbit = bits and 7
	}

	fun read(bits: Int): Int {
		var bits = bits
		var ret: Int
		val m = mask[bits]

		bits += endbit

		if (endbyte + 4 >= storage) {
			ret = -1
			if (endbyte + (bits - 1) / 8 >= storage) {
				ptr += bits / 8
				endbyte += bits / 8
				endbit = bits and 7
				return ret
			}
		}

		ret = (buffer[ptr].toUnsigned()).ushr(endbit)
		if (bits > 8) {
			ret = ret or (buffer[ptr + 1].toUnsigned() shl 8 - endbit)
			if (bits > 16) {
				ret = ret or (buffer[ptr + 2].toUnsigned() shl 16 - endbit)
				if (bits > 24) {
					ret = ret or (buffer[ptr + 3].toUnsigned() shl 24 - endbit)
					if (bits > 32 && endbit != 0) {
						ret = ret or (buffer[ptr + 4].toUnsigned() shl 32 - endbit)
					}
				}
			}
		}

		ret = ret and m

		ptr += bits / 8
		endbyte += bits / 8
		endbit = bits and 7
		return ret
	}

	fun read1(): Int {
		val ret: Int
		if (endbyte >= storage) {
			ret = -1
			endbit++
			if (endbit > 7) {
				endbit = 0
				ptr++
				endbyte++
			}
			return ret
		}

		ret = buffer[ptr].toInt() shr endbit and 1

		endbit++
		if (endbit > 7) {
			endbit = 0
			ptr++
			endbyte++
		}
		return ret
	}

	fun bytes(): Int {
		return endbyte + (endbit + 7) / 8
	}

	fun gbuffer(): ByteArray = buffer

	companion object {
		private val BUFFER_INCREMENT = 256
		private val mask = intArrayOf(
			0x00000000,
			0x00000001,
			0x00000003,
			0x00000007,
			0x0000000f,
			0x0000001f,
			0x0000003f,
			0x0000007f,
			0x000000ff,
			0x000001ff,
			0x000003ff,
			0x000007ff,
			0x00000fff,
			0x00001fff,
			0x00003fff,
			0x00007fff,
			0x0000ffff,
			0x0001ffff,
			0x0003ffff,
			0x0007ffff,
			0x000fffff,
			0x001fffff,
			0x003fffff,
			0x007fffff,
			0x00ffffff,
			0x01ffffff,
			0x03ffffff,
			0x07ffffff,
			0x0fffffff,
			0x1fffffff,
			0x3fffffff,
			0x7fffffff,
			0xffffffff.toInt()
		)
	}
}
