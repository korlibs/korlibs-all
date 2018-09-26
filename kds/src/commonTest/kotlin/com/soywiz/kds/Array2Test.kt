package com.soywiz.kds

import kotlin.test.*

class Array2Test {
	@Test
	fun name() {
		val map = Array2.fromString(
			Tiles.MAPPING, -1, """
			:    #####
			:    #   #
			:    #$  #
			:  ###  $##
			:  #  $ $ #
			:### # ## #   ######
			:#   # ## #####  ..#
			:# $  $         *..#
			:##### ### #@##  ..#
			:    #     #########
			:    #######
		"""
		)


		val output = map.toString(Tiles.REV_MAPPING, margin = ":")

		val expected = listOf(
			":     #####          ",
			":     #   #          ",
			":     #$  #          ",
			":   ###  $##         ",
			":   #  $ $ #         ",
			": ### # ## #   ######",
			": #   # ## #####  ..#",
			": # $  $         *..#",
			": ##### ### #@##  ..#",
			":     #     #########",
			":     #######        "
		).joinToString("\n")

		assertEquals(expected, output)
	}

	object Tiles {
		const val GROUND = 0
		const val WALL = 1
		const val CONTAINER = 2
		const val BOX = 3
		const val BOX_OVER = 4
		const val CHARACTER = 10

		val AVAILABLE = setOf(GROUND, CONTAINER)
		val BOXLIKE = setOf(BOX, BOX_OVER)

		val MAPPING = mapOf(
			' ' to GROUND,
			'#' to WALL,
			'.' to CONTAINER,
			'$' to BOX,
			'*' to BOX_OVER,
			'@' to CHARACTER
		)

		val REV_MAPPING = MAPPING.map { it.value to it.key }.toMap()
	}
}