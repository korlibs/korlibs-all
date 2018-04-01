/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
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

import com.soywiz.kmem.arraycopy
import com.soywiz.korau.format.com.jcraft.jogg.Packet
import com.soywiz.korau.format.com.jcraft.jogg.Page
import com.soywiz.korau.format.com.jcraft.jogg.StreamState
import com.soywiz.korau.format.com.jcraft.jogg.SyncState
import com.soywiz.korio.math.rint
import com.soywiz.korio.stream.SyncStream

class VorbisFile {
	var datasource: SyncStream? = null
	var seekable = false
	var offset: Long = 0
	var end: Long = 0

	var oy = SyncState()

	var links: Int = 0
	var offsets: LongArray = longArrayOf()
	var dataoffsets: LongArray = longArrayOf()
	var serialnos: IntArray = intArrayOf()
	var pcmlengths: LongArray = longArrayOf()
	var info: Array<Info> = arrayOf()
	var comment: Array<Comment> = arrayOf()

	// Decoding working state local storage
	var pcm_offset: Long = 0
	var decode_ready = false

	var current_serialno: Int = 0
	var current_link: Int = 0

	var bittrack: Float = 0.toFloat()
	var samptrack: Float = 0.toFloat()

	var os = StreamState() // take physical pages, weld into a logical
	// stream of packets
	var vd = DspState() // central working state for
	// the packet->PCM decoder
	var vb = Block(vd) // local working space for packet->PCM decode

	//ov_callbacks callbacks;

	//@Throws(JOrbisException::class)
	//constructor(file: String) : super() {
	//	var `is`: SyncStream? = null
	//	try {
	//		`is` = SeekableInputStream(file)
	//		val ret = open(`is`, null, 0)
	//		if (ret == -1) {
	//			throw JOrbisException("VorbisFile: open return -1")
	//		}
	//	} catch (e: Exception) {
	//		throw JOrbisException("VorbisFile: " + e.toString())
	//	} finally {
	//		if (`is` != null) {
	//			try {
	//				`is`.close()
	//			} catch (e: IOException) {
	//				e.printStackTrace()
	//			}
	//
	//		}
	//	}
	//}

	//@Throws(JOrbisException::class)
	constructor(`is`: SyncStream, initial: ByteArray, ibytes: Int) : super() {
		val ret = open(`is`, initial, ibytes)
		if (ret == -1) {
		}
	}

	private val _data: Int
		get() {
			val index = oy.buffer(CHUNKSIZE)
			val buffer = oy.data
			var bytes = 0
			try {
				bytes = datasource!!.read(buffer, index, CHUNKSIZE)
			} catch (e: Exception) {
				return OV_EREAD
			}

			oy.wrote(bytes)
			if (bytes == -1) {
				bytes = 0
			}
			return bytes
		}

	private fun seek_helper(offst: Long) {
		fseek(datasource!!, offst, SEEK_SET)
		this.offset = offst
		oy.reset()
	}

	private fun get_next_page(page: Page, boundary: Long): Int {
		var boundary = boundary
		if (boundary > 0)
			boundary += offset
		while (true) {
			val more: Int
			if (boundary > 0 && offset >= boundary)
				return OV_FALSE
			more = oy.pageseek(page)
			if (more < 0) {
				offset -= more.toLong()
			} else {
				if (more == 0) {
					if (boundary == 0L)
						return OV_FALSE
					val ret = _data
					if (ret == 0)
						return OV_EOF
					if (ret < 0)
						return OV_EREAD
				} else {
					val ret = offset.toInt() //!!!
					offset += more.toLong()
					return ret
				}
			}
		}
	}

	//@Throws(JOrbisException::class)
	private fun get_prev_page(page: Page): Int {
		var begin = offset //!!!
		var ret: Int
		var offst = -1
		while (offst == -1) {
			begin -= CHUNKSIZE.toLong()
			if (begin < 0) {
				begin = 0
			}
			seek_helper(begin)
			while (offset < begin + CHUNKSIZE) {
				ret = get_next_page(page, begin + CHUNKSIZE - offset)
				if (ret == OV_EREAD) {
					return OV_EREAD
				}
				if (ret < 0) {
					if (offst == -1) {
						throw JOrbisException()
					}
					break
				} else {
					offst = ret
				}
			}
		}
		seek_helper(offst.toLong()) //!!!
		ret = get_next_page(page, CHUNKSIZE.toLong())
		if (ret < 0) {
			return OV_EFAULT
		}
		return offst
	}

