/*
 *	Get Audio routines source file
 *
 *	Copyright (c) 1999 Albert L Faber
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/* $Id: GetAudio.java,v 1.30 2012/03/12 18:38:59 kenchis Exp $ */

package com.soywiz.korau.format.net.sourceforge.lame.mp3

import com.soywiz.kmem.fill
import com.soywiz.kmem.toUnsigned
import com.soywiz.korau.format.net.sourceforge.lame.mpg.MPGLib
import com.soywiz.korio.IOException
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readExact
import com.soywiz.korio.stream.skip
import kotlin.experimental.and

class GetAudio(internal var parse: Parse, internal var mpg: MPGLib) {

	/* AIFF Definitions */
	lateinit private var musicin: AsyncStream
	private var hip: MPGLib.mpstr_tag = MPGLib.mpstr_tag()

	suspend fun initInFile(gfp: LameGlobalFlags, inPath: AsyncStream, enc: FrameSkip) {
		try {
			musicin = OpenSndFile(gfp, inPath, enc)
		} catch (e: IOException) {
			e.printStackTrace()
		}

	}

	suspend fun get_audio16(gfp: LameGlobalFlags, buffer: Array<FloatArray>): Int {
		return get_audio_common(gfp, null, buffer)
	}

	suspend private fun get_audio_common(gfp: LameGlobalFlags, buffer: Array<FloatArray>?, buffer16: Array<FloatArray>): Int {
		val num_channels = gfp.inNumChannels
		val buf_tmp16 = Array(2) { FloatArray(1152) }
		val samples_read: Int

		samples_read = read_samples_mp3(gfp, musicin, if (buffer != null) buf_tmp16 else buffer16)
		if (samples_read < 0) return samples_read

		if (buffer != null) {
			run {
				var i = samples_read
				while (--i >= 0) {
					val value = buf_tmp16[0][i].toInt()
					buffer[0][i] = (value shl 16).toFloat()
				}
			}
			if (num_channels == 2) {
				var i = samples_read
				while (--i >= 0) {
					val value = buf_tmp16[1][i].toInt()
					buffer[1][i] = (value shl 16).toFloat()
				}
			} else if (num_channels == 1) {
				buffer[1].fill(0f, 0, samples_read)
			} else {
				throw RuntimeException("Channels must be 1 or 2")
			}
		}

		return samples_read
	}

	suspend internal fun read_samples_mp3(gfp: LameGlobalFlags, musicin: AsyncStream, mpg123pcm: Array<FloatArray>): Int {
		val out: Int

		out = lame_decode_fromfile(musicin, mpg123pcm[0], mpg123pcm[1], parse.mp3InputData)

		if (out < 0) {
			mpg123pcm[0].fill(0.toShort().toFloat())
			mpg123pcm[1].fill(0.toShort().toFloat())
			return 0
		}

		if (gfp.inNumChannels != parse.mp3InputData.stereo)
			throw RuntimeException("number of channels has changed")
		if (gfp.inSampleRate != parse.mp3InputData.samplerate)
			throw RuntimeException("sample frequency has changed")
		return out
	}

	//@Throws(IOException::class)
	suspend private fun OpenSndFile(gfp: LameGlobalFlags, musicin2: AsyncStream, enc: FrameSkip): AsyncStream {

		/* set the defaults from info in case we cannot determine them from file */
		gfp.num_samples = -1

		musicin = musicin2

		if (-1 == lame_decode_initfile(musicin, parse.mp3InputData, enc)) {
			throw RuntimeException("Error reading headers in mp3 input file $musicin2.")
		}
		gfp.inNumChannels = parse.mp3InputData.stereo
		gfp.inSampleRate = parse.mp3InputData.samplerate
		gfp.num_samples = parse.mp3InputData.numSamples

		if (gfp.num_samples == -1) {

			val flen = musicin2.getLength()
			if (flen >= 0L) {
				if (parse.mp3InputData.bitrate > 0) {
					val totalseconds = flen.toDouble() * 8.0 / (1000.0 * parse.mp3InputData.bitrate)
					val tmp_num_samples = (totalseconds * gfp.inSampleRate).toInt()

					gfp.num_samples = tmp_num_samples
					parse.mp3InputData.numSamples = tmp_num_samples
				}
			}
		}
		return musicin
	}

	private fun check_aid(header: ByteArray): Boolean {
		return header.size >= 4 && header[0] == 'A'.toByte() && header[1] == 'i'.toByte() && header[2] == 'D'.toByte() && header[3].toInt() == 1
	}

	private fun is_syncword_mp123(headerptr: ByteArray): Boolean {
		val p = 0

		if (headerptr[p + 0].toUnsigned() and 0xFF != 0xFF) return false /* first 8 bits must be '1' */
		if (headerptr[p + 1].toUnsigned() and 0xE0 != 0xE0) return false /* next 3 bits are also */
		if (headerptr[p + 1].toUnsigned() and 0x18 == 0x08) return false /* no MPEG-1, -2 or -2.5 */

		parse.layer = when (headerptr[p + 1].toInt() and 0x06) {
			0x02 -> 3
			0x04 -> 2
			0x06 -> 1
			else -> return false // illegal layer
		}
		if (headerptr[p + 1].toUnsigned() and 0x06 == 0x00) return false /* no Layer I, II and III */
		if (headerptr[p + 2].toUnsigned() and 0xF0 == 0xF0) return false /* bad bitrate */
		if (headerptr[p + 2].toUnsigned() and 0x0C == 0x0C) return false /* no sample frequency with (32,44.1,48)/(1,2,4) */
		if (headerptr[p + 1].toUnsigned() and 0x18 == 0x18 && headerptr[p + 1].toUnsigned() and 0x06 == 0x04 && abl2[headerptr[p + 2].toUnsigned() shr 4].toInt() and (1 shl (headerptr[p + 3].toUnsigned() shr 6)) != 0)
			return false
		if (headerptr[p + 3].toUnsigned() and 3 == 2) return false /* reserved enphasis mode */
		return true
	}

