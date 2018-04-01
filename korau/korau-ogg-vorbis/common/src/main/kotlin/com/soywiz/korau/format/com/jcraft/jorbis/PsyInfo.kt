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

// psychoacoustic setup
class PsyInfo {
    var athp: Int = 0
    var decayp: Int = 0
    var smoothp: Int = 0
    var noisefitp: Int = 0
    var noisefit_subblock: Int = 0
    var noisefit_threshdB: Float = 0.toFloat()

    var ath_att: Float = 0.toFloat()

    var tonemaskp: Int = 0
    var toneatt_125Hz = FloatArray(5)
    var toneatt_250Hz = FloatArray(5)
    var toneatt_500Hz = FloatArray(5)
    var toneatt_1000Hz = FloatArray(5)
    var toneatt_2000Hz = FloatArray(5)
    var toneatt_4000Hz = FloatArray(5)
    var toneatt_8000Hz = FloatArray(5)

    var peakattp: Int = 0
    var peakatt_125Hz = FloatArray(5)
    var peakatt_250Hz = FloatArray(5)
    var peakatt_500Hz = FloatArray(5)
    var peakatt_1000Hz = FloatArray(5)
    var peakatt_2000Hz = FloatArray(5)
    var peakatt_4000Hz = FloatArray(5)
    var peakatt_8000Hz = FloatArray(5)

    var noisemaskp: Int = 0
    var noiseatt_125Hz = FloatArray(5)
    var noiseatt_250Hz = FloatArray(5)
    var noiseatt_500Hz = FloatArray(5)
    var noiseatt_1000Hz = FloatArray(5)
    var noiseatt_2000Hz = FloatArray(5)
    var noiseatt_4000Hz = FloatArray(5)
    var noiseatt_8000Hz = FloatArray(5)

    var max_curve_dB: Float = 0.toFloat()

    var attack_coeff: Float = 0.toFloat()
    var decay_coeff: Float = 0.toFloat()

    fun free() {}
}