	fun bisect_forward_serialno(begin: Long, searched: Long, end: Long, currentno: Int, m: Int): Int {
		var searched = searched
		var endsearched = end
		var next = end
		val page = Page()
		var ret: Int

		while (searched < endsearched) {
			val bisect: Long
			if (endsearched - searched < CHUNKSIZE) {
				bisect = searched
			} else {
				bisect = (searched + endsearched) / 2
			}

			seek_helper(bisect)
			ret = get_next_page(page, -1)
			if (ret == OV_EREAD)
				return OV_EREAD
			if (ret < 0 || page.serialno() != currentno) {
				endsearched = bisect
				if (ret >= 0)
					next = ret.toLong()
			} else {
				searched = (ret + page.header_len + page.body_len).toLong()
			}
		}
		seek_helper(next)
		ret = get_next_page(page, -1)
		if (ret == OV_EREAD)
			return OV_EREAD

		if (searched >= end || ret == -1) {
			links = m + 1
			offsets = LongArray(m + 2)
			offsets[m + 1] = searched
		} else {
			ret = bisect_forward_serialno(next, offset, end, page.serialno(), m + 1)
			if (ret == OV_EREAD)
				return OV_EREAD
		}
		offsets[m] = begin
		return 0
	}

	// uses the local ogg_stream storage in vf; this is important for
	// non-streaming input sources
	fun fetch_headers(vi: Info, vc: Comment, serialno: IntArray?, og_ptr: Page?): Int {
		var og_ptr = og_ptr
		val og = Page()
		val op = Packet()
		val ret: Int

		if (og_ptr == null) {
			ret = get_next_page(og, CHUNKSIZE.toLong())
			if (ret == OV_EREAD)
				return OV_EREAD
			if (ret < 0)
				return OV_ENOTVORBIS
			og_ptr = og
		}

		if (serialno != null)
			serialno[0] = og_ptr.serialno()

		os.init(og_ptr.serialno())

		// extract the initial header from the first page and verify that the
		// Ogg bitstream is in fact Vorbis data

		vi.init()
		vc.init()

		var i = 0
		while (i < 3) {
			os.pagein(og_ptr)
			while (i < 3) {
				val result = os.packetout(op)
				if (result == 0)
					break
				if (result == -1) {
					vi.clear()
					vc.clear()
					os.clear()
					return -1
				}
				if (vi.synthesis_headerin(vc, op) != 0) {
					vi.clear()
					vc.clear()
					os.clear()
					return -1
				}
				i++
			}
			if (i < 3)
				if (get_next_page(og_ptr, 1) < 0) {
					vi.clear()
					vc.clear()
					os.clear()
					return -1
				}
		}
		return 0
	}

	// last step of the OggVorbis_File initialization; get all the
	// vorbis_info structs and PCM positions.  Only called by the seekable
	// initialization (local stream storage is hacked slightly; pay
	// attention to how that's done)
	//@Throws(JOrbisException::class)
	fun prefetch_all_headers(first_i: Info?, first_c: Comment?, dataoffset: Int) {
		val og = Page()
		var ret: Int

		info = Array<Info>(links) { Info() }
		comment = Array<Comment>(links) { Comment() }
		dataoffsets = LongArray(links)
		pcmlengths = LongArray(links)
		serialnos = IntArray(links)

		for (i in 0..links - 1) {
			if (first_i != null && first_c != null && i == 0) {
				// we already grabbed the initial header earlier.  This just
				// saves the waste of grabbing it again
				info[i] = first_i
				comment[i] = first_c
				dataoffsets[i] = dataoffset.toLong()
			} else {
				// seek to the location of the initial header
				seek_helper(offsets[i]) //!!!
				info[i] = Info()
				comment[i] = Comment()
				if (fetch_headers(info[i], comment[i], null, null) == -1) {
					dataoffsets[i] = -1
				} else {
					dataoffsets[i] = offset
					os.clear()
				}
			}

			// get the serial number and PCM length of this link. To do this,
			// get the last page of the stream
			run {
				val end = offsets[i + 1] //!!!
				seek_helper(end)

				while (true) {
					ret = get_prev_page(og)
					if (ret == -1) {
						// this should not be possible
						info[i].clear()
						comment[i].clear()
						break
					}
					if (og.granulepos() != -1L) {
						serialnos[i] = og.serialno()
						pcmlengths[i] = og.granulepos()
						break
					}
				}
			}
		}
	}

	private fun make_decode_ready(): Int {
		if (decode_ready) {
			throw RuntimeException("exit(1)")
		}
		vd.synthesis_init(info[0])
		vb.init(vd)
		decode_ready = true
		return 0
	}

