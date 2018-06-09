package com.soywiz.korge.tiled

import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.*

class TiledMapView(views: Views, val tiledMap: TiledMap) : Container(views) {
	init {
		for ((index, layer) in tiledMap.allLayers.withIndex()) {
			if (layer is TiledMap.Layer.Patterns) {
				this += TileMap(layer.map, tiledMap.tileset, views)
			}
		}
	}
}

fun TiledMap.createView(views: Views) = TiledMapView(views, this)
fun Views.tiledMap(tiledMap: TiledMap) = TiledMapView(this, tiledMap)
