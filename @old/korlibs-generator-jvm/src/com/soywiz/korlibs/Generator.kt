package com.soywiz.korlibs

import GenGl

object Generator {
	@JvmStatic
	fun main(args: Array<String>) {
		GenGl.main(args)
		KmemGenerator.main(args)
	}
}
