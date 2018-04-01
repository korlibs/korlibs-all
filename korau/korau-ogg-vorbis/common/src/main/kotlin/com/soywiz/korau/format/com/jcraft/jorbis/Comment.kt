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

import com.soywiz.korau.format.com.jcraft.jogg.Buffer
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.toString

class Comment {
    var user_comments: Array<ByteArray?>? = null
    var comment_lengths: IntArray? = null
    var comments: Int = 0
    var vendor: ByteArray? = null

    fun init() {
        user_comments = null
        comments = 0
        vendor = null
    }

    fun unpack(opb: Buffer): Int {
        val vendorlen = opb.read(32)
        if (vendorlen < 0) {
            clear()
            return -1
        }
        vendor = ByteArray(vendorlen + 1)
        opb.read(vendor!!, vendorlen)
        comments = opb.read(32)
        if (comments < 0) {
            clear()
            return -1
        }
        user_comments = arrayOfNulls<ByteArray>(comments + 1)
        comment_lengths = IntArray(comments + 1)

        for (i in 0..comments - 1) {
            val len = opb.read(32)
            if (len < 0) {
                clear()
                return -1
            }
            comment_lengths!![i] = len
            user_comments!![i] = ByteArray(len + 1)
            opb.read(user_comments!![i]!!, len)
        }
        if (opb.read(1) != 1) {
            clear()
            return -1

        }
        return 0
    }

    fun clear() {
        for (i in 0 until comments) user_comments!![i] = null
        user_comments = null
        vendor = null
    }

    private fun ByteArray.toZeroString() = this.toString(Charsets.UTF_8)

    fun getVendor(): String = vendor!!.toZeroString()

    override fun toString(): String {
        var foo = "Vendor: " + getVendor()
        for (i in 0 until comments) {
            foo = "$foo\nComment: ${user_comments!![i]!!.toZeroString()}"
        }
        foo += "\n"
        return foo
    }
}