	//@Throws(JOrbisException::class)
	fun open_seekable(): Int {
		val initial_i = Info()
		val initial_c = Comment()
		val serialno: Int
		var end: Long
		val ret: Int
		val dataoffset: Int
		val og = Page()
		// is this even vorbis...?
		val foo = IntArray(1)
		ret = fetch_headers(initial_i, initial_c, foo, null)
		serialno = foo[0]
		dataoffset = offset.toInt() //!!
		os.clear()
		if (ret == -1)
			return -1
		if (ret < 0)
			return ret
		// we can seek, so set out learning all about this file
		seekable = true
		fseek(datasource!!, 0, SEEK_END)
		offset = ftell(datasource!!)
		end = offset
		// We get the offset for the last page of the physical bitstream.
		// Most OggVorbis files will contain a single logical bitstream
		end = get_prev_page(og).toLong()
		// moer than one logical bitstream?
		if (og.serialno() != serialno) {
			// Chained bitstream. Bisect-search each logical bitstream
			// section.  Do so based on serial number only
			if (bisect_forward_serialno(0, 0, end + 1, serialno, 0) < 0) {
				clear()
				return OV_EREAD
			}
		} else {
			// Only one logical bitstream
			if (bisect_forward_serialno(0, end, end + 1, serialno, 0) < 0) {
				clear()
				return OV_EREAD
			}
		}
		prefetch_all_headers(initial_i, initial_c, dataoffset)
		return 0
	}

	fun open_nonseekable(): Int {
		// we cannot seek. Set up a 'single' (current) logical bitstream entry
		links = 1
		info = Array<Info>(links) { Info() }
		info[0] = Info() // ??
		comment = Array<Comment>(links) { Comment() }
		comment[0] = Comment() // ?? bug?

		// Try to fetch the headers, maintaining all the storage
		val foo = IntArray(1)
		if (fetch_headers(info[0], comment[0], foo, null) == -1)
			return -1
		current_serialno = foo[0]
		make_decode_ready()
		return 0
	}

	// clear out the current logical bitstream decoder
	fun decode_clear() {
		os.clear()
		vd.clear()
		vb.clear()
		decode_ready = false
		bittrack = 0f
		samptrack = 0f
	}

	// fetch and process a packet.  Handles the case where we're at a
	// bitstream boundary and dumps the decoding machine.  If the decoding
	// machine is unloaded, it loads it.  It also keeps pcm_offset up to
	// date (seek and read both use this.  seek uses a special hack with
	// readp).
	//
	// return: -1) hole in the data (lost packet)
	//          0) need more date (only if readp==0)/eof
	//          1) got a packet

	fun process_packet(readp: Int): Int {
		val og = Page()

		// handle one packet.  Try to fetch it from current stream state
		// extract packets from page
		while (true) {
			// process a packet if we can.  If the machine isn't loaded,
			// neither is a page
			if (decode_ready) {
				val op = Packet()
				val result = os.packetout(op)
				var granulepos: Long
				// if(result==-1)return(-1); // hole in the data. For now, swallow
				// and go. We'll need to add a real
				// error code in a bit.
				if (result > 0) {
					// got a packet.  process it
					granulepos = op.granulepos
					if (vb.synthesis(op) == 0) { // lazy check for lazy
						// header handling.  The
						// header packets aren't
						// audio, so if/when we
						// submit them,
						// vorbis_synthesis will
						// reject them
						// suck in the synthesis data and track bitrate
						run {
							val oldsamples = vd.synthesis_pcmout(null, intArrayOf())
							vd.synthesis_blockin(vb)
							samptrack += (vd.synthesis_pcmout(null, intArrayOf()) - oldsamples).toFloat()
							bittrack += (op.bytes * 8).toFloat()
						}

						// update the pcm offset.
						if (granulepos != -1L && op.e_o_s == 0) {
							val link = if (seekable) current_link else 0

							val samples = vd.synthesis_pcmout(null, intArrayOf())
							granulepos -= samples.toLong()
							for (i in 0..link - 1) {
								granulepos += pcmlengths[i]
							}
							pcm_offset = granulepos
						}
						return 1
					}
				}
			}

			if (readp == 0)
				return 0
			if (get_next_page(og, -1) < 0)
				return 0 // eof. leave unitialized

			// bitrate tracking; add the header's bytes here, the body bytes
			// are done by packet above
			bittrack += (og.header_len * 8).toFloat()

			// has our decoding just traversed a bitstream boundary?
			if (decode_ready) {
				if (current_serialno != og.serialno()) {
					decode_clear()
				}
			}

			// Do we need to load a new machine before submitting the page?
			// This is different in the seekable and non-seekable cases.
			//
			// In the seekable case, we already have all the header
			// information loaded and cached; we just initialize the machine
			// with it and continue on our merry way.
			//
			// In the non-seekable (streaming) case, we'll only be at a
			// boundary if we just left the previous logical bitstream and
			// we're now nominally at the header of the next bitstream

			if (!decode_ready) {
				var i: Int
				if (seekable) {
					current_serialno = og.serialno()

					// match the serialno to bitstream section.  We use this rather than
					// offset positions to avoid problems near logical bitstream
					// boundaries
					i = 0
					while (i < links) {
						if (serialnos[i] == current_serialno)
							break
						i++
					}
					if (i == links)
						return -1 // sign of a bogus stream.  error out,
					// leave machine uninitialized
					current_link = i

					os.init(current_serialno)
					os.reset()

				} else {
					// we're streaming
					// fetch the three header packets, build the info struct
					val foo = IntArray(1)
					val ret = fetch_headers(info[0], comment[0], foo, og)
					current_serialno = foo[0]
					if (ret != 0)
						return ret
					current_link++
					i = 0
				}
				make_decode_ready()
			}
			os.pagein(og)
		}
	}

