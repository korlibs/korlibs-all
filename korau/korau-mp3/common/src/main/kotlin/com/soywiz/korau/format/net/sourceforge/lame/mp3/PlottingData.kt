/*
 *      GTK plotting routines source file
 *
 *      Copyright (c) 1999 Mark Taylor
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

/**
 * used by the frame analyzer
 */
class PlottingData {
    var mpg123xr = Array(2) { Array(2) { DoubleArray(576) } }
    var sfb = Array(2) { Array(2) { DoubleArray(SBMAX_l) } }
    var sfb_s = Array(2) { Array(2) { DoubleArray(3 * SBMAX_s) } }
    var qss = Array(2) { IntArray(2) }
    var big_values = Array(2) { IntArray(2) }
    var sub_gain = Array(2) { Array(2) { IntArray(3) } }
    var scalefac_scale = Array(2) { IntArray(2) }
    var preflag = Array(2) { IntArray(2) }
    var mpg123blocktype = Array(2) { IntArray(2) }
    var mixed = Array(2) { IntArray(2) }
    var mainbits = Array(2) { IntArray(2) }
    var sfbits = Array(2) { IntArray(2) }
    var stereo: Int = 0
    var js: Int = 0
    var ms_stereo: Int = 0
    var i_stereo: Int = 0
    var emph: Int = 0
    var bitrate: Int = 0
    var sampfreq: Int = 0
    var maindata: Int = 0
    var crc: Int = 0
    var padding: Int = 0
    var scfsi = IntArray(2)

    companion object {
        val SBMAX_l = 22
        val SBMAX_s = 13
    }
}