	suspend private fun lame_decode_initfile(fd: AsyncStream, mp3data: MP3Data, enc: FrameSkip): Int {
		val buf = ByteArray(100)
		val pcm_l = FloatArray(1152)
		val pcm_r = FloatArray(1152)
		var freeformat = false

		mpg.hip_decode_exit(hip)
		hip = mpg.hip_decode_init()

		var len = 4
		try {
			fd.readExact(buf, 0, len)
		} catch (e: IOException) {
			e.printStackTrace()
			return -1 /* failed */
		}

		if (buf[0] == 'I'.toByte() && buf[1] == 'D'.toByte() && buf[2] == '3'.toByte()) {
			//System.out.println("ID3v2 found. Be aware that the ID3 tag is currently lost when transcoding.");
			len = 6
			try {
				fd.readExact(buf, 0, len)
			} catch (e: IOException) {
				e.printStackTrace()
				return -1 /* failed */
			}

			buf[2] = buf[2] and 127
			buf[3] = buf[3] and 127
			buf[4] = buf[4] and 127
			buf[5] = buf[5] and 127
			len = (((buf[2].toUnsigned() shl 7) + buf[3] shl 7) + buf[4] shl 7) + buf[5]
			try {
				fd.skip(len)
			} catch (e: IOException) {
				e.printStackTrace()
				return -1 /* failed */
			}

			len = 4
			try {
				fd.readExact(buf, 0, len)
			} catch (e: IOException) {
				e.printStackTrace()
				return -1 /* failed */
			}

		}
		if (check_aid(buf)) {
			try {
				fd.readExact(buf, 0, 2)
			} catch (e: IOException) {
				e.printStackTrace()
				return -1 /* failed */
			}

			val aid_header = (buf[0].toUnsigned()) + 256 * (buf[1].toUnsigned())
			//System.out.printf("Album ID found.  length=%d \n", aid_header);
			/* skip rest of AID, except for 6 bytes we have already read */
			try {
				fd.skip(aid_header - 6)
			} catch (e: IOException) {
				e.printStackTrace()
				return -1 /* failed */
			}

			/* read 4 more bytes to set up buffer for MP3 header check */
			try {
				fd.readExact(buf, 0, len)
			} catch (e: IOException) {
				e.printStackTrace()
				return -1 /* failed */
			}

		}
		len = 4
		while (!is_syncword_mp123(buf)) {
			var i: Int
			i = 0
			while (i < len - 1) {
				buf[i] = buf[i + 1]
				i++
			}
			try {
				fd.readExact(buf, len - 1, 1)
			} catch (e: IOException) {
				e.printStackTrace()
				return -1 /* failed */
			}

		}

		if (buf[2].toUnsigned() and 0xf0 == 0) {
			//System.out.println("Input file is freeformat.");
			freeformat = true
		}

		var ret = mpg.hip_decode1_headers(hip, buf, len, pcm_l, pcm_r, mp3data, enc)
		if (ret == -1) return -1

		while (!mp3data.header_parsed) {
			try {
				fd.readExact(buf, 0, buf.size)
			} catch (e: IOException) {
				e.printStackTrace()
				return -1 /* failed */
			}

			ret = mpg.hip_decode1_headers(hip, buf, buf.size, pcm_l, pcm_r, mp3data, enc)
			if (ret == -1) return -1
		}

		if (mp3data.bitrate == 0 && !freeformat) return lame_decode_initfile(fd, mp3data, enc)
		if (mp3data.totalFrames <= 0) mp3data.numSamples = -1

		return 0
	}

	suspend private fun lame_decode_fromfile(fd: AsyncStream, pcm_l: FloatArray, pcm_r: FloatArray, mp3data: MP3Data): Int {
		var len = 0
		val buf = ByteArray(1024)

		/* first see if we still have data buffered in the decoder: */
		var ret = mpg.hip_decode1_headers(hip, buf, len, pcm_l, pcm_r, mp3data, FrameSkip())
		if (ret != 0) return ret

		/* read until we get a valid output frame */
		while (true) {
			try {
				len = fd.read(buf, 0, 1024)
			} catch (e: IOException) {
				e.printStackTrace()
				return -1
			}

			if (len <= 0) {
				/* we are done reading the file, but check for buffered data */
				ret = mpg.hip_decode1_headers(hip, buf, 0, pcm_l, pcm_r, mp3data, FrameSkip())
				if (ret <= 0) {
					mpg.hip_decode_exit(hip)
					/* release mp3decoder memory */
					return -1 /* done with file */
				}
				break
			}

			ret = mpg.hip_decode1_headers(hip, buf, len, pcm_l, pcm_r, mp3data, FrameSkip())
			if (ret == -1) {
				mpg.hip_decode_exit(hip)
				/* release mp3decoder memory */
				return -1
			}
			if (ret > 0)
				break
		}
		return ret
	}

	companion object {
		private val abl2 = charArrayOf(0.toChar(), 7.toChar(), 7.toChar(), 7.toChar(), 0.toChar(), 7.toChar(), 0.toChar(), 0.toChar(), 0.toChar(), 0.toChar(), 0.toChar(), 8.toChar(), 8.toChar(), 8.toChar(), 8.toChar(), 8.toChar())
	}
}