	// The helpers are over; it's all toplevel interface from here on out
	// clear out the OggVorbis_File struct
	fun clear(): Int {
		vb.clear()
		vd.clear()
		os.clear()

		if (links != 0) {
			for (i in 0..links - 1) {
				info[i].clear()
				comment[i].clear()
			}
			info = arrayOf()
			comment = arrayOf()
		}
		dataoffsets = longArrayOf()
		pcmlengths = longArrayOf()
		serialnos = intArrayOf()
		offsets = longArrayOf()
		oy.clear()

		return 0
	}

	// inspects the OggVorbis file and finds/documents all the logical
	// bitstreams contained in it.  Tries to be tolerant of logical
	// bitstream sections that are truncated/woogie.
	//
	// return: -1) error
	//          0) OK

	//@Throws(JOrbisException::class)
	fun open(`is`: SyncStream, initial: ByteArray?, ibytes: Int): Int {
		return open_callbacks(`is`, initial, ibytes)
	}

	//@Throws(JOrbisException::class)
	fun open_callbacks(`is`: SyncStream, initial: ByteArray?, ibytes: Int//, callbacks callbacks
	): Int {
		val ret: Int
		datasource = `is`

		oy.init()

		// perhaps some data was previously read into a buffer for testing
		// against other stream types.  Allow initialization from this
		// previously read data (as we may be reading from a non-seekable
		// stream)
		if (initial != null) {
			val index = oy.buffer(ibytes)
			arraycopy(initial, 0, oy.data, index, ibytes)
			oy.wrote(ibytes)
		}
		// can we seek? Stevens suggests the seek test was portable
		if (`is`.isSeekable) {
			ret = open_seekable()
		} else {
			ret = open_nonseekable()
		}
		if (ret != 0) {
			datasource = null
			clear()
		}
		return ret
	}

	// How many logical bitstreams in this physical bitstream?
	fun streams(): Int {
		return links
	}

	// Is the FILE * associated with vf seekable?
	fun isSeekable(): Boolean {
		return seekable
	}

	// returns the bitrate for a given logical bitstream or the entire
	// physical bitstream.  If the file is open for random access, it will
	// find the *actual* average bitrate.  If the file is streaming, it
	// returns the nominal bitrate (if set) else the average of the
	// upper/lower bounds (if set) else -1 (unset).
	//
	// If you want the actual bitrate field settings, get them from the
	// vorbis_info structs

	fun bitrate(i: Int): Int {
		if (i >= links)
			return -1
		if (!seekable && i != 0)
			return bitrate(0)
		if (i < 0) {
			var bits: Long = 0
			for (j in 0..links - 1) {
				bits += (offsets[j + 1] - dataoffsets[j]) * 8
			}
			return rint((bits / time_total(-1)).toDouble()).toInt()
		} else {
			if (seekable) {
				// return the actual bitrate
				return rint(((offsets[i + 1] - dataoffsets[i]) * 8 / time_total(i)).toDouble()).toInt()
			} else {
				// return nominal if set
				if (info[i].bitrate_nominal > 0) {
					return info[i].bitrate_nominal
				} else {
					if (info[i].bitrate_upper > 0) {
						if (info[i].bitrate_lower > 0) {
							return (info[i].bitrate_upper + info[i].bitrate_lower) / 2
						} else {
							return info[i].bitrate_upper
						}
					}
					return -1
				}
			}
		}
	}

