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

import kotlin.math.cos
import kotlin.math.sin

class Drft {
    var n: Int = 0
    var trigcache: FloatArray = floatArrayOf()
    var splitcache: IntArray = intArrayOf()

    fun backward(data: FloatArray) {
        if (n == 1)
            return
        drftb1(n, data, trigcache, trigcache, n, splitcache)
    }

    fun init(n: Int) {
        this.n = n
        trigcache = FloatArray(3 * n)
        splitcache = IntArray(32)
        fdrffti(n, trigcache, splitcache)
    }

    fun clear() {
        trigcache = floatArrayOf()
        splitcache = intArrayOf()
    }

    companion object {

        var ntryh = intArrayOf(4, 2, 3, 5)
        var tpi = 6.28318530717958647692528676655900577f
        var hsqt2 = .70710678118654752440084436210485f
        var taui = .86602540378443864676372317075293618f
        var taur = -.5f
        var sqrt2 = 1.4142135623730950488016887242097f

        fun drfti1(n: Int, wa: FloatArray, index: Int, ifac: IntArray) {
            var arg: Float
            var argh: Float
            var argld: Float
            var fi: Float
            var ntry = 0
            var i: Int
            var j = -1
            var k1: Int
            var l1: Int
            var l2: Int
            var ib: Int
            var ld: Int
            var ii: Int
            var ip: Int
            var `is`: Int
            var nq: Int
            var nr: Int
            var ido: Int
            var ipm: Int
            var nfm1: Int
            var nl = n
            var nf = 0

            var state = 101

            loop@ while (true) {
                when (state) {
                    101 -> {
                        j++
                        if (j < 4) {
                            ntry = ntryh[j]
                        } else {
                            ntry += 2
                        }
                        nq = nl / ntry
                        nr = nl - ntry * nq
                        if (nr != 0) {
                            state = 101
                            continue@loop
                        }
                        nf++
                        ifac[nf + 1] = ntry
                        nl = nq
                        if (ntry != 2) {
                            state = 107
                            continue@loop
                        }
                        if (nf == 1) {
                            state = 107
                            continue@loop
                        }

                        i = 1
                        while (i < nf) {
                            ib = nf - i + 1
                            ifac[ib + 1] = ifac[ib]
                            i++
                        }
                        ifac[2] = 2
                        if (nl != 1) {
                            state = 104
                            continue@loop
                        }
                        ifac[0] = n
                        ifac[1] = nf
                        argh = tpi / n
                        `is` = 0
                        nfm1 = nf - 1
                        l1 = 1

                        if (nfm1 == 0)
                            return

                        k1 = 0
                        while (k1 < nfm1) {
                            ip = ifac[k1 + 2]
                            ld = 0
                            l2 = l1 * ip
                            ido = n / l2
                            ipm = ip - 1

                            j = 0
                            while (j < ipm) {
                                ld += l1
                                i = `is`
                                argld = ld.toFloat() * argh
                                fi = 0f
                                ii = 2
                                while (ii < ido) {
                                    fi += 1f
                                    arg = fi * argld
                                    wa[index + i++] = cos(arg.toDouble()).toFloat()
                                    wa[index + i++] = sin(arg.toDouble()).toFloat()
                                    ii += 2
                                }
                                `is` += ido
                                j++
                            }
                            l1 = l2
                            k1++
                        }
                        continue@loop
                    }
                    104 -> {
                        nq = nl / ntry
                        nr = nl - ntry * nq
                        if (nr != 0) {
                            state = 101
                            continue@loop
                        }
                        nf++
                        ifac[nf + 1] = ntry
                        nl = nq
                        if (ntry != 2) {
                            state = 107
                            continue@loop
                        }
                        if (nf == 1) {
                            state = 107
                            continue@loop
                        }
                        i = 1
                        while (i < nf) {
                            ib = nf - i + 1
                            ifac[ib + 1] = ifac[ib]
                            i++
                        }
                        ifac[2] = 2
                        if (nl != 1) {
                            state = 104
                            continue@loop
                        }
                        ifac[0] = n
                        ifac[1] = nf
                        argh = tpi / n
                        `is` = 0
                        nfm1 = nf - 1
                        l1 = 1
                        if (nfm1 == 0)
                            return
                        k1 = 0
                        while (k1 < nfm1) {
                            ip = ifac[k1 + 2]
                            ld = 0
                            l2 = l1 * ip
                            ido = n / l2
                            ipm = ip - 1
                            j = 0
                            while (j < ipm) {
                                ld += l1
                                i = `is`
                                argld = ld.toFloat() * argh
                                fi = 0f
                                ii = 2
                                while (ii < ido) {
                                    fi += 1f
                                    arg = fi * argld
                                    wa[index + i++] = cos(arg.toDouble()).toFloat()
                                    wa[index + i++] = sin(arg.toDouble()).toFloat()
                                    ii += 2
                                }
                                `is` += ido
                                j++
                            }
                            l1 = l2
                            k1++
                        }
                        continue@loop
                    }
                    107 -> {
                        if (nl != 1) {
                            state = 104
                            continue@loop
                        }
                        ifac[0] = n
                        ifac[1] = nf
                        argh = tpi / n
                        `is` = 0
                        nfm1 = nf - 1
                        l1 = 1
                        if (nfm1 == 0)
                            return
                        k1 = 0
                        while (k1 < nfm1) {
                            ip = ifac[k1 + 2]
                            ld = 0
                            l2 = l1 * ip
                            ido = n / l2
                            ipm = ip - 1
                            j = 0
                            while (j < ipm) {
                                ld += l1
                                i = `is`
                                argld = ld.toFloat() * argh
                                fi = 0f
                                ii = 2
                                while (ii < ido) {
                                    fi += 1f
                                    arg = fi * argld
                                    wa[index + i++] = cos(arg.toDouble()).toFloat()
                                    wa[index + i++] = sin(arg.toDouble()).toFloat()
                                    ii += 2
                                }
                                `is` += ido
                                j++
                            }
                            l1 = l2
                            k1++
                        }
                        continue@loop
                    }
                }
            }
        }

        fun fdrffti(n: Int, wsave: FloatArray, ifac: IntArray) {
            if (n == 1)
                return
            drfti1(n, wsave, n, ifac)
        }

        fun dradf2(ido: Int, l1: Int, cc: FloatArray, ch: FloatArray, wa1: FloatArray,
                   index: Int) {
            var i: Int
            var k: Int
            var ti2: Float
            var tr2: Float
            val t0: Int
            var t1: Int
            var t2: Int
            var t3: Int
            var t4: Int
            var t5: Int
            var t6: Int

            t1 = 0
            t2 = l1 * ido
            t0 = t2
            t3 = ido shl 1
            k = 0
            while (k < l1) {
                ch[t1 shl 1] = cc[t1] + cc[t2]
                ch[(t1 shl 1) + t3 - 1] = cc[t1] - cc[t2]
                t1 += ido
                t2 += ido
                k++
            }

            if (ido < 2)
                return

            if (ido != 2) {
                t1 = 0
                t2 = t0
                k = 0
                while (k < l1) {
                    t3 = t2
                    t4 = (t1 shl 1) + (ido shl 1)
                    t5 = t1
                    t6 = t1 + t1
                    i = 2
                    while (i < ido) {
                        t3 += 2
                        t4 -= 2
                        t5 += 2
                        t6 += 2
                        tr2 = wa1[index + i - 2] * cc[t3 - 1] + wa1[index + i - 1] * cc[t3]
                        ti2 = wa1[index + i - 2] * cc[t3] - wa1[index + i - 1] * cc[t3 - 1]
                        ch[t6] = cc[t5] + ti2
                        ch[t4] = ti2 - cc[t5]
                        ch[t6 - 1] = cc[t5 - 1] + tr2
                        ch[t4 - 1] = cc[t5 - 1] - tr2
                        i += 2
                    }
                    t1 += ido
                    t2 += ido
                    k++
                }
                if (ido % 2 == 1)
                    return
            }

            t1 = ido
            t2 = t1 - 1
            t3 = t2
            t2 += t0
            k = 0
            while (k < l1) {
                ch[t1] = -cc[t2]
                ch[t1 - 1] = cc[t3]
                t1 += ido shl 1
                t2 += ido
                t3 += ido
                k++
            }
        }

        fun dradf4(ido: Int, l1: Int, cc: FloatArray, ch: FloatArray, wa1: FloatArray,
                   index1: Int, wa2: FloatArray, index2: Int, wa3: FloatArray, index3: Int) {
            var i: Int
            var k: Int
            val t0: Int
            var t1: Int
            var t2: Int
            var t3: Int
            var t4: Int
            var t5: Int
            var t6: Int
            var ci2: Float
            var ci3: Float
            var ci4: Float
            var cr2: Float
            var cr3: Float
            var cr4: Float
            var ti1: Float
            var ti2: Float
            var ti3: Float
            var ti4: Float
            var tr1: Float
            var tr2: Float
            var tr3: Float
            var tr4: Float
            t0 = l1 * ido

            t1 = t0
            t4 = t1 shl 1
            t2 = t1 + (t1 shl 1)
            t3 = 0

            k = 0
            while (k < l1) {
                tr1 = cc[t1] + cc[t2]
                tr2 = cc[t3] + cc[t4]

                t5 = t3 shl 2
                ch[t5] = tr1 + tr2
                ch[(ido shl 2) + t5 - 1] = tr2 - tr1
                t5 += ido shl 1
                ch[t5 - 1] = cc[t3] - cc[t4]
                ch[t5] = cc[t2] - cc[t1]

                t1 += ido
                t2 += ido
                t3 += ido
                t4 += ido
                k++
            }
            if (ido < 2)
                return

            if (ido != 2) {
                t1 = 0
                k = 0
                while (k < l1) {
                    t2 = t1
                    t4 = t1 shl 2
                    t6 = ido shl 1
                    t5 = t6 + t4
                    i = 2
                    while (i < ido) {
                        t2 += 2
                        t3 = 2
                        t4 += 2
                        t5 -= 2

                        t3 += t0
                        cr2 = wa1[index1 + i - 2] * cc[t3 - 1] + wa1[index1 + i - 1] * cc[t3]
                        ci2 = wa1[index1 + i - 2] * cc[t3] - wa1[index1 + i - 1] * cc[t3 - 1]
                        t3 += t0
                        cr3 = wa2[index2 + i - 2] * cc[t3 - 1] + wa2[index2 + i - 1] * cc[t3]
                        ci3 = wa2[index2 + i - 2] * cc[t3] - wa2[index2 + i - 1] * cc[t3 - 1]
                        t3 += t0
                        cr4 = wa3[index3 + i - 2] * cc[t3 - 1] + wa3[index3 + i - 1] * cc[t3]
                        ci4 = wa3[index3 + i - 2] * cc[t3] - wa3[index3 + i - 1] * cc[t3 - 1]

                        tr1 = cr2 + cr4
                        tr4 = cr4 - cr2
                        ti1 = ci2 + ci4
                        ti4 = ci2 - ci4

                        ti2 = cc[t2] + ci3
                        ti3 = cc[t2] - ci3
                        tr2 = cc[t2 - 1] + cr3
                        tr3 = cc[t2 - 1] - cr3

                        ch[t4 - 1] = tr1 + tr2
                        ch[t4] = ti1 + ti2

                        ch[t5 - 1] = tr3 - ti4
                        ch[t5] = tr4 - ti3

                        ch[t4 + t6 - 1] = ti4 + tr3
                        ch[t4 + t6] = tr4 + ti3

                        ch[t5 + t6 - 1] = tr2 - tr1
                        ch[t5 + t6] = ti1 - ti2
                        i += 2
                    }
                    t1 += ido
                    k++
                }
                if (ido and 1 != 0)
                    return
            }

            t1 = t0 + ido - 1
            t2 = t1 + (t0 shl 1)
            t3 = ido shl 2
            t4 = ido
            t5 = ido shl 1
            t6 = ido

            k = 0
            while (k < l1) {
                ti1 = -hsqt2 * (cc[t1] + cc[t2])
                tr1 = hsqt2 * (cc[t1] - cc[t2])

                ch[t4 - 1] = tr1 + cc[t6 - 1]
                ch[t4 + t5 - 1] = cc[t6 - 1] - tr1

                ch[t4] = ti1 - cc[t1 + t0]
                ch[t4 + t5] = ti1 + cc[t1 + t0]

                t1 += ido
                t2 += ido
                t4 += t3
                t6 += ido
                k++
            }
        }

        fun dradfg(ido: Int, ip: Int, l1: Int, idl1: Int, cc: FloatArray, c1: FloatArray,
                   c2: FloatArray, ch: FloatArray, ch2: FloatArray, wa: FloatArray, index: Int) {
            var idij: Int
            val ipph: Int
            var i: Int
            var j: Int
            var k: Int
            var l: Int
            var ic: Int
            var ik: Int
            var `is`: Int
            val t0: Int
            var t1: Int
            var t2 = 0
            var t3: Int
            var t4: Int
            var t5: Int
            var t6: Int
            var t7: Int
            var t8: Int
            var t9: Int
            val t10: Int
            var dc2: Float
            var ai1: Float
            var ai2: Float
            var ar1: Float
            var ar2: Float
            var ds2: Float
            val nbd: Int
            var dcp = 0f
            val arg: Float
            var dsp = 0f
            var ar1h: Float
            var ar2h: Float
            val idp2: Int
            val ipp2: Int

            arg = tpi / ip.toFloat()
            dcp = cos(arg.toDouble()).toFloat()
            dsp = sin(arg.toDouble()).toFloat()
            ipph = ip + 1 shr 1
            ipp2 = ip
            idp2 = ido
            nbd = ido - 1 shr 1
            t0 = l1 * ido
            t10 = ip * ido

            var state = 100
            loop@ while (true) {
                when (state) {
                    101 -> {
                        if (ido == 1) {
                            state = 119
                            continue@loop
                        }
                        ik = 0
                        while (ik < idl1) {
                            ch2[ik] = c2[ik]
                            ik++
                        }

                        t1 = 0
                        j = 1
                        while (j < ip) {
                            t1 += t0
                            t2 = t1
                            k = 0
                            while (k < l1) {
                                ch[t2] = c1[t2]
                                t2 += ido
                                k++
                            }
                            j++
                        }

                        `is` = -ido
                        t1 = 0
                        if (nbd > l1) {
                            j = 1
                            while (j < ip) {
                                t1 += t0
                                `is` += ido
                                t2 = -ido + t1
                                k = 0
                                while (k < l1) {
                                    idij = `is` - 1
                                    t2 += ido
                                    t3 = t2
                                    i = 2
                                    while (i < ido) {
                                        idij += 2
                                        t3 += 2
                                        ch[t3 - 1] = wa[index + idij - 1] * c1[t3 - 1] + wa[index + idij] * c1[t3]
                                        ch[t3] = wa[index + idij - 1] * c1[t3] - wa[index + idij] * c1[t3 - 1]
                                        i += 2
                                    }
                                    k++
                                }
                                j++
                            }
                        } else {

                            j = 1
                            while (j < ip) {
                                `is` += ido
                                idij = `is` - 1
                                t1 += t0
                                t2 = t1
                                i = 2
                                while (i < ido) {
                                    idij += 2
                                    t2 += 2
                                    t3 = t2
                                    k = 0
                                    while (k < l1) {
                                        ch[t3 - 1] = wa[index + idij - 1] * c1[t3 - 1] + wa[index + idij] * c1[t3]
                                        ch[t3] = wa[index + idij - 1] * c1[t3] - wa[index + idij] * c1[t3 - 1]
                                        t3 += ido
                                        k++
                                    }
                                    i += 2
                                }
                                j++
                            }
                        }

                        t1 = 0
                        t2 = ipp2 * t0
                        if (nbd < l1) {
                            j = 1
                            while (j < ipph) {
                                t1 += t0
                                t2 -= t0
                                t3 = t1
                                t4 = t2
                                i = 2
                                while (i < ido) {
                                    t3 += 2
                                    t4 += 2
                                    t5 = t3 - ido
                                    t6 = t4 - ido
                                    k = 0
                                    while (k < l1) {
                                        t5 += ido
                                        t6 += ido
                                        c1[t5 - 1] = ch[t5 - 1] + ch[t6 - 1]
                                        c1[t6 - 1] = ch[t5] - ch[t6]
                                        c1[t5] = ch[t5] + ch[t6]
                                        c1[t6] = ch[t6 - 1] - ch[t5 - 1]
                                        k++
                                    }
                                    i += 2
                                }
                                j++
                            }
                        } else {
                            j = 1
                            while (j < ipph) {
                                t1 += t0
                                t2 -= t0
                                t3 = t1
                                t4 = t2
                                k = 0
                                while (k < l1) {
                                    t5 = t3
                                    t6 = t4
                                    i = 2
                                    while (i < ido) {
                                        t5 += 2
                                        t6 += 2
                                        c1[t5 - 1] = ch[t5 - 1] + ch[t6 - 1]
                                        c1[t6 - 1] = ch[t5] - ch[t6]
                                        c1[t5] = ch[t5] + ch[t6]
                                        c1[t6] = ch[t6 - 1] - ch[t5 - 1]
                                        i += 2
                                    }
                                    t3 += ido
                                    t4 += ido
                                    k++
                                }
                                j++
                            }
                        }
                        ik = 0
                        while (ik < idl1) {
                            c2[ik] = ch2[ik]
                            ik++
                        }

                        t1 = 0
                        t2 = ipp2 * idl1
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1 - ido
                            t4 = t2 - ido
                            k = 0
                            while (k < l1) {
                                t3 += ido
                                t4 += ido
                                c1[t3] = ch[t3] + ch[t4]
                                c1[t4] = ch[t4] - ch[t3]
                                k++
                            }
                            j++
                        }

                        ar1 = 1f
                        ai1 = 0f
                        t1 = 0
                        t2 = ipp2 * idl1
                        t3 = (ip - 1) * idl1
                        l = 1
                        while (l < ipph) {
                            t1 += idl1
                            t2 -= idl1
                            ar1h = dcp * ar1 - dsp * ai1
                            ai1 = dcp * ai1 + dsp * ar1
                            ar1 = ar1h
                            t4 = t1
                            t5 = t2
                            t6 = t3
                            t7 = idl1

                            ik = 0
                            while (ik < idl1) {
                                ch2[t4++] = c2[ik] + ar1 * c2[t7++]
                                ch2[t5++] = ai1 * c2[t6++]
                                ik++
                            }

                            dc2 = ar1
                            ds2 = ai1
                            ar2 = ar1
                            ai2 = ai1

                            t4 = idl1
                            t5 = (ipp2 - 1) * idl1
                            j = 2
                            while (j < ipph) {
                                t4 += idl1
                                t5 -= idl1

                                ar2h = dc2 * ar2 - ds2 * ai2
                                ai2 = dc2 * ai2 + ds2 * ar2
                                ar2 = ar2h

                                t6 = t1
                                t7 = t2
                                t8 = t4
                                t9 = t5
                                ik = 0
                                while (ik < idl1) {
                                    ch2[t6++] += ar2 * c2[t8++]
                                    ch2[t7++] += ai2 * c2[t9++]
                                    ik++
                                }
                                j++
                            }
                            l++
                        }
                        t1 = 0
                        j = 1
                        while (j < ipph) {
                            t1 += idl1
                            t2 = t1
                            ik = 0
                            while (ik < idl1) {
                                ch2[ik] += c2[t2++]
                                ik++
                            }
                            j++
                        }

                        if (ido < l1) {
                            state = 132
                            continue@loop
                        }

                        t1 = 0
                        t2 = 0
                        k = 0
                        while (k < l1) {
                            t3 = t1
                            t4 = t2
                            i = 0
                            while (i < ido) {
                                cc[t4++] = ch[t3++]
                                i++
                            }
                            t1 += ido
                            t2 += t10
                            k++
                        }
                        state = 135
                    }
                    119 -> {
                        ik = 0
                        while (ik < idl1) {
                            c2[ik] = ch2[ik]
                            ik++
                        }
                        t1 = 0
                        t2 = ipp2 * idl1
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1 - ido
                            t4 = t2 - ido
                            k = 0
                            while (k < l1) {
                                t3 += ido
                                t4 += ido
                                c1[t3] = ch[t3] + ch[t4]
                                c1[t4] = ch[t4] - ch[t3]
                                k++
                            }
                            j++
                        }
                        ar1 = 1f
                        ai1 = 0f
                        t1 = 0
                        t2 = ipp2 * idl1
                        t3 = (ip - 1) * idl1
                        l = 1
                        while (l < ipph) {
                            t1 += idl1
                            t2 -= idl1
                            ar1h = dcp * ar1 - dsp * ai1
                            ai1 = dcp * ai1 + dsp * ar1
                            ar1 = ar1h
                            t4 = t1
                            t5 = t2
                            t6 = t3
                            t7 = idl1
                            ik = 0
                            while (ik < idl1) {
                                ch2[t4++] = c2[ik] + ar1 * c2[t7++]
                                ch2[t5++] = ai1 * c2[t6++]
                                ik++
                            }
                            dc2 = ar1
                            ds2 = ai1
                            ar2 = ar1
                            ai2 = ai1
                            t4 = idl1
                            t5 = (ipp2 - 1) * idl1
                            j = 2
                            while (j < ipph) {
                                t4 += idl1
                                t5 -= idl1
                                ar2h = dc2 * ar2 - ds2 * ai2
                                ai2 = dc2 * ai2 + ds2 * ar2
                                ar2 = ar2h
                                t6 = t1
                                t7 = t2
                                t8 = t4
                                t9 = t5
                                ik = 0
                                while (ik < idl1) {
                                    ch2[t6++] += ar2 * c2[t8++]
                                    ch2[t7++] += ai2 * c2[t9++]
                                    ik++
                                }
                                j++
                            }
                            l++
                        }
                        t1 = 0
                        j = 1
                        while (j < ipph) {
                            t1 += idl1
                            t2 = t1
                            ik = 0
                            while (ik < idl1) {
                                ch2[ik] += c2[t2++]
                                ik++
                            }
                            j++
                        }
                        if (ido < l1) {
                            state = 132
                            continue@loop
                        }
                        t1 = 0
                        t2 = 0
                        k = 0
                        while (k < l1) {
                            t3 = t1
                            t4 = t2
                            i = 0
                            while (i < ido) {
                                cc[t4++] = ch[t3++]
                                i++
                            }
                            t1 += ido
                            t2 += t10
                            k++
                        }
                        state = 135
                    }

                    132 -> {
                        i = 0
                        while (i < ido) {
                            t1 = i
                            t2 = i
                            k = 0
                            while (k < l1) {
                                cc[t2] = ch[t1]
                                t1 += ido
                                t2 += t10
                                k++
                            }
                            i++
                        }
                        t1 = 0
                        t2 = ido shl 1
                        t3 = 0
                        t4 = ipp2 * t0
                        j = 1
                        while (j < ipph) {
                            t1 += t2
                            t3 += t0
                            t4 -= t0

                            t5 = t1
                            t6 = t3
                            t7 = t4

                            k = 0
                            while (k < l1) {
                                cc[t5 - 1] = ch[t6]
                                cc[t5] = ch[t7]
                                t5 += t10
                                t6 += ido
                                t7 += ido
                                k++
                            }
                            j++
                        }

                        if (ido == 1)
                            return
                        if (nbd < l1) {
                            state = 141
                            continue@loop
                        }

                        t1 = -ido
                        t3 = 0
                        t4 = 0
                        t5 = ipp2 * t0
                        j = 1
                        while (j < ipph) {
                            t1 += t2
                            t3 += t2
                            t4 += t0
                            t5 -= t0
                            t6 = t1
                            t7 = t3
                            t8 = t4
                            t9 = t5
                            k = 0
                            while (k < l1) {
                                i = 2
                                while (i < ido) {
                                    ic = idp2 - i
                                    cc[i + t7 - 1] = ch[i + t8 - 1] + ch[i + t9 - 1]
                                    cc[ic + t6 - 1] = ch[i + t8 - 1] - ch[i + t9 - 1]
                                    cc[i + t7] = ch[i + t8] + ch[i + t9]
                                    cc[ic + t6] = ch[i + t9] - ch[i + t8]
                                    i += 2
                                }
                                t6 += t10
                                t7 += t10
                                t8 += ido
                                t9 += ido
                                k++
                            }
                            j++
                        }
                        return
                    }
                    135 -> {
                        t1 = 0
                        t2 = ido shl 1
                        t3 = 0
                        t4 = ipp2 * t0
                        j = 1
                        while (j < ipph) {
                            t1 += t2
                            t3 += t0
                            t4 -= t0
                            t5 = t1
                            t6 = t3
                            t7 = t4
                            k = 0
                            while (k < l1) {
                                cc[t5 - 1] = ch[t6]
                                cc[t5] = ch[t7]
                                t5 += t10
                                t6 += ido
                                t7 += ido
                                k++
                            }
                            j++
                        }
                        if (ido == 1)
                            return
                        if (nbd < l1) {
                            state = 141
                            continue@loop
                        }
                        t1 = -ido
                        t3 = 0
                        t4 = 0
                        t5 = ipp2 * t0
                        j = 1
                        while (j < ipph) {
                            t1 += t2
                            t3 += t2
                            t4 += t0
                            t5 -= t0
                            t6 = t1
                            t7 = t3
                            t8 = t4
                            t9 = t5
                            k = 0
                            while (k < l1) {
                                i = 2
                                while (i < ido) {
                                    ic = idp2 - i
                                    cc[i + t7 - 1] = ch[i + t8 - 1] + ch[i + t9 - 1]
                                    cc[ic + t6 - 1] = ch[i + t8 - 1] - ch[i + t9 - 1]
                                    cc[i + t7] = ch[i + t8] + ch[i + t9]
                                    cc[ic + t6] = ch[i + t9] - ch[i + t8]
                                    i += 2
                                }
                                t6 += t10
                                t7 += t10
                                t8 += ido
                                t9 += ido
                                k++
                            }
                            j++
                        }
                        return
                    }
                    141 -> {
                        t1 = -ido
                        t3 = 0
                        t4 = 0
                        t5 = ipp2 * t0
                        j = 1
                        while (j < ipph) {
                            t1 += t2
                            t3 += t2
                            t4 += t0
                            t5 -= t0
                            i = 2
                            while (i < ido) {
                                t6 = idp2 + t1 - i
                                t7 = i + t3
                                t8 = i + t4
                                t9 = i + t5
                                k = 0
                                while (k < l1) {
                                    cc[t7 - 1] = ch[t8 - 1] + ch[t9 - 1]
                                    cc[t6 - 1] = ch[t8 - 1] - ch[t9 - 1]
                                    cc[t7] = ch[t8] + ch[t9]
                                    cc[t6] = ch[t9] - ch[t8]
                                    t6 += t10
                                    t7 += t10
                                    t8 += ido
                                    t9 += ido
                                    k++
                                }
                                i += 2
                            }
                            j++
                        }
                        continue@loop
                    }
                }
            }
        }

        fun drftf1(n: Int, c: FloatArray, ch: FloatArray, wa: FloatArray, ifac: IntArray) {
            var i: Int
            var k1: Int
            var l1: Int
            var l2: Int
            var na: Int
            var kh: Int
            val nf: Int
            var ip: Int
            var iw: Int
            var ido: Int
            var idl1: Int
            var ix2: Int
            var ix3: Int

            nf = ifac[1]
            na = 1
            l2 = n
            iw = n

            k1 = 0
            while (k1 < nf) {
                kh = nf - k1
                ip = ifac[kh + 1]
                l1 = l2 / ip
                ido = n / l2
                idl1 = ido * l1
                iw -= (ip - 1) * ido
                na = 1 - na

                var state = 100
                loop@ while (true) {
                    when (state) {
                        100 -> {
                            if (ip != 4) {
                                state = 102
                                continue@loop
                            }

                            ix2 = iw + ido
                            ix3 = ix2 + ido
                            if (na != 0) {
                                dradf4(ido, l1, ch, c, wa, iw - 1, wa, ix2 - 1, wa, ix3 - 1)
                            } else {
                                dradf4(ido, l1, c, ch, wa, iw - 1, wa, ix2 - 1, wa, ix3 - 1)
                            }
                            state = 110
                        }
                        102 -> {
                            if (ip != 2) {
                                state = 104
                                continue@loop
                            }
                            if (na != 0) {
                                state = 103
                                continue@loop
                            }
                            dradf2(ido, l1, c, ch, wa, iw - 1)
                            state = 110
                        }
                        103 -> {
                            dradf2(ido, l1, ch, c, wa, iw - 1)
                            if (ido == 1)
                                na = 1 - na
                            if (na != 0) {
                                state = 109
                                continue@loop
                            }
                            dradfg(ido, ip, l1, idl1, c, c, c, ch, ch, wa, iw - 1)
                            na = 1
                            state = 110
                        }
                        104 -> {
                            if (ido == 1)
                                na = 1 - na
                            if (na != 0) {
                                state = 109
                                continue@loop
                            }
                            dradfg(ido, ip, l1, idl1, c, c, c, ch, ch, wa, iw - 1)
                            na = 1
                            state = 110
                        }
                        109 -> {
                            dradfg(ido, ip, l1, idl1, ch, ch, ch, c, c, wa, iw - 1)
                            na = 0
                            l2 = l1
                            continue@loop
                        }
                        110 -> {
                            l2 = l1
                            continue@loop
                        }
                    }
                }
                k1++
            }
            if (na == 1)
                return
            i = 0
            while (i < n) {
                c[i] = ch[i]
                i++
            }
        }

        fun dradb2(ido: Int, l1: Int, cc: FloatArray, ch: FloatArray, wa1: FloatArray,
                   index: Int) {
            var i: Int
            var k: Int
            val t0: Int
            var t1: Int
            var t2: Int
            var t3: Int
            var t4: Int
            var t5: Int
            var t6: Int
            var ti2: Float
            var tr2: Float

            t0 = l1 * ido

            t1 = 0
            t2 = 0
            t3 = (ido shl 1) - 1
            k = 0
            while (k < l1) {
                ch[t1] = cc[t2] + cc[t3 + t2]
                ch[t1 + t0] = cc[t2] - cc[t3 + t2]
                t1 += ido
                t2 = t1 shl 1
                k++
            }

            if (ido < 2)
                return
            if (ido != 2) {
                t1 = 0
                t2 = 0
                k = 0
                while (k < l1) {
                    t3 = t1
                    t4 = t2
                    t5 = t4 + (ido shl 1)
                    t6 = t0 + t1
                    i = 2
                    while (i < ido) {
                        t3 += 2
                        t4 += 2
                        t5 -= 2
                        t6 += 2
                        ch[t3 - 1] = cc[t4 - 1] + cc[t5 - 1]
                        tr2 = cc[t4 - 1] - cc[t5 - 1]
                        ch[t3] = cc[t4] - cc[t5]
                        ti2 = cc[t4] + cc[t5]
                        ch[t6 - 1] = wa1[index + i - 2] * tr2 - wa1[index + i - 1] * ti2
                        ch[t6] = wa1[index + i - 2] * ti2 + wa1[index + i - 1] * tr2
                        i += 2
                    }
                    t1 += ido
                    t2 = t1 shl 1
                    k++
                }
                if (ido % 2 == 1)
                    return
            }

            t1 = ido - 1
            t2 = ido - 1
            k = 0
            while (k < l1) {
                ch[t1] = cc[t2] + cc[t2]
                ch[t1 + t0] = -(cc[t2 + 1] + cc[t2 + 1])
                t1 += ido
                t2 += ido shl 1
                k++
            }
        }

        fun dradb3(ido: Int, l1: Int, cc: FloatArray, ch: FloatArray, wa1: FloatArray,
                   index1: Int, wa2: FloatArray, index2: Int) {
            var i: Int
            var k: Int
            val t0: Int
            var t1: Int
            val t2: Int
            var t3: Int
            val t4: Int
            var t5: Int
            var t6: Int
            var t7: Int
            var t8: Int
            var t9: Int
            var t10: Int
            var ci2: Float
            var ci3: Float
            var di2: Float
            var di3: Float
            var cr2: Float
            var cr3: Float
            var dr2: Float
            var dr3: Float
            var ti2: Float
            var tr2: Float
            t0 = l1 * ido

            t1 = 0
            t2 = t0 shl 1
            t3 = ido shl 1
            t4 = ido + (ido shl 1)
            t5 = 0
            k = 0
            while (k < l1) {
                tr2 = cc[t3 - 1] + cc[t3 - 1]
                cr2 = cc[t5] + taur * tr2
                ch[t1] = cc[t5] + tr2
                ci3 = taui * (cc[t3] + cc[t3])
                ch[t1 + t0] = cr2 - ci3
                ch[t1 + t2] = cr2 + ci3
                t1 += ido
                t3 += t4
                t5 += t4
                k++
            }

            if (ido == 1)
                return

            t1 = 0
            t3 = ido shl 1
            k = 0
            while (k < l1) {
                t7 = t1 + (t1 shl 1)
                t5 = t7 + t3
                t6 = t5
                t8 = t1
                t9 = t1 + t0
                t10 = t9 + t0

                i = 2
                while (i < ido) {
                    t5 += 2
                    t6 -= 2
                    t7 += 2
                    t8 += 2
                    t9 += 2
                    t10 += 2
                    tr2 = cc[t5 - 1] + cc[t6 - 1]
                    cr2 = cc[t7 - 1] + taur * tr2
                    ch[t8 - 1] = cc[t7 - 1] + tr2
                    ti2 = cc[t5] - cc[t6]
                    ci2 = cc[t7] + taur * ti2
                    ch[t8] = cc[t7] + ti2
                    cr3 = taui * (cc[t5 - 1] - cc[t6 - 1])
                    ci3 = taui * (cc[t5] + cc[t6])
                    dr2 = cr2 - ci3
                    dr3 = cr2 + ci3
                    di2 = ci2 + cr3
                    di3 = ci2 - cr3
                    ch[t9 - 1] = wa1[index1 + i - 2] * dr2 - wa1[index1 + i - 1] * di2
                    ch[t9] = wa1[index1 + i - 2] * di2 + wa1[index1 + i - 1] * dr2
                    ch[t10 - 1] = wa2[index2 + i - 2] * dr3 - wa2[index2 + i - 1] * di3
                    ch[t10] = wa2[index2 + i - 2] * di3 + wa2[index2 + i - 1] * dr3
                    i += 2
                }
                t1 += ido
                k++
            }
        }

        fun dradb4(ido: Int, l1: Int, cc: FloatArray, ch: FloatArray, wa1: FloatArray,
                   index1: Int, wa2: FloatArray, index2: Int, wa3: FloatArray, index3: Int) {
            var i: Int
            var k: Int
            val t0: Int
            var t1: Int
            var t2: Int
            var t3: Int
            var t4: Int
            var t5: Int
            val t6: Int
            var t7: Int
            var t8: Int
            var ci2: Float
            var ci3: Float
            var ci4: Float
            var cr2: Float
            var cr3: Float
            var cr4: Float
            var ti1: Float
            var ti2: Float
            var ti3: Float
            var ti4: Float
            var tr1: Float
            var tr2: Float
            var tr3: Float
            var tr4: Float
            t0 = l1 * ido

            t1 = 0
            t2 = ido shl 2
            t3 = 0
            t6 = ido shl 1
            k = 0
            while (k < l1) {
                t4 = t3 + t6
                t5 = t1
                tr3 = cc[t4 - 1] + cc[t4 - 1]
                tr4 = cc[t4] + cc[t4]
                t4 += t6
                tr1 = cc[t3] - cc[t4 - 1]
                tr2 = cc[t3] + cc[t4 - 1]
                ch[t5] = tr2 + tr3
                t5 += t0
                ch[t5] = tr1 - tr4
                t5 += t0
                ch[t5] = tr2 - tr3
                t5 += t0
                ch[t5] = tr1 + tr4
                t1 += ido
                t3 += t2
                k++
            }

            if (ido < 2)
                return
            if (ido != 2) {
                t1 = 0
                k = 0
                while (k < l1) {
                    t2 = t1 shl 2
                    t3 = t2 + t6
                    t4 = t3
                    t5 = t4 + t6
                    t7 = t1
                    i = 2
                    while (i < ido) {
                        t2 += 2
                        t3 += 2
                        t4 -= 2
                        t5 -= 2
                        t7 += 2
                        ti1 = cc[t2] + cc[t5]
                        ti2 = cc[t2] - cc[t5]
                        ti3 = cc[t3] - cc[t4]
                        tr4 = cc[t3] + cc[t4]
                        tr1 = cc[t2 - 1] - cc[t5 - 1]
                        tr2 = cc[t2 - 1] + cc[t5 - 1]
                        ti4 = cc[t3 - 1] - cc[t4 - 1]
                        tr3 = cc[t3 - 1] + cc[t4 - 1]
                        ch[t7 - 1] = tr2 + tr3
                        cr3 = tr2 - tr3
                        ch[t7] = ti2 + ti3
                        ci3 = ti2 - ti3
                        cr2 = tr1 - tr4
                        cr4 = tr1 + tr4
                        ci2 = ti1 + ti4
                        ci4 = ti1 - ti4

                        t8 = t7 + t0
                        ch[t8 - 1] = wa1[index1 + i - 2] * cr2 - wa1[index1 + i - 1] * ci2
                        ch[t8] = wa1[index1 + i - 2] * ci2 + wa1[index1 + i - 1] * cr2
                        t8 += t0
                        ch[t8 - 1] = wa2[index2 + i - 2] * cr3 - wa2[index2 + i - 1] * ci3
                        ch[t8] = wa2[index2 + i - 2] * ci3 + wa2[index2 + i - 1] * cr3
                        t8 += t0
                        ch[t8 - 1] = wa3[index3 + i - 2] * cr4 - wa3[index3 + i - 1] * ci4
                        ch[t8] = wa3[index3 + i - 2] * ci4 + wa3[index3 + i - 1] * cr4
                        i += 2
                    }
                    t1 += ido
                    k++
                }
                if (ido % 2 == 1)
                    return
            }

            t1 = ido
            t2 = ido shl 2
            t3 = ido - 1
            t4 = ido + (ido shl 1)
            k = 0
            while (k < l1) {
                t5 = t3
                ti1 = cc[t1] + cc[t4]
                ti2 = cc[t4] - cc[t1]
                tr1 = cc[t1 - 1] - cc[t4 - 1]
                tr2 = cc[t1 - 1] + cc[t4 - 1]
                ch[t5] = tr2 + tr2
                t5 += t0
                ch[t5] = sqrt2 * (tr1 - ti1)
                t5 += t0
                ch[t5] = ti2 + ti2
                t5 += t0
                ch[t5] = -sqrt2 * (tr1 + ti1)

                t3 += ido
                t1 += t2
                t4 += t2
                k++
            }
        }

        fun dradbg(ido: Int, ip: Int, l1: Int, idl1: Int, cc: FloatArray, c1: FloatArray,
                   c2: FloatArray, ch: FloatArray, ch2: FloatArray, wa: FloatArray, index: Int) {

            var idij: Int
            var ipph = 0
            var i: Int
            var j: Int
            var k: Int
            var l: Int
            var ik: Int
            var `is`: Int
            var t0 = 0
            var t1: Int
            var t2: Int
            var t3: Int
            var t4: Int
            var t5: Int
            var t6: Int
            var t7: Int
            var t8: Int
            var t9: Int
            var t10 = 0
            var t11: Int
            var t12: Int
            var dc2: Float
            var ai1: Float
            var ai2: Float
            var ar1: Float
            var ar2: Float
            var ds2: Float
            var nbd = 0
            var dcp = 0f
            var arg: Float
            var dsp = 0f
            var ar1h: Float
            var ar2h: Float
            var ipp2 = 0

            var state = 100

            loop@ while (true) {
                when (state) {
                    100 -> {
                        t10 = ip * ido
                        t0 = l1 * ido
                        arg = tpi / ip.toFloat()
                        dcp = cos(arg.toDouble()).toFloat()
                        dsp = sin(arg.toDouble()).toFloat()
                        nbd = (ido - 1).ushr(1)
                        ipp2 = ip
                        ipph = (ip + 1).ushr(1)
                        if (ido < l1) {
                            state = 103
                            continue@loop
                        }
                        t1 = 0
                        t2 = 0
                        k = 0
                        while (k < l1) {
                            t3 = t1
                            t4 = t2
                            i = 0
                            while (i < ido) {
                                ch[t3] = cc[t4]
                                t3++
                                t4++
                                i++
                            }
                            t1 += ido
                            t2 += t10
                            k++
                        }
                        state = 106
                    }
                    103 -> {
                        t1 = 0
                        i = 0
                        while (i < ido) {
                            t2 = t1
                            t3 = t1
                            k = 0
                            while (k < l1) {
                                ch[t2] = cc[t3]
                                t2 += ido
                                t3 += t10
                                k++
                            }
                            t1++
                            i++
                        }
                        t1 = 0
                        t2 = ipp2 * t0
                        t5 = ido shl 1
                        t7 = t5
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1
                            t4 = t2
                            t6 = t5
                            k = 0
                            while (k < l1) {
                                ch[t3] = cc[t6 - 1] + cc[t6 - 1]
                                ch[t4] = cc[t6] + cc[t6]
                                t3 += ido
                                t4 += ido
                                t6 += t10
                                k++
                            }
                            t5 += t7
                            j++
                        }
                        if (ido == 1) {
                            state = 116
                            continue@loop
                        }
                        if (nbd < l1) {
                            state = 112
                            continue@loop
                        }

                        t1 = 0
                        t2 = ipp2 * t0
                        t7 = 0
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1
                            t4 = t2

                            t7 += ido shl 1
                            t8 = t7
                            k = 0
                            while (k < l1) {
                                t5 = t3
                                t6 = t4
                                t9 = t8
                                t11 = t8
                                i = 2
                                while (i < ido) {
                                    t5 += 2
                                    t6 += 2
                                    t9 += 2
                                    t11 -= 2
                                    ch[t5 - 1] = cc[t9 - 1] + cc[t11 - 1]
                                    ch[t6 - 1] = cc[t9 - 1] - cc[t11 - 1]
                                    ch[t5] = cc[t9] - cc[t11]
                                    ch[t6] = cc[t9] + cc[t11]
                                    i += 2
                                }
                                t3 += ido
                                t4 += ido
                                t8 += t10
                                k++
                            }
                            j++
                        }
                        state = 116
                    }
                    106 -> {
                        t1 = 0
                        t2 = ipp2 * t0
                        t5 = ido shl 1
                        t7 = t5
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1
                            t4 = t2
                            t6 = t5
                            k = 0
                            while (k < l1) {
                                ch[t3] = cc[t6 - 1] + cc[t6 - 1]
                                ch[t4] = cc[t6] + cc[t6]
                                t3 += ido
                                t4 += ido
                                t6 += t10
                                k++
                            }
                            t5 += t7
                            j++
                        }
                        if (ido == 1) {
                            state = 116
                            continue@loop
                        }
                        if (nbd < l1) {
                            state = 112
                            continue@loop
                        }
                        t1 = 0
                        t2 = ipp2 * t0
                        t7 = 0
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1
                            t4 = t2
                            t7 += ido shl 1
                            t8 = t7
                            k = 0
                            while (k < l1) {
                                t5 = t3
                                t6 = t4
                                t9 = t8
                                t11 = t8
                                i = 2
                                while (i < ido) {
                                    t5 += 2
                                    t6 += 2
                                    t9 += 2
                                    t11 -= 2
                                    ch[t5 - 1] = cc[t9 - 1] + cc[t11 - 1]
                                    ch[t6 - 1] = cc[t9 - 1] - cc[t11 - 1]
                                    ch[t5] = cc[t9] - cc[t11]
                                    ch[t6] = cc[t9] + cc[t11]
                                    i += 2
                                }
                                t3 += ido
                                t4 += ido
                                t8 += t10
                                k++
                            }
                            j++
                        }
                        state = 116
                    }
                    112 -> {
                        t1 = 0
                        t2 = ipp2 * t0
                        t7 = 0
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1
                            t4 = t2
                            t7 += ido shl 1
                            t8 = t7
                            t9 = t7
                            i = 2
                            while (i < ido) {
                                t3 += 2
                                t4 += 2
                                t8 += 2
                                t9 -= 2
                                t5 = t3
                                t6 = t4
                                t11 = t8
                                t12 = t9
                                k = 0
                                while (k < l1) {
                                    ch[t5 - 1] = cc[t11 - 1] + cc[t12 - 1]
                                    ch[t6 - 1] = cc[t11 - 1] - cc[t12 - 1]
                                    ch[t5] = cc[t11] - cc[t12]
                                    ch[t6] = cc[t11] + cc[t12]
                                    t5 += ido
                                    t6 += ido
                                    t11 += t10
                                    t12 += t10
                                    k++
                                }
                                i += 2
                            }
                            j++
                        }
                        ar1 = 1f
                        ai1 = 0f
                        t1 = 0
                        t2 = ipp2 * idl1
                        t9 = t2
                        t3 = (ip - 1) * idl1
                        l = 1
                        while (l < ipph) {
                            t1 += idl1
                            t2 -= idl1

                            ar1h = dcp * ar1 - dsp * ai1
                            ai1 = dcp * ai1 + dsp * ar1
                            ar1 = ar1h
                            t4 = t1
                            t5 = t2
                            t6 = 0
                            t7 = idl1
                            t8 = t3
                            ik = 0
                            while (ik < idl1) {
                                c2[t4++] = ch2[t6++] + ar1 * ch2[t7++]
                                c2[t5++] = ai1 * ch2[t8++]
                                ik++
                            }
                            dc2 = ar1
                            ds2 = ai1
                            ar2 = ar1
                            ai2 = ai1

                            t6 = idl1
                            t7 = t9 - idl1
                            j = 2
                            while (j < ipph) {
                                t6 += idl1
                                t7 -= idl1
                                ar2h = dc2 * ar2 - ds2 * ai2
                                ai2 = dc2 * ai2 + ds2 * ar2
                                ar2 = ar2h
                                t4 = t1
                                t5 = t2
                                t11 = t6
                                t12 = t7
                                ik = 0
                                while (ik < idl1) {
                                    c2[t4++] += ar2 * ch2[t11++]
                                    c2[t5++] += ai2 * ch2[t12++]
                                    ik++
                                }
                                j++
                            }
                            l++
                        }

                        t1 = 0
                        j = 1
                        while (j < ipph) {
                            t1 += idl1
                            t2 = t1
                            ik = 0
                            while (ik < idl1) {
                                ch2[ik] += ch2[t2++]
                                ik++
                            }
                            j++
                        }

                        t1 = 0
                        t2 = ipp2 * t0
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1
                            t4 = t2
                            k = 0
                            while (k < l1) {
                                ch[t3] = c1[t3] - c1[t4]
                                ch[t4] = c1[t3] + c1[t4]
                                t3 += ido
                                t4 += ido
                                k++
                            }
                            j++
                        }

                        if (ido == 1) {
                            state = 132
                            continue@loop
                        }
                        if (nbd < l1) {
                            state = 128
                            continue@loop
                        }

                        t1 = 0
                        t2 = ipp2 * t0
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1
                            t4 = t2
                            k = 0
                            while (k < l1) {
                                t5 = t3
                                t6 = t4
                                i = 2
                                while (i < ido) {
                                    t5 += 2
                                    t6 += 2
                                    ch[t5 - 1] = c1[t5 - 1] - c1[t6]
                                    ch[t6 - 1] = c1[t5 - 1] + c1[t6]
                                    ch[t5] = c1[t5] + c1[t6 - 1]
                                    ch[t6] = c1[t5] - c1[t6 - 1]
                                    i += 2
                                }
                                t3 += ido
                                t4 += ido
                                k++
                            }
                            j++
                        }
                        state = 132
                    }
                    116 -> {
                        ar1 = 1f
                        ai1 = 0f
                        t1 = 0
                        t2 = ipp2 * idl1
                        t9 = t2
                        t3 = (ip - 1) * idl1
                        l = 1
                        while (l < ipph) {
                            t1 += idl1
                            t2 -= idl1
                            ar1h = dcp * ar1 - dsp * ai1
                            ai1 = dcp * ai1 + dsp * ar1
                            ar1 = ar1h
                            t4 = t1
                            t5 = t2
                            t6 = 0
                            t7 = idl1
                            t8 = t3
                            ik = 0
                            while (ik < idl1) {
                                c2[t4++] = ch2[t6++] + ar1 * ch2[t7++]
                                c2[t5++] = ai1 * ch2[t8++]
                                ik++
                            }
                            dc2 = ar1
                            ds2 = ai1
                            ar2 = ar1
                            ai2 = ai1
                            t6 = idl1
                            t7 = t9 - idl1
                            j = 2
                            while (j < ipph) {
                                t6 += idl1
                                t7 -= idl1
                                ar2h = dc2 * ar2 - ds2 * ai2
                                ai2 = dc2 * ai2 + ds2 * ar2
                                ar2 = ar2h
                                t4 = t1
                                t5 = t2
                                t11 = t6
                                t12 = t7
                                ik = 0
                                while (ik < idl1) {
                                    c2[t4++] += ar2 * ch2[t11++]
                                    c2[t5++] += ai2 * ch2[t12++]
                                    ik++
                                }
                                j++
                            }
                            l++
                        }
                        t1 = 0
                        j = 1
                        while (j < ipph) {
                            t1 += idl1
                            t2 = t1
                            ik = 0
                            while (ik < idl1) {
                                ch2[ik] += ch2[t2++]
                                ik++
                            }
                            j++
                        }
                        t1 = 0
                        t2 = ipp2 * t0
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1
                            t4 = t2
                            k = 0
                            while (k < l1) {
                                ch[t3] = c1[t3] - c1[t4]
                                ch[t4] = c1[t3] + c1[t4]
                                t3 += ido
                                t4 += ido
                                k++
                            }
                            j++
                        }
                        if (ido == 1) {
                            state = 132
                            continue@loop
                        }
                        if (nbd < l1) {
                            state = 128
                            continue@loop
                        }
                        t1 = 0
                        t2 = ipp2 * t0
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1
                            t4 = t2
                            k = 0
                            while (k < l1) {
                                t5 = t3
                                t6 = t4
                                i = 2
                                while (i < ido) {
                                    t5 += 2
                                    t6 += 2
                                    ch[t5 - 1] = c1[t5 - 1] - c1[t6]
                                    ch[t6 - 1] = c1[t5 - 1] + c1[t6]
                                    ch[t5] = c1[t5] + c1[t6 - 1]
                                    ch[t6] = c1[t5] - c1[t6 - 1]
                                    i += 2
                                }
                                t3 += ido
                                t4 += ido
                                k++
                            }
                            j++
                        }
                        state = 132
                    }
                    128 -> {
                        t1 = 0
                        t2 = ipp2 * t0
                        j = 1
                        while (j < ipph) {
                            t1 += t0
                            t2 -= t0
                            t3 = t1
                            t4 = t2
                            i = 2
                            while (i < ido) {
                                t3 += 2
                                t4 += 2
                                t5 = t3
                                t6 = t4
                                k = 0
                                while (k < l1) {
                                    ch[t5 - 1] = c1[t5 - 1] - c1[t6]
                                    ch[t6 - 1] = c1[t5 - 1] + c1[t6]
                                    ch[t5] = c1[t5] + c1[t6 - 1]
                                    ch[t6] = c1[t5] - c1[t6 - 1]
                                    t5 += ido
                                    t6 += ido
                                    k++
                                }
                                i += 2
                            }
                            j++
                        }
                        if (ido == 1)
                            return

                        ik = 0
                        while (ik < idl1) {
                            c2[ik] = ch2[ik]
                            ik++
                        }

                        t1 = 0
                        j = 1
                        while (j < ip) {
                            t1 += t0
                            t2 = t1
                            k = 0
                            while (k < l1) {
                                c1[t2] = ch[t2]
                                t2 += ido
                                k++
                            }
                            j++
                        }

                        if (nbd > l1) {
                            state = 139
                            continue@loop
                        }

                        `is` = -ido - 1
                        t1 = 0
                        j = 1
                        while (j < ip) {
                            `is` += ido
                            t1 += t0
                            idij = `is`
                            t2 = t1
                            i = 2
                            while (i < ido) {
                                t2 += 2
                                idij += 2
                                t3 = t2
                                k = 0
                                while (k < l1) {
                                    c1[t3 - 1] = wa[index + idij - 1] * ch[t3 - 1] - wa[index + idij] * ch[t3]
                                    c1[t3] = wa[index + idij - 1] * ch[t3] + wa[index + idij] * ch[t3 - 1]
                                    t3 += ido
                                    k++
                                }
                                i += 2
                            }
                            j++
                        }
                        return
                    }
                    132 -> {
                        if (ido == 1)
                            return
                        ik = 0
                        while (ik < idl1) {
                            c2[ik] = ch2[ik]
                            ik++
                        }
                        t1 = 0
                        j = 1
                        while (j < ip) {
                            t1 += t0
                            t2 = t1
                            k = 0
                            while (k < l1) {
                                c1[t2] = ch[t2]
                                t2 += ido
                                k++
                            }
                            j++
                        }
                        if (nbd > l1) {
                            state = 139
                            continue@loop
                        }
                        `is` = -ido - 1
                        t1 = 0
                        j = 1
                        while (j < ip) {
                            `is` += ido
                            t1 += t0
                            idij = `is`
                            t2 = t1
                            i = 2
                            while (i < ido) {
                                t2 += 2
                                idij += 2
                                t3 = t2
                                k = 0
                                while (k < l1) {
                                    c1[t3 - 1] = wa[index + idij - 1] * ch[t3 - 1] - wa[index + idij] * ch[t3]
                                    c1[t3] = wa[index + idij - 1] * ch[t3] + wa[index + idij] * ch[t3 - 1]
                                    t3 += ido
                                    k++
                                }
                                i += 2
                            }
                            j++
                        }
                        return
                    }

                    139 -> {
                        `is` = -ido - 1
                        t1 = 0
                        j = 1
                        while (j < ip) {
                            `is` += ido
                            t1 += t0
                            t2 = t1
                            k = 0
                            while (k < l1) {
                                idij = `is`
                                t3 = t2
                                i = 2
                                while (i < ido) {
                                    idij += 2
                                    t3 += 2
                                    c1[t3 - 1] = wa[index + idij - 1] * ch[t3 - 1] - wa[index + idij] * ch[t3]
                                    c1[t3] = wa[index + idij - 1] * ch[t3] + wa[index + idij] * ch[t3 - 1]
                                    i += 2
                                }
                                t2 += ido
                                k++
                            }
                            j++
                        }
                        continue@loop
                    }
                }
            }
        }

        fun drftb1(n: Int, c: FloatArray, ch: FloatArray, wa: FloatArray, index: Int, ifac: IntArray) {
            var i: Int
            var k1: Int
            var l1: Int
            var l2 = 0
            var na: Int
            val nf: Int
            var ip = 0
            var iw: Int
            var ix2: Int
            var ix3: Int
            var ido = 0
            var idl1 = 0

            nf = ifac[1]
            na = 0
            l1 = 1
            iw = 1

            k1 = 0
            while (k1 < nf) {
                var state = 100
                loop@ while (true) {
                    when (state) {
                        100 -> {
                            ip = ifac[k1 + 2]
                            l2 = ip * l1
                            ido = n / l2
                            idl1 = ido * l1
                            if (ip != 4) {
                                state = 103
                                continue@loop
                            }
                            ix2 = iw + ido
                            ix3 = ix2 + ido

                            if (na != 0)
                                dradb4(ido, l1, ch, c, wa, index + iw - 1, wa, index + ix2 - 1, wa, index + ix3 - 1)
                            else
                                dradb4(ido, l1, c, ch, wa, index + iw - 1, wa, index + ix2 - 1, wa, index + ix3 - 1)
                            na = 1 - na
                            state = 115
                        }
                        103 -> {
                            if (ip != 2) {
                                state = 106
                                continue@loop
                            }

                            if (na != 0)
                                dradb2(ido, l1, ch, c, wa, index + iw - 1)
                            else
                                dradb2(ido, l1, c, ch, wa, index + iw - 1)
                            na = 1 - na
                            state = 115
                        }

                        106 -> {
                            if (ip != 3) {
                                state = 109
                                continue@loop
                            }

                            ix2 = iw + ido
                            if (na != 0)
                                dradb3(ido, l1, ch, c, wa, index + iw - 1, wa, index + ix2 - 1)
                            else
                                dradb3(ido, l1, c, ch, wa, index + iw - 1, wa, index + ix2 - 1)
                            na = 1 - na
                            state = 115
                        }
                        109 -> {
                            if (na != 0)
                                dradbg(ido, ip, l1, idl1, ch, ch, ch, c, c, wa, index + iw - 1)
                            else
                                dradbg(ido, ip, l1, idl1, c, c, c, ch, ch, wa, index + iw - 1)
                            if (ido == 1)
                                na = 1 - na
                            l1 = l2
                            iw += (ip - 1) * ido
                            continue@loop
                        }

                        115 -> {
                            l1 = l2
                            iw += (ip - 1) * ido
                            continue@loop
                        }
                    }
                }
                k1++
            }
            if (na == 0)
                return
            i = 0
            while (i < n) {
                c[i] = ch[i]
                i++
            }
        }
    }
}
