package com.soywiz.korge.view.tiles

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import kotlin.math.*

class TileSet(
	val views: Views,
	val textures: List<Texture?>,
	val width: Int,
	val height: Int,
	val base: Texture.Base = textures.filterNotNull().firstOrNull()?.base ?: views.transparentTexture.base
) {
	init {
		if (textures.any { if (it != null) it.base != base else false }) {
			throw RuntimeException("All tiles in the set must have the same base texture")
		}
	}

	operator fun get(index: Int): Texture? = textures.getOrNull(index)

	companion object {
		operator fun invoke(
			views: Views,
			base: Texture,
			tileWidth: Int,
			tileHeight: Int,
			columns: Int = -1,
			totalTiles: Int = -1
		): TileSet {
			val out = arrayListOf<Texture>()
			val rows = base.height / tileHeight
			val actualColumns = if (columns < 0) base.width / tileWidth else columns
			val actualTotalTiles = if (totalTiles < 0) rows * actualColumns else totalTiles

			complete@ for (y in 0 until rows) {
				for (x in 0 until actualColumns) {
					out += base.slice(x * tileWidth, y * tileHeight, tileWidth, tileHeight)
					if (out.size >= actualTotalTiles) break@complete
				}
			}

			return TileSet(views, out, tileWidth, tileHeight)
		}

		fun extractBitmaps(
			bmp: Bitmap32,
			tilewidth: Int,
			tileheight: Int,
			columns: Int,
			tilecount: Int
		): List<Bitmap32> {
			return (0 until tilecount).map { n ->
				val y = n / columns
				val x = n % columns
				bmp.sliceWithSize(x * tilewidth, y * tileheight, tilewidth, tileheight).extract()
			}
		}

		fun fromBitmaps(
			views: Views,
			tilewidth: Int,
			tileheight: Int,
			bitmaps: List<Bitmap32>,
			border: Int = 1,
			mipmaps: Boolean = false
		): TileSet {
			check(bitmaps.all { it.width == tilewidth && it.height == tileheight })
			if (bitmaps.isEmpty()) return TileSet(views, listOf(), tilewidth, tileheight)

			//sqrt(bitmaps.size.toDouble()).toIntCeil() * tilewidth

			val border2 = border * 2
			val btilewidth = tilewidth + border2
			val btileheight = tileheight + border2
			val barea = btilewidth * btileheight
			val fullArea = bitmaps.size * barea
			val expectedSide = sqrt(fullArea.toDouble()).toIntCeil().nextPowerOfTwo

			val out = Bitmap32(expectedSide, expectedSide)
			val texs = arrayListOf<Texture>()

			val columns = (out.width / btilewidth)

			lateinit var tex: Texture
			//val tex = views.texture(out, mipmaps = mipmaps)
			for (m in 0 until 2) {
				for (n in 0 until bitmaps.size) {
					val y = n / columns
					val x = n % columns
					val px = x * btilewidth + border
					val py = y * btileheight + border
					if (m == 0) {
						out.putWithBorder(px, py, bitmaps[n], border)
					} else {
						texs += tex.slice(px, py, tilewidth, tileheight)
					}
				}
				if (m == 0) {
					tex = views.texture(out, mipmaps = mipmaps)
				}
			}

			return TileSet(views, texs, tilewidth, tileheight, tex.base)
		}
	}
}

fun Views.tileSet(
	textures: List<Texture?>,
	width: Int,
	height: Int,
	base: Texture.Base = textures.filterNotNull().first().base
): TileSet {
	return TileSet(this, textures, width, height, base)
}

fun Views.tileSet(textureMap: Map<Int, Texture?>): TileSet {
	val views = this
	val maxKey = textureMap.keys.max() ?: 0
	val textures = (0..maxKey).map { textureMap[it] }
	val firstTexture = textures.first() ?: views.transparentTexture
	return TileSet(this, textures, firstTexture.width, firstTexture.height, firstTexture.base)
}