	// returns the actual bitrate since last call.  returns -1 if no
	// additional data to offer since last call (or at beginning of stream)
	fun bitrate_instant(): Int {
		val _link = if (seekable) current_link else 0
		if (samptrack == 0f)
			return -1
		val ret = (bittrack / samptrack * info[_link].rate + .5).toInt()
		bittrack = 0f
		samptrack = 0f
		return ret
	}

	fun serialnumber(i: Int): Int {
		if (i >= links)
			return -1
		if (!seekable && i >= 0)
			return serialnumber(-1)
		if (i < 0) {
			return current_serialno
		} else {
			return serialnos[i]
		}
	}

	// returns: total raw (compressed) length of content if i==-1
	//          raw (compressed) length of that logical bitstream for i==0 to n
	//          -1 if the stream is not seekable (we can't know the length)

	fun raw_total(i: Int): Long {
		if (!seekable || i >= links)
			return (-1).toLong()
		if (i < 0) {
			var acc: Long = 0 // bug?
			for (j in 0..links - 1) {
				acc += raw_total(j)
			}
			return acc
		} else {
			return offsets[i + 1] - offsets[i]
		}
	}

	// returns: total PCM length (samples) of content if i==-1
	//          PCM length (samples) of that logical bitstream for i==0 to n
	//          -1 if the stream is not seekable (we can't know the length)
	fun pcm_total(i: Int): Long {
		if (!seekable || i >= links)
			return (-1).toLong()
		if (i < 0) {
			var acc: Long = 0
			for (j in 0..links - 1) {
				acc += pcm_total(j)
			}
			return acc
		} else {
			return pcmlengths[i]
		}
	}

	// returns: total seconds of content if i==-1
	//          seconds in that logical bitstream for i==0 to n
	//          -1 if the stream is not seekable (we can't know the length)
	fun time_total(i: Int): Float {
		if (!seekable || i >= links)
			return (-1).toFloat()
		if (i < 0) {
			var acc = 0f
			for (j in 0..links - 1) {
				acc += time_total(j)
			}
			return acc
		} else {
			return pcmlengths[i].toFloat() / info[i].rate
		}
	}

	// seek to an offset relative to the *compressed* data. This also
	// immediately sucks in and decodes pages to update the PCM cursor. It
	// will cross a logical bitstream boundary, but only if it can't get
	// any packets out of the tail of the bitstream we seek to (so no
	// surprises).
	//
	// returns zero on success, nonzero on failure

	fun raw_seek(pos: Int): Int {
		if (!seekable)
			return -1 // don't dump machine if we can't seek
		if (pos < 0 || pos > offsets[links]) {
			//goto seek_error;
			pcm_offset = -1
			decode_clear()
			return -1
		}

		// clear out decoding machine state
		pcm_offset = -1
		decode_clear()

		// seek
		seek_helper(pos.toLong())

		// we need to make sure the pcm_offset is set.  We use the
		// _fetch_packet helper to process one packet with readp set, then
		// call it until it returns '0' with readp not set (the last packet
		// from a page has the 'granulepos' field set, and that's how the
		// helper updates the offset

		when (process_packet(1)) {
			0 -> {
				// oh, eof. There are no packets remaining.  Set the pcm offset to
				// the end of file
				pcm_offset = pcm_total(-1)
				return 0
			}
			-1 -> {
				// error! missing data or invalid bitstream structure
				//goto seek_error;
				pcm_offset = -1
				decode_clear()
				return -1
			}
			else -> {
			}
		}// all OK
		while (true) {
			when (process_packet(0)) {
				0 ->
					// the offset is set.  If it's a bogus bitstream with no offset
					// information, it's not but that's not our fault.  We still run
					// gracefully, we're just missing the offset
					return 0
				-1 -> {
					// error! missing data or invalid bitstream structure
					//goto seek_error;
					pcm_offset = -1
					decode_clear()
					return -1
				}
				else -> {
				}
			}// continue processing packets
		}

		// seek_error:
		// dump the machine so we're in a known state
		//pcm_offset=-1;
		//decode_clear();
		//return -1;
	}

	// seek to a sample offset relative to the decompressed pcm stream
	// returns zero on success, nonzero on failure

