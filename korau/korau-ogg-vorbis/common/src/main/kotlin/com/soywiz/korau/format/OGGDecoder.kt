package com.soywiz.korau.format

import com.soywiz.kds.LinkedList
import com.soywiz.kmem.arraycopy
import com.soywiz.korau.format.com.jcraft.jogg.Packet
import com.soywiz.korau.format.com.jcraft.jogg.Page
import com.soywiz.korau.format.com.jcraft.jogg.StreamState
import com.soywiz.korau.format.com.jcraft.jogg.SyncState
import com.soywiz.korau.format.com.jcraft.jorbis.Block
import com.soywiz.korau.format.com.jcraft.jorbis.Comment
import com.soywiz.korau.format.com.jcraft.jorbis.DspState
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.buffered
import com.soywiz.korio.util.clamp
import kotlin.math.min

object OGGDecoder : OggBase() {
	val BUFSIZE = 4096 * 2

	data class OutInfo(
		var rate: Int = 0,
		var channels: Int = 0
	)

	suspend private fun stream(outInfo: OutInfo, data: AsyncStream) = asyncGenerate<ShortArray> {
		val oy = SyncState()
		val os = StreamState()
		val og = Page()
		val op = Packet()

		val vi = com.soywiz.korau.format.com.jcraft.jorbis.Info()
		val vc = Comment()
		val vd = DspState()
		val vb = Block(vd)
		var eos = false

		var buffer: ByteArray? = null
		var bytes = 0

		oy.init()

		var index = oy.buffer(BUFSIZE)
		val rbytes = data.read(oy.data, index, BUFSIZE)
		oy.wrote(rbytes)

		if (oy.pageout(og) != 1) {
			invalidOp("Input does not appear to be an Ogg bitstream.")
		}

		os.init(og.serialno())
		os.reset()

		vi.init()
		vc.init()

		if (os.pagein(og) < 0) invalidOp("Error reading first page of Ogg bitstream data.")
		if (os.packetout(op) != 1) invalidOp("Error reading initial header packet.")
		if (vi.synthesis_headerin(vc, op) < 0) invalidOp("This Ogg bitstream does not contain Vorbis audio data.")

		var i = 0
		while (i < 2) {
			while (i < 2) {
				var result = oy.pageout(og)
				if (result == 0) break // Need more data
				if (result == 1) {
					os.pagein(og)
					while (i < 2) {
						result = os.packetout(op)
						if (result == 0) break
						if (result == -1) invalidOp("Corrupt secondary header.  Exiting.")
						vi.synthesis_headerin(vc, op)
						i++
					}
				}
			}

			if (i == 2) break

			index = oy.buffer(BUFSIZE)
			buffer = oy.data
			bytes = data.read(buffer, index, BUFSIZE)

			if (bytes == 0 && i < 2) invalidOp("End of file before finding all Vorbis headers!")
			oy.wrote(bytes)
		}

		val ptr = vc.user_comments
		val sb = StringBuilder()

		for (j in ptr!!.indices) {
			if (ptr[j] == null) break
			//System.err.println("Comment: " + String(ptr[j], 0, ptr[j].size - 1))
			//sb.append(" " + String(ptr[j]!!, 0, ptr[j]!!.size - 1))
			sb.append(" " + ptr[j]!!.copyOf(ptr[j]!!.size - 1).toString(Charsets.UTF_8))
		}
		//System.err.println("Bitstream is ${vi.channels} channel, ${vi.rate}Hz")
		//System.err.println("Encoded by: ${String(vc.vendor, 0, vc.vendor.size - 1)}\n")

		//acontext.showStatus(sb.toString())

		outInfo.rate = vi.rate
		outInfo.channels = vi.channels

		val convsize = BUFSIZE / vi.channels

		vd.synthesis_init(vi)
		vb.init(vd)

		val _pcmf = Array<Array<FloatArray>>(1) { arrayOf() }
		val _index = IntArray(vi.channels)

		var chained = false
		val BUFSIZE = 4096 * 2
		val convbuffer = ShortArray(convsize / 2)

		while (!eos) {
			while (!eos) {
				var result = oy.pageout(og)
				if (result == 0) break
				if (result != -1) {
					os.pagein(og)

					if (og.granulepos() == 0L) {
						chained = true
						eos = true
						break
					}

					while (true) {
						result = os.packetout(op)
						if (result == 0) break
						if (result != -1) {
							var samples: Int
							// test for success!
							if (vb.synthesis(op) == 0) vd.synthesis_blockin(vb)

							while (true) {
								samples = vd.synthesis_pcmout(_pcmf, _index)
								if (samples <= 0) break
								val pcmf = _pcmf[0]
								val bout = min(samples, convsize)

								i = 0
								while (i < vi.channels) {
									var ptr2 = i
									val mono = _index[i]
									for (j in 0 until bout) {
										convbuffer[ptr2] = (pcmf!![i][mono + j] * 32767.0).toInt().clamp(-32768, 32767).toShort()
										ptr2 += vi.channels
									}
									i++
								}
								if (vi.channels * bout > 0) {
									//println(vi.channels * bout)
									yield(convbuffer.copyOfRange(0, vi.channels * bout))
								}
								vd.synthesis_read(bout)
							}
						}
					}
					if (og.eos() != 0) eos = true
				}
			}

			if (!eos) {
				index = oy.buffer(BUFSIZE)
				buffer = oy.data
				try {
					bytes = data.read(buffer, index, BUFSIZE)
				} catch (e: Exception) {
					e.printStackTrace()
					throw e
				}

				if (bytes == -1) break
				oy.wrote(bytes)
				if (bytes == 0) eos = true
			}
		}

		os.clear()
		vb.clear()
		vd.clear()
		vi.clear()
	}

	suspend override fun decodeStream(data: AsyncStream): AudioStream? {
		val info = OutInfo()
		val ss = stream(info, data.buffered())
		val ssi = ss.iterator()

		var current: ShortArray? = null
		var currentpos: Int = 0
		val items = LinkedList<ShortArray>()

		while (items.isEmpty() && ssi.hasNext()) items += ssi.next()

		return object : AudioStream(info.rate, info.channels) {
			suspend override fun read(out: ShortArray, offset: Int, length: Int): Int {
				if (current == null || currentpos >= current!!.size) {
					while (items.isEmpty() && ssi.hasNext()) items += ssi.next()
					current = if (items.isNotEmpty()) items.removeFirst() else null
					currentpos = 0
				}
				if (current != null) {
					val available = current!!.size - currentpos
					val toread = min(length, available)
					arraycopy(current!!, currentpos, out, offset, toread)
					currentpos += toread
					return toread
				} else {
					return 0
				}
			}
		}
	}
}

fun AudioFormats.registerOggVorbisDecoder(): AudioFormats = this.apply { register(OGGDecoder) }
