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

import com.soywiz.korio.Synchronized
import com.soywiz.korio.math.rint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log
import kotlin.math.sin

internal class Mdct {

    var n: Int = 0
    var log2n: Int = 0

    var trig: FloatArray = floatArrayOf()
    var bitrev: IntArray = intArrayOf()

    var scale: Float = 0.toFloat()

    fun init(n: Int) {
        bitrev = IntArray(n / 4)
        trig = FloatArray(n + n / 4)

        log2n = rint(log(n.toDouble(), 2.0)).toInt()
        this.n = n

        val AE = 0
        val AO = 1
        val BE = AE + n / 2
        val BO = BE + 1
        val CE = BE + n / 2
        val CO = CE + 1
        // trig lookups...
        for (i in 0 until n / 4) {
            trig[AE + i * 2] = cos(PI / n * (4 * i)).toFloat()
            trig[AO + i * 2] = (-sin(PI / n * (4 * i))).toFloat()
            trig[BE + i * 2] = cos(PI / (2 * n) * (2 * i + 1)).toFloat()
            trig[BO + i * 2] = sin(PI / (2 * n) * (2 * i + 1)).toFloat()
        }
        for (i in 0 until n / 8) {
            trig[CE + i * 2] = cos(PI / n * (4 * i + 2)).toFloat()
            trig[CO + i * 2] = (-sin(PI / n * (4 * i + 2))).toFloat()
        }

        run {
            val mask = (1 shl log2n - 1) - 1
            val msb = 1 shl log2n - 2
            for (i in 0..n / 8 - 1) {
                var acc = 0
                var j = 0
                while (msb.ushr(j) != 0) {
                    if (msb.ushr(j) and i != 0) {
                        acc = acc or (1 shl j)
                    }
                    j++
                }
                bitrev[i * 2] = acc.inv() and mask
                //	bitrev[i*2]=((~acc)&mask)-1;
                bitrev[i * 2 + 1] = acc
            }
        }
        scale = 4f / n
    }

    fun clear() {}

    fun forward(`in`: FloatArray, out: FloatArray) {}

    var _x = FloatArray(1024)
    var _w = FloatArray(1024)

    @Synchronized
    fun backward(`in`: FloatArray, out: FloatArray) {
        if (_x.size < n / 2) {
            _x = FloatArray(n / 2)
        }
        if (_w.size < n / 2) {
            _w = FloatArray(n / 2)
        }
        val x = _x
        val w = _w
        val n2 = n.ushr(1)
        val n4 = n.ushr(2)
        val n8 = n.ushr(3)

        // rotate + step 1
        run {
            var inO = 1
            var xO = 0
            var A = n2

            var i: Int
            i = 0
            while (i < n8) {
                A -= 2
                x[xO++] = -`in`[inO + 2] * trig[A + 1] - `in`[inO] * trig[A]
                x[xO++] = `in`[inO] * trig[A + 1] - `in`[inO + 2] * trig[A]
                inO += 4
                i++
            }

            inO = n2 - 4

            i = 0
            while (i < n8) {
                A -= 2
                x[xO++] = `in`[inO] * trig[A + 1] + `in`[inO + 2] * trig[A]
                x[xO++] = `in`[inO] * trig[A] - `in`[inO + 2] * trig[A + 1]
                inO -= 4
                i++
            }
        }

        val xxx = mdct_kernel(x, w, n, n2, n4, n8)
        var xx = 0

        // step 8

        run {
            var B = n2
            var o1 = n4
            var o2 = o1 - 1
            var o3 = n4 + n2
            var o4 = o3 - 1

            for (i in 0..n4 - 1) {
                val temp1 = xxx[xx] * trig[B + 1] - xxx[xx + 1] * trig[B]
                val temp2 = -(xxx[xx] * trig[B] + xxx[xx + 1] * trig[B + 1])

                out[o1] = -temp1
                out[o2] = temp1
                out[o3] = temp2
                out[o4] = temp2

                o1++
                o2--
                o3++
                o4--
                xx += 2
                B += 2
            }
        }
    }

    private fun mdct_kernel(x: FloatArray, w: FloatArray, n: Int, n2: Int, n4: Int, n8: Int): FloatArray {
        var x = x
        var w = w
        // step 2

        var xA = n4
        var xB = 0
        var w2 = n4
        var A = n2

        run {
            var i = 0
            while (i < n4) {
                val x0 = x[xA] - x[xB]
                val x1: Float
                w[w2 + i] = x[xA++] + x[xB++]

                x1 = x[xA] - x[xB]
                A -= 4

                w[i++] = x0 * trig[A] + x1 * trig[A + 1]
                w[i] = x1 * trig[A] - x0 * trig[A + 1]

                w[w2 + i] = x[xA++] + x[xB++]
                i++
            }
        }

        // step 3

        run {
            for (i in 0..log2n - 3 - 1) {
                var k0 = n.ushr(i + 2)
                val k1 = 1 shl i + 3
                var wbase = n2 - 2

                A = 0
                val temp: FloatArray

                for (r in 0..k0.ushr(2) - 1) {
                    var w1 = wbase
                    w2 = w1 - (k0 shr 1)
                    val AEv = trig[A]
                    var wA: Float
                    val AOv = trig[A + 1]
                    var wB: Float
                    wbase -= 2

                    k0++
                    for (s in 0..(2 shl i) - 1) {
                        wB = w[w1] - w[w2]
                        x[w1] = w[w1] + w[w2]

                        wA = w[++w1] - w[++w2]
                        x[w1] = w[w1] + w[w2]

                        x[w2] = wA * AEv - wB * AOv
                        x[w2 - 1] = wB * AEv + wA * AOv

                        w1 -= k0
                        w2 -= k0
                    }
                    k0--
                    A += k1
                }

                temp = w
                w = x
                x = temp
            }
        }

        // step 4, 5, 6, 7
        run {
            var C = n
            var bit = 0
            var x1 = 0
            var x2 = n2 - 1

            for (i in 0..n8 - 1) {
                val t1 = bitrev[bit++]
                val t2 = bitrev[bit++]

                val wA = w[t1] - w[t2 + 1]
                val wB = w[t1 - 1] + w[t2]
                val wC = w[t1] + w[t2 + 1]
                val wD = w[t1 - 1] - w[t2]

                val wACE = wA * trig[C]
                val wBCE = wB * trig[C++]
                val wACO = wA * trig[C]
                val wBCO = wB * trig[C++]

                x[x1++] = (wC + wACO + wBCE) * .5f
                x[x2--] = (-wD + wBCO - wACE) * .5f
                x[x1++] = (wD + wBCO - wACE) * .5f
                x[x2--] = (wC - wACO - wBCE) * .5f
            }
        }
        return x
    }
}
