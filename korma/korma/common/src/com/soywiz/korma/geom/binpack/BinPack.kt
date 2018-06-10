package com.soywiz.korma.geom.binpack

import com.soywiz.korma.geom.*

interface BinPack {
	val maxWidth: Double
	val maxHeight: Double
	fun add(width: Double, height: Double): Rectangle?
}