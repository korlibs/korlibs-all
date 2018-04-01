/**
 * LAME MP3 encoding engine
 *
 *
 * Copyright (c) 1999-2000 Mark Taylor
 * Copyright (c) 2003 Olcios
 * Copyright (c) 2008 Robert Hegemann
 *
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.

 * @author Ken H�ndel
 */
package com.soywiz.korau.format.net.sourceforge.lame.mpg

import com.soywiz.korau.format.net.sourceforge.lame.mp3.FrameSkip
import com.soywiz.korau.format.net.sourceforge.lame.mp3.MP3Data
import com.soywiz.korau.format.net.sourceforge.lame.mp3.PlottingData
import com.soywiz.korio.JvmField
import com.soywiz.korio.lang.assert

class MPGLib(internal var interf: Interface) {
    companion object {
        const val MP3_ERR = -1
        const val MP3_OK = 0
        const val MP3_NEED_MORE = 1

        private val smpls = arrayOf(intArrayOf(0, 384, 1152, 1152), intArrayOf(0, 384, 1152, 576))
        private val OUTSIZE_CLIPPED = 4096
    }

    /* copy mono samples */
    protected fun COPY_MONO(
            pcm_l: FloatArray, pcm_lPos: Int,
            processed_samples: Int, p: FloatArray
    ) {
        var lp = pcm_lPos
        var p_samples = 0
        for (i in 0 until processed_samples)
            pcm_l[lp++] = p[p_samples++]
    }

    /* copy stereo samples */
    protected fun COPY_STEREO(
            pcm_l: FloatArray, pcm_lPos: Int, pcm_r: FloatArray,
            pcm_rPos: Int, processed_samples: Int, p: FloatArray
    ) {
        var lp = pcm_lPos
        var rp = pcm_rPos
        var p_samples = 0
        for (i in 0 until processed_samples) {
            pcm_l[lp++] = p[p_samples++]
            pcm_r[rp++] = p[p_samples++]
        }
    }

    private fun decode1_headersB_clipchoice(
            pmp: mpstr_tag, buffer: ByteArray,
            bufferPos: Int, len: Int, pcm_l: FloatArray, pcm_lPos: Int, pcm_r: FloatArray,
            pcm_rPos: Int, mp3data: MP3Data, enc: FrameSkip, p: FloatArray, psize: Int,
            decodeMP3_ptr: IDecoder
    ): Int {

        mp3data.header_parsed = false

        val pb = ProcessedBytes()
        val ret = decodeMP3_ptr.decode(pmp, buffer, bufferPos, len, p, psize, pb)
        var processed_samples = pb.pb
        if (pmp.header_parsed || pmp.fsizeold > 0 || pmp.framesize > 0) {
            mp3data.header_parsed = true
            mp3data.stereo = pmp.fr.stereo
            mp3data.samplerate = Common.freqs[pmp.fr.sampling_frequency]
            mp3data.mode = pmp.fr.mode
            mp3data.mode_ext = pmp.fr.mode_ext
            mp3data.frameSize = smpls[pmp.fr.lsf][pmp.fr.lay]

            /* free format, we need the entire frame before we can determine
             * the bitrate.  If we haven't gotten the entire frame, bitrate=0 */
            if (pmp.fsizeold > 0)
            /* works for free format and fixed, no overrun, temporal results are < 400.e6 */ {
                mp3data.bitrate = (8 * (4 + pmp.fsizeold) * mp3data.samplerate / (1e3 * mp3data.frameSize) + 0.5).toInt()
            }
            else if (pmp.framesize > 0) {
                mp3data.bitrate = (8 * (4 + pmp.framesize) * mp3data.samplerate / (1e3 * mp3data.frameSize) + 0.5).toInt()
            }
            else {
                mp3data.bitrate = Common.tabsel_123[pmp.fr.lsf][pmp.fr.lay - 1][pmp.fr.bitrate_index]
            }


            if (pmp.num_frames > 0) {
                /* Xing VBR header found and num_frames was set */
                mp3data.totalFrames = pmp.num_frames
                mp3data.numSamples = mp3data.frameSize * pmp.num_frames
                enc.encoderDelay = pmp.enc_delay
                enc.encoderPadding = pmp.enc_padding
            }
        }

        when (ret) {
            MP3_OK -> when (pmp.fr.stereo) {
                1 -> COPY_MONO(pcm_l, pcm_lPos, processed_samples, p)
                2 -> {
                    processed_samples = processed_samples shr 1
                    COPY_STEREO(pcm_l, pcm_lPos, pcm_r, pcm_rPos, processed_samples, p)
                }
                else -> {
                    processed_samples = -1
                    assert(false)
                }
            }

            MP3_NEED_MORE -> processed_samples = 0

            MP3_ERR -> processed_samples = -1

            else -> {
                processed_samples = -1
                assert(false)
            }
        }

        return processed_samples
    }

