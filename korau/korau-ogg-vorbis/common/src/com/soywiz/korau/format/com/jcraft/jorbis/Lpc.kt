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

import kotlin.math.*

class Lpc {
	// en/decode lookups
	var fft = Drft()

	var ln: Int = 0
	var m: Int = 0

	// Input : n element envelope spectral curve
	// Output: m lpc coefficients, excitation energy

	fun lpc_from_curve(curve: FloatArray, lpc: FloatArray): Float {
		var n = ln
		val work = FloatArray(n + n)
		val fscale = (.5 / n).toFloat()
		var i: Int
		var j: Int

		// input is a real curve. make it complex-real
		// This mixes phase, but the LPC generation doesn't care.
		i = 0
		while (i < n) {
			work[i * 2] = curve[i] * fscale
			work[i * 2 + 1] = 0f
			i++
		}
		work[n * 2 - 1] = curve[n - 1] * fscale

		n *= 2
		fft.backward(work)

		// The autocorrelation will not be circular.  Shift, else we lose
		// most of the power in the edges.

		i = 0
		j = n / 2
		while (i < n / 2) {
			val temp = work[i]
			work[i++] = work[j]
			work[j++] = temp
		}

		return lpc_from_data(work, lpc, n, m)
	}

	fun init(mapped: Int, m: Int) {
		ln = mapped
		this.m = m

		// we cheat decoding the LPC spectrum via FFTs
		fft.init(mapped * 2)
	}

	fun clear() {
		fft.clear()
	}

	// One can do this the long way by generating the transfer function in
	// the time domain and taking the forward FFT of the result.  The
	// results from direct calculation are cleaner and faster.
	//
	// This version does a linear curve generation and then later
	// interpolates the log curve from the linear curve.

	fun lpc_to_curve(curve: FloatArray, lpc: FloatArray, amp: Float) {

		for (i in 0 until ln * 2) {
			curve[i] = 0.0f
		}

		if (amp == 0f) {
			return
		}

		for (i in 0 until m) {
			curve[i * 2 + 1] = lpc[i] / (4 * amp)
			curve[i * 2 + 2] = -lpc[i] / (4 * amp)
		}

		fft.backward(curve)

		run {
			val l2 = ln * 2
			val unit = (1.0 / amp).toFloat()
			curve[0] = (1.0 / (curve[0] * 2 + unit)).toFloat()
			for (i in 1 until ln) {
				val real = curve[i] + curve[l2 - i]
				val imag = curve[i] - curve[l2 - i]

				val a = real + unit
				curve[i] = (1.0 / FAST_HYPOT(a, imag)).toFloat()
			}
		}
	}

	companion object {

		// Autocorrelation LPC coeff generation algorithm invented by
		// N. Levinson in 1947, modified by J. Durbin in 1959.

		// Input : n elements of time doamin data
		// Output: m lpc coefficients, excitation energy

		fun lpc_from_data(data: FloatArray, lpc: FloatArray, n: Int, m: Int): Float {
			val aut = FloatArray(m + 1)
			var error: Float
			var i: Int
			var j: Int

			// autocorrelation, p+1 lag coefficients

			j = m + 1
			while (j-- != 0) {
				var d = 0f
				i = j
				while (i < n) {
					d += data[i] * data[i - j]
					i++
				}
				aut[j] = d
			}

			// Generate lpc coefficients from autocorr values

			error = aut[0]
			/*
    if(error==0){
      for(int k=0; k<m; k++) lpc[k]=0.0f;
      return 0;
    }
    */

			i = 0
			while (i < m) {
				var r = -aut[i + 1]

				if (error == 0f) {
					for (k in 0 until m) {
						lpc[k] = 0.0f
					}
					return 0f
				}

				// Sum up this iteration's reflection coefficient; note that in
				// Vorbis we don't save it.  If anyone wants to recycle this code
				// and needs reflection coefficients, save the results of 'r' from
				// each iteration.

				j = 0
				while (j < i) {
					r -= lpc[j] * aut[i - j]
					j++
				}
				r /= error

				// Update LPC coefficients and total error

				lpc[i] = r
				j = 0
				while (j < i / 2) {
					val tmp = lpc[j]
					lpc[j] += r * lpc[i - 1 - j]
					lpc[i - 1 - j] += r * tmp
					j++
				}
				if (i % 2 != 0) {
					lpc[j] += lpc[j] * r
				}

				error *= (1.0 - r * r).toFloat()
				i++
			}

			// we need the error value to know how big an impulse to hit the
			// filter with later

			return error
		}

		fun FAST_HYPOT(a: Float, b: Float): Float {
			return sqrt((a * a + b * b).toDouble()).toFloat()
		}
	}
}