	fun pcm_seek(pos: Long): Int {
		var link = -1
		var total = pcm_total(-1)

		if (!seekable)
			return -1 // don't dump machine if we can't seek
		if (pos < 0 || pos > total) {
			//goto seek_error;
			pcm_offset = -1
			decode_clear()
			return -1
		}

		// which bitstream section does this pcm offset occur in?
		link = links - 1
		while (link >= 0) {
			total -= pcmlengths[link]
			if (pos >= total)
				break
			link--
		}

		// search within the logical bitstream for the page with the highest
		// pcm_pos preceeding (or equal to) pos.  There is a danger here;
		// missing pages or incorrect frame number information in the
		// bitstream could make our task impossible.  Account for that (it
		// would be an error condition)
		run {
			val target = pos - total
			var end = offsets[link + 1]
			var begin = offsets[link]
			var best = begin.toInt()

			val og = Page()
			while (begin < end) {
				val bisect: Long
				val ret: Int

				if (end - begin < CHUNKSIZE) {
					bisect = begin
				} else {
					bisect = (end + begin) / 2
				}

				seek_helper(bisect)
				ret = get_next_page(og, end - bisect)

				if (ret == -1) {
					end = bisect
				} else {
					val granulepos = og.granulepos()
					if (granulepos < target) {
						best = ret // raw offset of packet with granulepos
						begin = offset // raw offset of next packet
					} else {
						end = bisect
					}
				}
			}
			// found our page. seek to it (call raw_seek).
			if (raw_seek(best) != 0) {
				//goto seek_error;
				pcm_offset = -1
				decode_clear()
				return -1
			}
		}

		// verify result
		if (pcm_offset >= pos) {
			//goto seek_error;
			pcm_offset = -1
			decode_clear()
			return -1
		}
		if (pos > pcm_total(-1)) {
			//goto seek_error;
			pcm_offset = -1
			decode_clear()
			return -1
		}

		// discard samples until we reach the desired position. Crossing a
		// logical bitstream boundary with abandon is OK.
		while (pcm_offset < pos) {
			val target = (pos - pcm_offset).toInt()
			val _pcm = Array<Array<FloatArray>>(1) { arrayOf() }
			val _index = IntArray(getInfo(-1)!!.channels)
			var samples = vd.synthesis_pcmout(_pcm, _index)

			if (samples > target)
				samples = target
			vd.synthesis_read(samples)
			pcm_offset += samples.toLong()

			if (samples < target)
				if (process_packet(1) == 0) {
					pcm_offset = pcm_total(-1) // eof
				}
		}
		return 0

		// seek_error:
		// dump machine so we're in a known state
		//pcm_offset=-1;
		//decode_clear();
		//return -1;
	}

	// seek to a playback time relative to the decompressed pcm stream
	// returns zero on success, nonzero on failure
	fun time_seek(seconds: Float): Int {
		// translate time to PCM position and call pcm_seek

		var link = -1
		var pcm_total = pcm_total(-1)
		var time_total = time_total(-1)

		if (!seekable)
			return -1 // don't dump machine if we can't seek
		if (seconds < 0 || seconds > time_total) {
			//goto seek_error;
			pcm_offset = -1
			decode_clear()
			return -1
		}

		// which bitstream section does this time offset occur in?
		link = links - 1
		while (link >= 0) {
			pcm_total -= pcmlengths[link]
			time_total -= time_total(link)
			if (seconds >= time_total)
				break
			link--
		}

		// enough information to convert time offset to pcm offset
		run {
			val target = (pcm_total + (seconds - time_total) * info[link].rate).toLong()
			return pcm_seek(target)
		}

		//seek_error:
		// dump machine so we're in a known state
		//pcm_offset=-1;
		//decode_clear();
		//return -1;
	}

	// tell the current stream offset cursor.  Note that seek followed by
	// tell will likely not give the set offset due to caching
	fun raw_tell(): Long {
		return offset
	}

	// return PCM offset (sample) of next PCM sample to be read
	fun pcm_tell(): Long {
		return pcm_offset
	}

	// return time offset (seconds) of next PCM sample to be read
	fun time_tell(): Float {
		// translate time to PCM position and call pcm_seek

		var link = -1
		var pcm_total: Long = 0
		var time_total = 0f

		if (seekable) {
			pcm_total = pcm_total(-1)
			time_total = time_total(-1)

			// which bitstream section does this time offset occur in?
			link = links - 1
			while (link >= 0) {
				pcm_total -= pcmlengths[link]
				time_total -= time_total(link)
				if (pcm_offset >= pcm_total)
					break
				link--
			}
		}

		return time_total.toFloat() + (pcm_offset - pcm_total).toFloat() / info[link].rate
	}