    fun hip_decode_init(): mpstr_tag {
        return interf.InitMP3()
    }

    fun hip_decode_exit(hip: mpstr_tag?): Int {
        if (hip != null) interf.ExitMP3(hip)
        return 0
    }

    fun hip_decode1_headers(
            hip: mpstr_tag?, buffer: ByteArray,
            len: Int,
            pcm_l: FloatArray, pcm_r: FloatArray, mp3data: MP3Data,
            enc: FrameSkip
    ): Int {
        if (hip != null) {
            val dec = object : IDecoder {
                override fun decode(mp: mpstr_tag, `in`: ByteArray, bufferPos: Int, isize: Int, out: FloatArray, osize: Int, done: ProcessedBytes): Int {
                    return interf.decodeMP3(mp, `in`, bufferPos, isize, out, osize, done)
                }
            }
            val out = FloatArray(OUTSIZE_CLIPPED)
            return decode1_headersB_clipchoice(hip, buffer, 0, len, pcm_l, 0, pcm_r, 0, mp3data, enc, out, OUTSIZE_CLIPPED, dec)
        }
        return -1
    }

    interface IDecoder {
        fun decode(mp: mpstr_tag, `in`: ByteArray, bufferPos: Int, isize: Int, out: FloatArray, osize: Int, done: ProcessedBytes): Int
    }

    class buf {
        internal var pnt: ByteArray = byteArrayOf()
        internal var size: Int = 0
        internal var pos: Int = 0
    }

    class mpstr_tag {
        @JvmField var list: ArrayList<buf> = arrayListOf()
        @JvmField var vbr_header: Boolean = false
        @JvmField var num_frames: Int = 0
        @JvmField var enc_delay: Int = 0
        @JvmField var enc_padding: Int = 0
        @JvmField var header_parsed: Boolean = false
        @JvmField var side_parsed: Boolean = false
        @JvmField var data_parsed: Boolean = false
        @JvmField var free_format: Boolean = false
        @JvmField var old_free_format: Boolean = false
        @JvmField var bsize: Int = 0
        @JvmField var framesize: Int = 0
        @JvmField var ssize: Int = 0
        @JvmField var dsize: Int = 0
        @JvmField var fsizeold: Int = 0
        @JvmField var fsizeold_nopadding: Int = 0
        @JvmField var fr = Frame()
        @JvmField var bsspace = Array(2) { ByteArray(MPG123.MAXFRAMESIZE + 1024) }
        @JvmField var hybrid_block = Array(2) { Array(2) { FloatArray(MPG123.SBLIMIT * MPG123.SSLIMIT) } }
        @JvmField var hybrid_blc = IntArray(2)
        @JvmField var header: Long = 0
        @JvmField var bsnum: Int = 0
        @JvmField var synth_buffs = Array(2) { Array(2) { FloatArray(0x110) } }
        @JvmField var synth_bo: Int = 0
        @JvmField var sync_bitstream: Boolean = false
        @JvmField var bitindex: Int = 0
        @JvmField var wordpointer: ByteArray = byteArrayOf(0)
        @JvmField var wordpointerPos: Int = 0
        @JvmField var pinfo: PlottingData = PlottingData()
    }

    class ProcessedBytes {
        @JvmField var pb: Int = 0
    }
}
