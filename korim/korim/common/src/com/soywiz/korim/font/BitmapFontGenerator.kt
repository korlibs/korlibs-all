package com.soywiz.korim.font

import com.soywiz.klock.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.std.*

object BitmapFontGenerator {
	val SPACE = " "
	val UPPERCASE = ('A'..'Z').joinToString("")
	val LOWERCASE = ('a'..'z').joinToString("")
	val NUMBERS = ('0'..'9').joinToString("")
	val PUNCTUATION = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}"
	val LATIN_BASIC = "ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥PÉáíóúñÑª°¿¬½¼¡«»ßµø±÷°·.²"
	val LATIN_ALL = SPACE + UPPERCASE + LOWERCASE + NUMBERS + PUNCTUATION + LATIN_BASIC

	fun generate(fontName: String, fontSize: Int, chars: String): BitmapFont =
		generate(fontName, fontSize, chars.indices.map { chars[it].toInt() }.toIntArray())

	fun generate(fontName: String, fontSize: Int, chars: IntArray): BitmapFont {
		println("BitmapFontGenerator.generate($fontName, $fontSize, $chars)...")

		val result = measureTime {
			val bni = NativeImage(1, 1)
			val bnictx = bni.getContext2d()
			bnictx.font = Context2d.Font(fontName, fontSize.toDouble())
			val bitmapHeight = bnictx.getTextBounds("a").bounds.height.toInt()

			val widths: List<Int> = chars.map { bnictx.getTextBounds("${it.toChar()}").bounds.width.toInt() }
			val widthsSum = widths.map { it + 2 }.sum()
			val ni = NativeImage(widthsSum, bitmapHeight)

			//println("BitmapFont:")
			//println("bitmapHeight=$bitmapHeight")
			//for ((index, width) in widths.withIndex()) {
			//	val char = chars[index]
			//	println("$index: $char: width=$width")
			//}

			val g = ni.getContext2d()
			g.fillStyle = g.createColor(Colors.WHITE)
			g.font = Context2d.Font(fontName, fontSize.toDouble())
			g.horizontalAlign = Context2d.HorizontalAlign.LEFT
			g.verticalAlign = Context2d.VerticalAlign.TOP
			val glyphs = arrayListOf<BitmapFont.GlyphInfo>()
			var x = 0
			val itemp = IntArray(1)
			for ((index, char) in chars.withIndex()) {
				val width = widths[index]
				itemp[0] = char
				g.fillText(String_fromIntArray(itemp, 0, 1), x.toDouble(), 0.0)
				glyphs += BitmapFont.GlyphInfo(char, RectangleInt(x, 0, width, ni.height), width)
				x += width + 2
			}
			BitmapFont(ni.toBMP32(), fontSize, fontSize, glyphs)
		}

		println("   --> generated in ${result.time}")

		return result.result
	}
}