	//  link:   -1) return the vorbis_info struct for the bitstream section
	//              currently being decoded
	//         0-n) to request information for a specific bitstream section
	//
	// In the case of a non-seekable bitstream, any call returns the
	// current bitstream.  NULL in the case that the machine is not
	// initialized

	fun getInfo(link: Int): Info? {
		if (seekable) {
			if (link < 0) {
				if (decode_ready) {
					return info[current_link]
				} else {
					return null
				}
			} else {
				if (link >= links) {
					return null
				} else {
					return info[link]
				}
			}
		} else {
			if (decode_ready) {
				return info[0]
			} else {
				return null
			}
		}
	}

	fun getComment(link: Int): Comment? {
		if (seekable) {
			if (link < 0) {
				if (decode_ready) {
					return comment[current_link]
				} else {
					return null
				}
			} else {
				if (link >= links) {
					return null
				} else {
					return comment[link]
				}
			}
		} else {
			if (decode_ready) {
				return comment[0]
			} else {
				return null
			}
		}
	}

	fun host_is_big_endian(): Int {
		return 1
		//    short pattern = 0xbabe;
		//    unsigned char *bytewise = (unsigned char *)&pattern;
		//    if (bytewise[0] == 0xba) return 1;
		//    assert(bytewise[0] == 0xbe);
		//    return 0;
	}

	// up to this point, everything could more or less hide the multiple
	// logical bitstream nature of chaining from the toplevel application
	// if the toplevel application didn't particularly care.  However, at
	// the point that we actually read audio back, the multiple-section
	// nature must surface: Multiple bitstream sections do not necessarily
	// have to have the same number of channels or sampling rate.
	//
	// read returns the sequential logical bitstream number currently
	// being decoded along with the PCM data in order that the toplevel
	// application can take action on channel/sample rate changes.  This
	// number will be incremented even for streamed (non-seekable) streams
	// (for seekable streams, it represents the actual logical bitstream
	// index within the physical bitstream.  Note that the accessor
	// functions above are aware of this dichotomy).
	//
	// input values: buffer) a buffer to hold packed PCM data for return
	//               length) the byte length requested to be placed into buffer
	//               bigendianp) should the data be packed LSB first (0) or
	//                           MSB first (1)
	//               word) word size for output.  currently 1 (byte) or
	//                     2 (16 bit short)
	//
	// return values: -1) error/hole in data
	//                 0) EOF
	//                 n) number of bytes of PCM actually returned.  The
	//                    below works on a packet-by-packet basis, so the
	//                    return length is not related to the 'length' passed
	//                    in, just guaranteed to fit.
	//
	// *section) set to the logical bitstream number

	fun read(buffer: ByteArray, length: Int, bigendianp: Int, word: Int, sgned: Int,
			 bitstream: IntArray?): Int {
		val host_endian = host_is_big_endian()
		var index = 0

		while (true) {
			if (decode_ready) {
				val pcm: Array<FloatArray>
				val _pcm = Array<Array<FloatArray>>(1) { arrayOf() }
				val _index = IntArray(getInfo(-1)!!.channels)
				var samples = vd.synthesis_pcmout(_pcm, _index)
				pcm = _pcm[0]
				if (samples != 0) {
					// yay! proceed to pack data into the byte buffer
					val channels = getInfo(-1)!!.channels
					val bytespersample = word * channels
					if (samples > length / bytespersample)
						samples = length / bytespersample

					// a tight loop to pack each size
					run {
						var `val`: Int
						if (word == 1) {
							val off = if (sgned != 0) 0 else 128
							for (j in 0..samples - 1) {
								for (i in 0..channels - 1) {
									`val` = (pcm[i][_index[i] + j] * 128.0 + 0.5).toInt()
									if (`val` > 127)
										`val` = 127
									else if (`val` < -128)
										`val` = -128
									buffer[index++] = (`val` + off).toByte()
								}
							}
						} else {
							val off = if (sgned != 0) 0 else 32768

							if (host_endian == bigendianp) {
								if (sgned != 0) {
									for (i in 0..channels - 1) { // It's faster in this order
										val src = _index[i]
										var dest = i
										for (j in 0..samples - 1) {
											`val` = (pcm[i][src + j] * 32768.0 + 0.5).toInt()
											if (`val` > 32767)
												`val` = 32767
											else if (`val` < -32768)
												`val` = -32768
											buffer[dest] = `val`.ushr(8).toByte()
											buffer[dest + 1] = `val`.toByte()
											dest += channels * 2
										}
									}
								} else {
									for (i in 0..channels - 1) {
										val src = pcm[i]
										var dest = i
										for (j in 0..samples - 1) {
											`val` = (src[j] * 32768.0 + 0.5).toInt()
											if (`val` > 32767)
												`val` = 32767
											else if (`val` < -32768)
												`val` = -32768
											buffer[dest] = (`val` + off).ushr(8).toByte()
											buffer[dest + 1] = (`val` + off).toByte()
											dest += channels * 2
										}
									}
								}
							} else if (bigendianp != 0) {
								for (j in 0..samples - 1) {
									for (i in 0..channels - 1) {
										`val` = (pcm[i][j] * 32768.0 + 0.5).toInt()
										if (`val` > 32767)
											`val` = 32767
										else if (`val` < -32768)
											`val` = -32768
										`val` += off
										buffer[index++] = `val`.ushr(8).toByte()
										buffer[index++] = `val`.toByte()
									}
								}
							} else {
								//int val;
								for (j in 0..samples - 1) {
									for (i in 0..channels - 1) {
										`val` = (pcm[i][j] * 32768.0 + 0.5).toInt()
										if (`val` > 32767)
											`val` = 32767
										else if (`val` < -32768)
											`val` = -32768
										`val` += off
										buffer[index++] = `val`.toByte()
										buffer[index++] = `val`.ushr(8).toByte()
									}
								}
							}
						}
					}

					vd.synthesis_read(samples)
					pcm_offset += samples.toLong()
					if (bitstream != null)
						bitstream[0] = current_link
					return samples * bytespersample
				}
			}

			// suck in another packet
			when (process_packet(1)) {
				0 -> return 0
				-1 -> return -1
				else -> {
				}
			}
		}
	}

