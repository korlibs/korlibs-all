/*
 *      Xing VBR tagging for LAME.
 *
 *      Copyright (c) 1999 A.L. Faber
 *      Copyright (c) 2001 Jonathan Dee
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package com.soywiz.korau.format.net.sourceforge.lame.mp3

import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.toUnsigned
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.toString

class VBRTag {
    companion object {
        const val NUMTOCENTRIES = 100
        const private val FRAMES_FLAG = 0x0001
        const private val BYTES_FLAG = 0x0002
        const private val TOC_FLAG = 0x0004
        const private val VBR_SCALE_FLAG = 0x0008
        const private val VBRTag0 = "Xing"
        const private val VBRTag1 = "Info"
        private val ISO_8859_1: Charset = Charsets.ISO_8859_1
    }

    private fun extractInteger(buf: ByteArray, bufPos: Int): Int {
        var x = buf[bufPos + 0].toUnsigned()
        x = x shl 8
        x = x or (buf[bufPos + 1].toUnsigned())
        x = x shl 8
        x = x or (buf[bufPos + 2].toUnsigned())
        x = x shl 8
        x = x or (buf[bufPos + 3].toUnsigned())
        return x
    }

    private fun isVbrTag(buf: ByteArray, bufPos: Int): Boolean {

        return buf.copyOfRange(bufPos, bufPos + VBRTag0.length).toString(ISO_8859_1) == VBRTag0 ||
                buf.copyOfRange(bufPos, bufPos + VBRTag1.length).toString(ISO_8859_1) == VBRTag1
    }

    fun getVbrTag(buf: ByteArray): VBRTagData? {
        val pTagData = VBRTagData()
        var bufPos = 0

        /* get Vbr header data */
        pTagData.flags = 0

        /* get selected MPEG header data */
        val hId = buf[bufPos + 1].toUnsigned() shr 3 and 1
        val hSrIndex = buf[bufPos + 2].toUnsigned() shr 2 and 3
        val hMode = buf[bufPos + 3].toUnsigned() shr 6 and 3
        var hBitrate = buf[bufPos + 2].toUnsigned() shr 4 and 0xf
        hBitrate = Tables.bitrate_table[hId][hBitrate]

        /* check for FFE syncword */
        pTagData.samprate = if (buf[bufPos + 1].toUnsigned() shr 4 == 0xE) Tables.samplerate_table[2][hSrIndex] else Tables.samplerate_table[hId][hSrIndex]

        if (hId != 0) {
            bufPos += if (hMode != 3) 32 + 4 else 17 + 4 // mpeg1
        } else {
            bufPos += if (hMode != 3) 17 + 4 else 9 + 4 // mpeg2
        }

        if (!isVbrTag(buf, bufPos)) return null

        bufPos += 4

        pTagData.hId = hId

        /* get flags */
        pTagData.flags = extractInteger(buf, bufPos)
        val head_flags = pTagData.flags
        bufPos += 4

        if (head_flags and FRAMES_FLAG != 0) {
            pTagData.frames = extractInteger(buf, bufPos)
            bufPos += 4
        }

        if (head_flags and BYTES_FLAG != 0) {
            pTagData.bytes = extractInteger(buf, bufPos)
            bufPos += 4
        }

        if (head_flags and TOC_FLAG != 0) {
            arraycopy(buf, bufPos + 0, pTagData.toc, 0, NUMTOCENTRIES)
            bufPos += NUMTOCENTRIES
        }

        pTagData.vbrScale = -1

        if (head_flags and VBR_SCALE_FLAG != 0) {
            pTagData.vbrScale = extractInteger(buf, bufPos)
            bufPos += 4
        }

        pTagData.headersize = (hId + 1) * 72000 * hBitrate / pTagData.samprate

        bufPos += 21
        var encDelay = buf[bufPos + 0].toUnsigned() shl 4
        encDelay += buf[bufPos + 1].toUnsigned() shr 4
        var encPadding = buf[bufPos + 1].toUnsigned() and 0x0F shl 8
        encPadding += buf[bufPos + 2].toUnsigned() and 0xff
        if (encDelay < 0 || encDelay > 3000) encDelay = -1
        if (encPadding < 0 || encPadding > 3000) encPadding = -1

        pTagData.encDelay = encDelay
        pTagData.encPadding = encPadding

        return pTagData
    }
}
