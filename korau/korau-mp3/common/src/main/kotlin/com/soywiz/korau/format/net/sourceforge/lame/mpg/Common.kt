/**
 * Some common bitstream operations
 *
 *
 * Copyright (C) 1999-2010 The L.A.M.E. project
 *
 *
 * Initially written by Michael Hipp, see also AUTHORS and README.
 *
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 *
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.

 * @author Ken H�ndel
 */
package com.soywiz.korau.format.net.sourceforge.lame.mpg

import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.toUnsigned
import com.soywiz.korio.JvmField
import com.soywiz.korio.lang.Console

class Common {
    companion object {
        val tabsel_123 = arrayOf(
                arrayOf(
                        intArrayOf(0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448),
                        intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384),
                        intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)
                ),
                arrayOf(
                        intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256),
                        intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160),
                        intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)
                )
        )

        val freqs = intArrayOf(44100, 48000, 32000, 22050, 24000, 16000, 11025, 12000, 8000)
        private val MAX_INPUT_FRAMESIZE = 4096
    }

    @JvmField
    var muls = Array(27) { FloatArray(64) }

    fun head_check(head: Long, check_layer: Int): Boolean {
        /* bits 13-14 = layer 3 */
        val nLayer = (4 - (head shr 17 and 3)).toInt()

        if (head and 0xffe00000L != 0xffe00000L) return false /* syncword */
        if (nLayer == 4) return false
        if (check_layer > 0 && nLayer != check_layer) return false
        if (head shr 12 and 0xf == 0xfL) return false/* bits 16,17,18,19 = 1111 invalid bitrate */
        if (head shr 10 and 0x3 == 0x3L) return false /* bits 20,21 = 11 invalid sampling freq */
        if (head and 0x3 == 0x2L) return false/* invalid emphasis */

        return true
    }

    /**
     * decode a header and write the information into the frame structure
     */
    fun decode_header(fr: Frame, newhead: Long): Int {

        if (newhead and (1 shl 20) != 0L) {
            fr.lsf = if (newhead and (1 shl 19) != 0L) 0x0 else 0x1
            fr.mpeg25 = false
        } else {
            fr.lsf = 1
            fr.mpeg25 = true
        }

        fr.lay = (4 - (newhead shr 17 and 3)).toInt()
        if (newhead shr 10 and 0x3 == 0x3L) throw RuntimeException("Stream error")
        if (fr.mpeg25) {
            fr.sampling_frequency = (6 + (newhead shr 10 and 0x3)).toInt()
        } else {
            fr.sampling_frequency = ((newhead shr 10 and 0x3) + fr.lsf * 3).toInt()
        }

        fr.error_protection = newhead shr 16 and 0x1 == 0L

        if (fr.mpeg25) fr.bitrate_index = (newhead shr 12 and 0xf).toInt() /* allow Bitrate change for 2.5 ... */

        fr.bitrate_index = (newhead shr 12 and 0xf).toInt()
        fr.padding = (newhead shr 9 and 0x1).toInt()
        fr.extension = (newhead shr 8 and 0x1).toInt()
        fr.mode = (newhead shr 6 and 0x3).toInt()
        fr.mode_ext = (newhead shr 4 and 0x3).toInt()
        fr.copyright = (newhead shr 3 and 0x1).toInt()
        fr.original = (newhead shr 2 and 0x1).toInt()
        fr.emphasis = (newhead and 0x3).toInt()

        fr.stereo = if (fr.mode == MPG123.MPG_MD_MONO) 1 else 2

        when (fr.lay) {
            1 -> {
                fr.framesize = tabsel_123[fr.lsf][0][fr.bitrate_index] * 12000
                fr.framesize /= freqs[fr.sampling_frequency]
                fr.framesize = (fr.framesize + fr.padding shl 2) - 4
                fr.down_sample = 0
                fr.down_sample_sblimit = MPG123.SBLIMIT shr fr.down_sample
            }

            2 -> {
                fr.framesize = tabsel_123[fr.lsf][1][fr.bitrate_index] * 144000
                fr.framesize /= freqs[fr.sampling_frequency]
                fr.framesize += fr.padding - 4
                fr.down_sample = 0
                fr.down_sample_sblimit = MPG123.SBLIMIT shr fr.down_sample
            }

            3 -> {
                if (fr.framesize > MAX_INPUT_FRAMESIZE) {
                    Console.error("Frame size too big.")
                    fr.framesize = MAX_INPUT_FRAMESIZE
                    return 0
                }

                if (fr.bitrate_index == 0)
                    fr.framesize = 0
                else {
                    fr.framesize = tabsel_123[fr.lsf][2][fr.bitrate_index] * 144000
                    fr.framesize /= freqs[fr.sampling_frequency] shl fr.lsf
                    fr.framesize = fr.framesize + fr.padding - 4
                }
            }
            else -> {
                Console.error("Sorry, layer ${fr.lay} not supported")
                return 0
            }
        }
        /* print_header(fr); */

        return 1
    }

    fun getbits(mp: MPGLib.mpstr_tag, number_of_bits: Int): Int {
        var rval: Long

        if (number_of_bits <= 0 || null == mp.wordpointer) return 0


        rval = (mp.wordpointer[mp.wordpointerPos + 0].toUnsigned()).toLong()
        rval = rval shl 8
        rval = rval or (mp.wordpointer[mp.wordpointerPos + 1].toUnsigned()).toLong()
        rval = rval shl 8
        rval = rval or (mp.wordpointer[mp.wordpointerPos + 2].toUnsigned()).toLong()
        rval = (rval shl mp.bitindex)
        rval = rval and 0xffffffL

        mp.bitindex += number_of_bits

        rval = rval shr (24 - number_of_bits)

        mp.wordpointerPos += mp.bitindex shr 3
        mp.bitindex = mp.bitindex and 7

        return rval.toInt()
    }

    fun getbits_fast(mp: MPGLib.mpstr_tag, number_of_bits: Int): Int {
        var rval: Long

        rval = (mp.wordpointer[mp.wordpointerPos + 0].toUnsigned()).toLong()
        rval = rval shl 8
        rval = rval or (mp.wordpointer[mp.wordpointerPos + 1].toUnsigned()).toLong()
        rval = rval shl mp.bitindex
        rval = rval and 0xffffL
        mp.bitindex += number_of_bits

        rval = rval shr (16 - number_of_bits)

        mp.wordpointerPos += mp.bitindex shr 3
        mp.bitindex = mp.bitindex and 7

        return rval.toInt()
    }

    fun set_pointer(mp: MPGLib.mpstr_tag, backstep: Int): Int {
        if (mp.fsizeold < 0 && backstep > 0) {
            Console.error("hip: Can't step back $backstep bytes!")
            return MPGLib.MP3_ERR
        }
        val bsbufold = mp.bsspace[1 - mp.bsnum]
        val bsbufoldPos = 512
        mp.wordpointerPos -= backstep
        if (backstep != 0)
            arraycopy(bsbufold, bsbufoldPos + mp.fsizeold - backstep, mp.wordpointer, mp.wordpointerPos, backstep)
        mp.bitindex = 0
        return MPGLib.MP3_OK
    }
}
