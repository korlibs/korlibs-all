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

import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.toUnsigned

// DECODING PRIMITIVES: packet streaming layer

// This has two layers to place more of the multi-serialno and paging
// control in the application's hands.  First, we expose a data buffer
// using ogg_decode_buffer().  The app either copies into the
// buffer, or passes it directly to read(), etc.  We then call
// ogg_decode_wrote() to tell how many bytes we just added.
//
// Pages are returned (pointers into the buffer in ogg_sync_state)
// by ogg_decode_stream().  The page is then submitted to
// ogg_decode_page() along with the appropriate
// ogg_stream_state* (ie, matching serialno).  We then get raw
// packets out calling ogg_stream_packet() with a
// ogg_stream_state.  See the 'frame-prog.txt' docs for details and
// example code.

class SyncState {
	var data: ByteArray = byteArrayOf()
	internal var storage: Int = 0
	var bufferOffset: Int = 0
		internal set
	var dataOffset: Int = 0
		internal set

	internal var unsynced: Int = 0
	internal var headerbytes: Int = 0
	internal var bodybytes: Int = 0

	fun clear(): Int {
		data = byteArrayOf()
		return 0
	}

	fun buffer(size: Int): Int {
		// first, clear out any space that has been previously returned
		if (dataOffset != 0) {
			bufferOffset -= dataOffset
			if (bufferOffset > 0) {
				arraycopy(data, dataOffset, data, 0, bufferOffset)
			}
			dataOffset = 0
		}

		if (size > storage - bufferOffset) {
			// We need to extend the internal buffer
			val newsize = size + bufferOffset + 4096 // an extra page to be nice
			if (data != null) {
				val foo = ByteArray(newsize)
				arraycopy(data, 0, foo, 0, data.size)
				data = foo
			} else {
				data = ByteArray(newsize)
			}
			storage = newsize
		}

		return bufferOffset
	}

	fun wrote(bytes: Int): Int {
		if (bufferOffset + bytes > storage) {
			return -1
		}
		bufferOffset += bytes
		return 0
	}

	// sync the stream.  This is meant to be useful for finding page
	// boundaries.
	//
	// return values for this:
	// -n) skipped n bytes
	//  0) page not ready; more data (no bytes skipped)
	//  n) page synced at current location; page length n bytes
	private val pageseek = Page()
	private val chksum = ByteArray(4)

	fun pageseek(og: Page?): Int {
		var page = dataOffset
		var next: Int
		var bytes = bufferOffset - dataOffset

		if (headerbytes == 0) {
			val _headerbytes: Int
			var i: Int
			if (bytes < 27) {
				return 0 // not enough for a header
			}

			/* verify capture pattern */
			if (data[page] != 'O'.toByte() || data[page + 1] != 'g'.toByte() || data[page + 2] != 'g'.toByte() || data[page + 3] != 'S'.toByte()) {
				headerbytes = 0
				bodybytes = 0

				// search for possible capture
				next = 0
				for (ii in 0..bytes - 1 - 1) {
					if (data[page + 1 + ii] == 'O'.toByte()) {
						next = page + 1 + ii
						break
					}
				}
				//next=memchr(page+1,'O',bytes-1);
				if (next == 0) {
					next = bufferOffset
				}

				dataOffset = next
				return -(next - page)
			}
			_headerbytes = (data[page + 26].toUnsigned()) + 27
			if (bytes < _headerbytes) {
				return 0 // not enough for header + seg table
			}

			// count up body length in the segment table

			i = 0
			while (i < data[page + 26].toUnsigned()) {
				bodybytes += data[page + 27 + i].toUnsigned()
				i++
			}
			headerbytes = _headerbytes
		}

		if (bodybytes + headerbytes > bytes)
			return 0

		// The whole test page is buffered.  Verify the checksum
		val result = synchronized(chksum) {
			// Grab the checksum bytes, set the header field to zero

			arraycopy(data, page + 22, chksum, 0, 4)
			data[page + 22] = 0
			data[page + 23] = 0
			data[page + 24] = 0
			data[page + 25] = 0

			// set up a temp page struct and recompute the checksum
			val log = pageseek
			log.header_base = data
			log.header = page
			log.header_len = headerbytes

			log.body_base = data
			log.body = page + headerbytes
			log.body_len = bodybytes
			log.checksum()

			// Compare
			if (chksum[0] != data[page + 22] || chksum[1] != data[page + 23] || chksum[2] != data[page + 24] || chksum[3] != data[page + 25]) {
				// D'oh.  Mismatch! Corrupt page (or miscapture and not a page at all)
				// replace the computed checksum with the one actually read in
				arraycopy(chksum, 0, data, page + 22, 4)
				// Bad checksum. Lose sync */

				headerbytes = 0
				bodybytes = 0
				// search for possible capture
				next = 0
				for (ii in 0..bytes - 1 - 1) {
					if (data[page + 1 + ii] == 'O'.toByte()) {
						next = page + 1 + ii
						break
					}
				}
				//next=memchr(page+1,'O',bytes-1);
				if (next == 0)
					next = bufferOffset
				dataOffset = next
				-(next - page)
			} else {
				null
			}
		}

		if (result != null) {
			return result
		}

		// yes, have a whole page all ready to go
		run {
			page = dataOffset

			if (og != null) {
				og.header_base = data
				og.header = page
				og.header_len = headerbytes
				og.body_base = data
				og.body = page + headerbytes
				og.body_len = bodybytes
			}

			unsynced = 0
			bytes = headerbytes + bodybytes
			dataOffset += bytes
			headerbytes = 0
			bodybytes = 0
			return bytes
		}
	}

	// sync the stream and get a page.  Keep trying until we find a page.
	// Supress 'sync errors' after reporting the first.
	//
	// return values:
	//  -1) recapture (hole in data)
	//   0) need more data
	//   1) page returned
	//
	// Returns pointers into buffered data; invalidated by next call to
	// _stream, _clear, _init, or _buffer

	fun pageout(og: Page): Int {
		// all we need to do is verify a page at the head of the stream
		// buffer.  If it doesn't verify, we look for the next potential
		// frame

		while (true) {
			val ret = pageseek(og)
			if (ret > 0) {
				// have a page
				return 1
			}
			if (ret == 0) {
				// need more data
				return 0
			}

			// head did not start a synced page... skipped some bytes
			if (unsynced == 0) {
				unsynced = 1
				return -1
			}
			// loop. keep looking
		}
	}

	// clear things to an initial state.  Good to call, eg, before seeking
	fun reset(): Int {
		bufferOffset = 0
		dataOffset = 0
		unsynced = 0
		headerbytes = 0
		bodybytes = 0
		return 0
	}

	fun init() {}
}
