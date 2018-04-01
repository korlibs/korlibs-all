/**
 * Mpeg Layer-1,2,3 audio decoder
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

class Decode {

    private val tab = TabInit
    private val dct64 = DCT64()

    private fun writeSampleClipped(sum: Float, clip: Int, out: FloatArray, outPos: Int): Int {
        if (sum > 32767.0) {
            out[outPos] = 32767f
            return clip + 1
        } else if (sum < -32768.0) {
            out[outPos] = -32768f
            return clip + 1
        } else {
            out[outPos] = (if (sum > 0) sum + 0.5 else sum - 0.5).toInt().toFloat()
            return clip
        }
    }

    private fun writeSampleUnclipped(sum: Float, out: FloatArray, outPos: Int) {
        out[outPos] = sum
    }

    fun synth1to1mono(mp: MPGLib.mpstr_tag, bandPtr: FloatArray, bandPos: Int, out: FloatArray, pnt: MPGLib.ProcessedBytes): Int {
        val samples = FloatArray(64)

        val clip = synth_1to1(mp, bandPtr, bandPos, 0, samples, MPGLib.ProcessedBytes())

        var i = 0
        while (i < samples.size) {
            out[pnt.pb++] = samples[i]
            i += 2
        }
        return clip
    }

    fun synth1to1monoUnclipped(mp: MPGLib.mpstr_tag, bandPtr: FloatArray, bandPos: Int, out: FloatArray, pnt: MPGLib.ProcessedBytes) {
        val samples = FloatArray(64)

        synth_1to1_unclipped(mp, bandPtr, bandPos, 0, samples, MPGLib.ProcessedBytes())

        var i = 0
        while (i < samples.size) {
            out[pnt.pb++] = samples[i]
            i += 2
        }
    }

    fun synth_1to1(mp: MPGLib.mpstr_tag, bandPtr: FloatArray, bandPos: Int, ch: Int, out: FloatArray, pnt: MPGLib.ProcessedBytes): Int {
        val b0: FloatArray
        var clip = 0
        val bo1: Int

        if (0 == ch) {
            mp.synth_bo--
            mp.synth_bo = mp.synth_bo and 0xf
        } else {
            pnt.pb++
        }

        if (mp.synth_bo and 0x1 != 0) {
            b0 = mp.synth_buffs[ch][0]
            bo1 = mp.synth_bo
            val bufs = FloatArray(0x40)
            dct64.dct64_1(mp.synth_buffs[ch][1], mp.synth_bo + 1 and 0xf, mp.synth_buffs[ch][0], mp.synth_bo, bufs, 0x20, bandPtr, bandPos, tab.pnts)
        } else {
            b0 = mp.synth_buffs[ch][1]
            bo1 = mp.synth_bo + 1
            val bufs = FloatArray(0x40)
            dct64.dct64_1(mp.synth_buffs[ch][0], mp.synth_bo, mp.synth_buffs[ch][1], mp.synth_bo + 1, bufs, 0x20, bandPtr, bandPos, tab.pnts)
        }

        run {
            var window = 16 - bo1

            var b0Pos = 0
            run {
                var j = 16
                while (j != 0) {
                    var sum = 0f
                    sum += tab.decwin[window + 0x0] * b0[b0Pos + 0x0]
                    sum -= tab.decwin[window + 0x1] * b0[b0Pos + 0x1]
                    sum += tab.decwin[window + 0x2] * b0[b0Pos + 0x2]
                    sum -= tab.decwin[window + 0x3] * b0[b0Pos + 0x3]
                    sum += tab.decwin[window + 0x4] * b0[b0Pos + 0x4]
                    sum -= tab.decwin[window + 0x5] * b0[b0Pos + 0x5]
                    sum += tab.decwin[window + 0x6] * b0[b0Pos + 0x6]
                    sum -= tab.decwin[window + 0x7] * b0[b0Pos + 0x7]
                    sum += tab.decwin[window + 0x8] * b0[b0Pos + 0x8]
                    sum -= tab.decwin[window + 0x9] * b0[b0Pos + 0x9]
                    sum += tab.decwin[window + 0xA] * b0[b0Pos + 0xA]
                    sum -= tab.decwin[window + 0xB] * b0[b0Pos + 0xB]
                    sum += tab.decwin[window + 0xC] * b0[b0Pos + 0xC]
                    sum -= tab.decwin[window + 0xD] * b0[b0Pos + 0xD]
                    sum += tab.decwin[window + 0xE] * b0[b0Pos + 0xE]
                    sum -= tab.decwin[window + 0xF] * b0[b0Pos + 0xF]
                    clip = writeSampleClipped(sum, clip, out, pnt.pb)
                    j--
                    b0Pos += 0x10
                    window += 0x20
                    pnt.pb += 2
                }
            }

            run {
                var sum = 0f
                sum += tab.decwin[window + 0x0] * b0[b0Pos + 0x0]
                sum += tab.decwin[window + 0x2] * b0[b0Pos + 0x2]
                sum += tab.decwin[window + 0x4] * b0[b0Pos + 0x4]
                sum += tab.decwin[window + 0x6] * b0[b0Pos + 0x6]
                sum += tab.decwin[window + 0x8] * b0[b0Pos + 0x8]
                sum += tab.decwin[window + 0xA] * b0[b0Pos + 0xA]
                sum += tab.decwin[window + 0xC] * b0[b0Pos + 0xC]
                sum += tab.decwin[window + 0xE] * b0[b0Pos + 0xE]
                clip = writeSampleClipped(sum, clip, out, pnt.pb)
                b0Pos -= 0x10
                window -= 0x20
                pnt.pb += 2
            }
            window += bo1 shl 1

            var j = 15
            while (j != 0) {
                var sum: Float = 0f
                sum -= tab.decwin[window + -0x1] * b0[b0Pos + 0x0]
                sum -= tab.decwin[window + -0x2] * b0[b0Pos + 0x1]
                sum -= tab.decwin[window + -0x3] * b0[b0Pos + 0x2]
                sum -= tab.decwin[window + -0x4] * b0[b0Pos + 0x3]
                sum -= tab.decwin[window + -0x5] * b0[b0Pos + 0x4]
                sum -= tab.decwin[window + -0x6] * b0[b0Pos + 0x5]
                sum -= tab.decwin[window + -0x7] * b0[b0Pos + 0x6]
                sum -= tab.decwin[window + -0x8] * b0[b0Pos + 0x7]
                sum -= tab.decwin[window + -0x9] * b0[b0Pos + 0x8]
                sum -= tab.decwin[window + -0xA] * b0[b0Pos + 0x9]
                sum -= tab.decwin[window + -0xB] * b0[b0Pos + 0xA]
                sum -= tab.decwin[window + -0xC] * b0[b0Pos + 0xB]
                sum -= tab.decwin[window + -0xD] * b0[b0Pos + 0xC]
                sum -= tab.decwin[window + -0xE] * b0[b0Pos + 0xD]
                sum -= tab.decwin[window + -0xF] * b0[b0Pos + 0xE]
                sum -= tab.decwin[window + -0x0] * b0[b0Pos + 0xF]

                clip = writeSampleClipped(sum, clip, out, pnt.pb)
                j--
                b0Pos -= 0x10
                window -= 0x20
                pnt.pb += 2
            }
        }
        if (ch == 1) {
            pnt.pb--
        }
        return clip
    }

    fun synth_1to1_unclipped(mp: MPGLib.mpstr_tag, bandPtr: FloatArray, bandPos: Int, ch: Int, out: FloatArray, pnt: MPGLib.ProcessedBytes) {
        val b0: FloatArray
        val bo1: Int

        if (0 == ch) {
            mp.synth_bo--
            mp.synth_bo = mp.synth_bo and 0xf
        } else {
            pnt.pb++
        }

        if (mp.synth_bo and 0x1 != 0) {
            b0 = mp.synth_buffs[ch][0]
            bo1 = mp.synth_bo
            val bufs = FloatArray(0x40)
            dct64.dct64_1(mp.synth_buffs[ch][1], mp.synth_bo + 1 and 0xf, mp.synth_buffs[ch][0], mp.synth_bo, bufs, 0x20, bandPtr, bandPos, tab.pnts)
        } else {
            b0 = mp.synth_buffs[ch][1]
            bo1 = mp.synth_bo + 1
            val bufs = FloatArray(0x40)
            dct64.dct64_1(mp.synth_buffs[ch][0], mp.synth_bo, mp.synth_buffs[ch][1], mp.synth_bo + 1, bufs, 0x20, bandPtr, bandPos, tab.pnts)
        }

        run {
            var window = 16 - bo1

            var b0Pos = 0
            run {
                var j = 16
                while (j != 0) {
                    var sum: Float
                    sum = tab.decwin[window + 0x0] * b0[b0Pos + 0x0]
                    sum -= tab.decwin[window + 0x1] * b0[b0Pos + 0x1]
                    sum += tab.decwin[window + 0x2] * b0[b0Pos + 0x2]
                    sum -= tab.decwin[window + 0x3] * b0[b0Pos + 0x3]
                    sum += tab.decwin[window + 0x4] * b0[b0Pos + 0x4]
                    sum -= tab.decwin[window + 0x5] * b0[b0Pos + 0x5]
                    sum += tab.decwin[window + 0x6] * b0[b0Pos + 0x6]
                    sum -= tab.decwin[window + 0x7] * b0[b0Pos + 0x7]
                    sum += tab.decwin[window + 0x8] * b0[b0Pos + 0x8]
                    sum -= tab.decwin[window + 0x9] * b0[b0Pos + 0x9]
                    sum += tab.decwin[window + 0xA] * b0[b0Pos + 0xA]
                    sum -= tab.decwin[window + 0xB] * b0[b0Pos + 0xB]
                    sum += tab.decwin[window + 0xC] * b0[b0Pos + 0xC]
                    sum -= tab.decwin[window + 0xD] * b0[b0Pos + 0xD]
                    sum += tab.decwin[window + 0xE] * b0[b0Pos + 0xE]
                    sum -= tab.decwin[window + 0xF] * b0[b0Pos + 0xF]
                    writeSampleUnclipped(sum, out, pnt.pb)
                    j--
                    b0Pos += 0x10
                    window += 0x20
                    pnt.pb += 2
                }
            }

            run {
                var sum: Float
                sum = tab.decwin[window + 0x0] * b0[b0Pos + 0x0]
                sum += tab.decwin[window + 0x2] * b0[b0Pos + 0x2]
                sum += tab.decwin[window + 0x4] * b0[b0Pos + 0x4]
                sum += tab.decwin[window + 0x6] * b0[b0Pos + 0x6]
                sum += tab.decwin[window + 0x8] * b0[b0Pos + 0x8]
                sum += tab.decwin[window + 0xA] * b0[b0Pos + 0xA]
                sum += tab.decwin[window + 0xC] * b0[b0Pos + 0xC]
                sum += tab.decwin[window + 0xE] * b0[b0Pos + 0xE]
                writeSampleUnclipped(sum, out, pnt.pb)
                b0Pos -= 0x10
                window -= 0x20
                pnt.pb += 2
            }
            window += bo1 shl 1

            var j = 15
            while (j != 0) {
                var sum: Float
                sum = -tab.decwin[window + -0x1] * b0[b0Pos + 0x0]
                sum -= tab.decwin[window + -0x2] * b0[b0Pos + 0x1]
                sum -= tab.decwin[window + -0x3] * b0[b0Pos + 0x2]
                sum -= tab.decwin[window + -0x4] * b0[b0Pos + 0x3]
                sum -= tab.decwin[window + -0x5] * b0[b0Pos + 0x4]
                sum -= tab.decwin[window + -0x6] * b0[b0Pos + 0x5]
                sum -= tab.decwin[window + -0x7] * b0[b0Pos + 0x6]
                sum -= tab.decwin[window + -0x8] * b0[b0Pos + 0x7]
                sum -= tab.decwin[window + -0x9] * b0[b0Pos + 0x8]
                sum -= tab.decwin[window + -0xA] * b0[b0Pos + 0x9]
                sum -= tab.decwin[window + -0xB] * b0[b0Pos + 0xA]
                sum -= tab.decwin[window + -0xC] * b0[b0Pos + 0xB]
                sum -= tab.decwin[window + -0xD] * b0[b0Pos + 0xC]
                sum -= tab.decwin[window + -0xE] * b0[b0Pos + 0xD]
                sum -= tab.decwin[window + -0xF] * b0[b0Pos + 0xE]
                sum -= tab.decwin[window + -0x0] * b0[b0Pos + 0xF]

                writeSampleUnclipped(sum, out, pnt.pb)
                j--
                b0Pos -= 0x10
                window -= 0x20
                pnt.pb += 2
            }
        }
        if (ch == 1) {
            pnt.pb--
        }
    }
}
