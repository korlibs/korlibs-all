/*
 *      LAME MP3 encoding engine
 *
 *      Copyright (c) 1999-2000 Mark Taylor
 *      Copyright (c) 2000-2005 Takehiro Tominaga
 *      Copyright (c) 2000-2005 Robert Hegemann
 *      Copyright (c) 2000-2005 Gabriel Bouvigne
 *      Copyright (c) 2000-2004 Alexander Leidinger
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

/* $Id: Lame.java,v 1.44 2012/03/23 10:02:29 kenchis Exp $ */

package com.soywiz.korau.format.net.sourceforge.lame.mp3

import com.soywiz.korau.format.net.sourceforge.lame.mpg.Interface
import com.soywiz.korau.format.net.sourceforge.lame.mpg.MPGLib

class Lame {
    val flags = LameGlobalFlags()
    val vbr = VBRTag()
    val parser = Parse()
    val intf = Interface(vbr)
    val mpg = MPGLib(intf)
    val audio = GetAudio(parser, mpg)
}