	//@Throws(IOException::class)
	fun close() {
		datasource!!.close()
	}

//	inner class SeekableInputStream @Throws(IOException::class)
//	constructor(file: String) : InputStream() {
//		var raf: java.io.RandomAccessFile? = null
//		val mode = "r"
//
//		init {
//			raf = java.io.RandomAccessFile(file, mode)
//		}
//
//		//@Throws(IOException::class)
//		override fun read(): Int {
//			return raf!!.read()
//		}
//
//		//@Throws(IOException::class)
//		override fun read(buf: ByteArray): Int {
//			return raf!!.read(buf)
//		}
//
//		//@Throws(IOException::class)
//		override fun read(buf: ByteArray, s: Int, len: Int): Int {
//			return raf!!.read(buf, s, len)
//		}
//
//		//@Throws(IOException::class)
//		override fun skip(n: Long): Long {
//			return raf!!.skipBytes(n.toInt()).toLong()
//		}
//
//		val length: Long
//			//@Throws(IOException::class)
//			get() = raf!!.length()
//
//		//@Throws(IOException::class)
//		fun tell(): Long {
//			return raf!!.filePointer
//		}
//
//		//@Throws(IOException::class)
//		override fun available(): Int {
//			return if (raf!!.length() == raf!!.filePointer) 0 else 1
//		}
//
//		//@Throws(IOException::class)
//		override fun close() {
//			raf!!.close()
//		}
//
//		@Synchronized
//		override fun mark(m: Int) {
//		}
//
//		@Synchronized
//		//@Throws(IOException::class)
//		override fun reset() {
//		}
//
//		override fun markSupported(): Boolean {
//			return false
//		}
//
//		//@Throws(IOException::class)
//		fun seek(pos: Long) {
//			raf!!.seek(pos)
//		}
//	}

	companion object {
		val CHUNKSIZE = 8500
		val SEEK_SET = 0
		val SEEK_CUR = 1
		val SEEK_END = 2

		val OV_FALSE = -1
		val OV_EOF = -2
		val OV_HOLE = -3

		val OV_EREAD = -128
		val OV_EFAULT = -129
		val OV_EIMPL = -130
		val OV_EINVAL = -131
		val OV_ENOTVORBIS = -132
		val OV_EBADHEADER = -133
		val OV_EVERSION = -134
		val OV_ENOTAUDIO = -135
		val OV_EBADPACKET = -136
		val OV_EBADLINK = -137
		val OV_ENOSEEK = -138

		fun fseek(fis: SyncStream, off: Long, whence: Int): Int {
			val sis = fis
			try {
				if (whence == SEEK_SET) {
					sis.position = off
				} else if (whence == SEEK_END) {
					sis.position = sis.length - off
				} else {
				}
			} catch (e: Exception) {
			}

			return 0
		}

		fun ftell(fis: SyncStream): Long {
			try {
				return fis.position
			} catch (e: Exception) {
			}

			return 0
		}
	}

}

// @TODO:
val SyncStream.isSeekable: Boolean get() = true
