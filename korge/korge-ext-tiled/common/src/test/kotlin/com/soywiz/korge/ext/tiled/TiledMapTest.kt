package com.soywiz.korge.ext.tiled

import com.soywiz.korge.resources.*
import com.soywiz.korge.tests.*
import com.soywiz.korim.format.*
import kotlin.test.*

class TiledMapTest : ViewsForTesting() {
	@Test
	@Ignore // Must fix mapping first
	fun name() = syncTest {
		disableNativeImageLoading {
			class Demo(@Path("sample.tmx") val map: TiledMap)

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
}
