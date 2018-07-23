package com.soywiz.korge.view.tiles

import com.soywiz.kmem.*
import com.soywiz.korge.component.*
import com.soywiz.korge.render.*
import com.soywiz.korge.util.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

open class TileMap(val map: IntArray2, val tileset: TileSet) : View() {
	val tileWidth = tileset.width.toDouble()
	val tileHeight = tileset.height.toDouble()
	var smoothing = true

	private val t0 = MPoint2d(0, 0)
	private val tt0 = MPoint2d(0, 0)
	private val tt1 = MPoint2d(0, 0)

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return

		val renderTilesCounter = ctx.stats.counter("renderedTiles")

		val pos = m.transform(0.0, 0.0)
		val dU = m.transform(tileWidth, 0.0) - pos
		val dV = m.transform(0.0, tileHeight) - pos

		val colorMul = globalColorMul
		val colorAdd = globalColorAdd

		ctx.batch.setStateFast(
			ctx.getTex(tileset.base),
			blendFactors = computedBlendMode.factors, smoothing = smoothing
		)

		// @TODO: Bounds in clipped view
		val pp0 = globalToLocal(t0.setTo(ctx.virtualLeft, ctx.virtualTop), tt0)
		//val pp1 = globalToLocal(t0.setTo(views.actualVirtualWidth, views.actualVirtualHeight), tt1)
		val pp1 = globalToLocal(t0.setTo(ctx.virtualRight, ctx.virtualBottom), tt1)

		val mx0 = ((pp0.x / tileWidth) - 1).toInt().clamp(0, map.width)
		val mx1 = ((pp1.x / tileWidth) + 1).toInt().clamp(0, map.width)
		val my0 = ((pp0.y / tileHeight) - 1).toInt().clamp(0, map.height)
		val my1 = ((pp1.y / tileHeight) + 1).toInt().clamp(0, map.height)

		//views.stats.value("tiledmap.$name.bounds").set("${views.virtualLeft},${views.virtualTop},${views.virtualRight},${views.virtualBottom}")
		//views.stats.value("tiledmap.$name.pp0,pp1").set("$pp0,$pp1")
		//views.stats.value("tiledmap.$name.tileWidth,tileHeight").set("$tileWidth,$tileHeight")
		//views.stats.value("tiledmap.$name.mx0,my0").set("$mx0,$my0")
		//views.stats.value("tiledmap.$name.mx1,my1").set("$mx1,$my1")

		var count = 0
		for (y in my0 until my1) {
			for (x in mx0 until mx1) {
				if (x < 0 || x >= map.width) continue
				val tex = tileset[map[x, y]] ?: continue
				val p0 = pos + (dU * x.toDouble()) + (dV * y.toDouble())
				val p1 = p0 + dU
				val p2 = p0 + dU + dV
				val p3 = p0 + dV
				render(ctx, p0, p1, p2, p3, tex, colorMul, colorAdd)
				count++
			}
		}
		renderTilesCounter?.increment(count)

		ctx.flush()
	}

	open fun render(
		ctx: RenderContext,
		p0: Vector2,
		p1: Vector2,
		p2: Vector2,
		p3: Vector2,
		tex: BmpSlice,
		colorMul: Int,
		colorAdd: Int
	) {
		ctx.batch.drawQuadFast(
			p0.x.toFloat(), p0.y.toFloat(),
			p1.x.toFloat(), p1.y.toFloat(),
			p2.x.toFloat(), p2.y.toFloat(),
			p3.x.toFloat(), p3.y.toFloat(),
			ctx.getTex(tex), colorMul, colorAdd
		)
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(0, 0, tileWidth * map.width, tileHeight * map.height)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		return if (checkGlobalBounds(x, y, 0.0, 0.0, tileWidth * map.width, tileHeight * map.height)) this else null
	}
}

fun Views.tileMap(map: IntArray2, tileset: TileSet) = TileMap(map, tileset)

fun Container.tileMap(map: IntArray2, tileset: TileSet): TileMap = tileMap(map, tileset) { }

inline fun Container.tileMap(map: IntArray2, tileset: TileSet, callback: TileMap.() -> Unit): TileMap {
	val child = TileMap(map, tileset)
	this += child
	callback(child)
	return child
}
