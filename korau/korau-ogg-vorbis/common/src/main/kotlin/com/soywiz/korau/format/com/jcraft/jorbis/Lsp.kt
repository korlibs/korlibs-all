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

/*
  function: LSP (also called LSF) conversion routines

  The LSP generation code is taken (with minimal modification) from
  "On the Computation of the LSP Frequencies" by Joseph Rothweiler
  <rothwlr@altavista.net>, available at:

  http://www2.xtdl.com/~rothwlr/lsfpaper/lsfpage.html
 ********************************************************************/

internal object Lsp {

	val M_PI = 3.1415926539.toFloat()

	fun lsp_to_curve(
		curve: FloatArray,
		map: IntArray,
		n: Int,
		ln: Int,
		lsp: FloatArray,
		m: Int,
		amp: Float,
		ampoffset: Float
	) {
		var i: Int
		val wdel = M_PI / ln
		i = 0
		while (i < m) {
			lsp[i] = Lookup.coslook(lsp[i])
			i++
		}
		val m2 = m / 2 * 2

		i = 0
		while (i < n) {
			val k = map[i]
			var p = .7071067812f
			var q = .7071067812f
			val w = Lookup.coslook(wdel * k)

			var j = 0
			while (j < m2) {
				q *= lsp[j] - w
				p *= lsp[j + 1] - w
				j += 2
			}

			if (m and 1 != 0) {
				/* odd order filter; slightly assymetric */
				/* the last coefficient */
				q *= lsp[m - 1] - w
				q *= q
				p *= p * (1f - w * w)
			} else {
				/* even order filter; still symmetric */
				q *= q * (1f + w)
				p *= p * (1f - w)
			}

			//  q=frexp(p+q,&qexp);
			q = p + q
			var hx = q.toBits()
			var ix = 0x7fffffff and hx
			var qexp = 0

			if (ix >= 0x7f800000 || ix == 0) {
				// 0,inf,nan
			} else {
				if (ix < 0x00800000) { // subnormal
					q *= 3.3554432000e+07f // 0x4c000000
					hx = q.toBits()
					ix = 0x7fffffff and hx
					qexp = -25
				}
				qexp += ix.ushr(23) - 126
				hx = hx and 0x807fffff.toInt() or 0x3f000000
				q = Float.fromBits(hx)
			}

			q = Lookup.fromdBlook(amp * Lookup.invsqlook(q) * Lookup.invsq2explook(qexp + m) - ampoffset)

			do {
				curve[i++] *= q
			} while (i < n && map[i] == k)

		}
	}
}
