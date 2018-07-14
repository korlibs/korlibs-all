package com.soywiz.korge.tiled

import com.soywiz.korge.tests.*
import com.soywiz.korge.util.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import kotlin.test.*

class TiledMapTest : ViewsForTesting() {
	@Test
	@Ignore // Must fix mapping first
	fun name() = suspendTest {
		disableNativeImageLoading {
			//class Demo(@Path("sample.tmx") val map: TiledMap)
			class Demo(val map: TiledMap)

			val demo = injector.get<Demo>()
			val map = demo.map
			assertEquals(1, map.tilesets.size)
			assertEquals(1, map.tilesets.first().firstgid)
			assertEquals(256, map.tilesets.first().tileset.textures.size)
			assertEquals(3, map.allLayers.size)
			assertEquals(1, map.imageLayers.size)
			assertEquals(1, map.objectLayers.size)
			assertEquals(1, map.patternLayers.size)
			//println(map)
			//println(demo.map)
		}
	}

	@Test
	fun testRenderInBounds() = suspendTest {
		val tileset = TileSet(views, Bitmap32(32, 32).texture(views), 32, 32)
		val map = TileMap(IntArray2(200, 200), tileset, views)
		views.stage += map
		views.frameUpdateAndRender(false, 0)
		assertEquals(640, views.actualVirtualWidth)
		assertEquals(480, views.actualVirtualHeight)
		//assertEquals(300, count)
		assertEquals(336, map.renderTilesCounter.countThisFrame) // Update if optimized when no decimal scrolling
	}
}
