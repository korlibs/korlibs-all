package com.soywiz.korim.vector

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

// References:
// - https://github.com/memononen/nanosvg/blob/master/src/nanosvgrast.h
// - https://www.geeksforgeeks.org/scan-line-polygon-filling-using-opengl-c/
// - https://hackernoon.com/computer-graphics-scan-line-polygon-fill-algorithm-3cb47283df6
// - https://nothings.org/gamedev/rasterize/
class Bitmap32Context2d(val bmp: Bitmap32) : Context2d.Renderer() {
	override val width: Int get() = bmp.width
	override val height: Int get() = bmp.height

	val colorFiller = ColorFiller()
	val gradientFiller = GradientFiller()

	// Super slow
	override fun render(state: Context2d.State, fill: Boolean) {
		//println("RENDER")
		val fillStyle = state.fillStyle
		val filler = when (fillStyle) {
			is Context2d.Color -> colorFiller.apply { this.set(fillStyle) }
			is Context2d.Gradient -> gradientFiller.apply { this.set(fillStyle) }
			else -> TODO()
		}
		val points = state.path.getPoints().map { it.transformed(state.transform) }
		val edges = arrayListOf<Edge>()
		for (n in 0 until points.size) {
			val a = points[n]
			val b = points[(n + 1) % points.size]
			val edge = if (a.y < b.y) Edge(a, b, +1) else Edge(b, a, -1)
			if (edge.isNotCoplanarX) {
				edges += edge
			}
		}
		val bounds = points.bounds()
		//println("bounds:$bounds")
		for (y in bounds.top.toInt() .. bounds.bottom.toInt()) {
			if (y !in 0 until bmp.height) continue // Calculate right range instead of skipping

			// @TODO: Optimize
			val xx = edges.filter { it.containsY(y) }.map { Point2d(it.intersectX(y), y) }.sortedBy { it.x }.map { it.x.toInt() }
			for (n in 0 until xx.size - 1) {
				val a = xx[n + 0].clamp(0, bmp.width)
				val b = xx[n + 1].clamp(0, bmp.width)

				filler.fill(bmp.data, bmp.index(a, y), a, y, b - a)
			}
			//println("y:$y -- $xx")
		}
		//println("WARNING: Not implemented context2d on Bitmap32, please use NativeImage instead. Filled the image with PINK.")
		//bmp.fill(Colors.PINK)
	}

	data class Edge(val a: IPoint2d, val b: IPoint2d, val wind: Int) {
		val isCoplanarX = a.y == b.y
		val isNotCoplanarX get() = !isCoplanarX

		val isCoplanarY = a.x == b.x

		private val slope = (b.y - a.y) / (b.x - a.x)
		private val h = a.y - (a.x * slope)

		//init {
			//println("a=$a,b=$b :: h=$h,slope=$slope, coplanaer=")
		//}

		fun containsY(y: Int): Boolean = y in (a.y .. b.y)
		fun intersectX(y: Int): Double = if (isCoplanarY) a.x else ((y - h) / slope)
	}

	abstract class Filler<T : Context2d.Paint> {
		protected lateinit var fill: T
		fun set(paint: T) {
			fill = paint
			updated()
		}
		open fun updated() {
		}
		abstract fun fill(data: IntArray, offset: Int, x: Int, y: Int, count: Int)
	}

	class ColorFiller : Filler<Context2d.Color>() {
		override fun fill(data: IntArray, offset: Int, x: Int, y: Int, count: Int) {
			val c = fill.color
			for (n in 0 until count) {
				data[offset + n] = c
			}
		}
	}

	class GradientFiller : Filler<Context2d.Gradient>() {
		val NCOLORS = 256
		val colors = IntArray(NCOLORS)

		fun stopN(n: Int): Int = (fill.stops[n] * NCOLORS).toInt()

		override fun updated() {
			for (n in 0 until stopN(0)) colors[n] = fill.colors.first()
			for (n in 0 until fill.numberOfStops - 1) {
				val stop0 = stopN(n + 0)
				val stop1 = stopN(n + 1)
				val color0 = fill.colors[n + 0]
				val color1 = fill.colors[n + 1]
				for (s in stop0 until stop1) {
					val ratio = (s - stop0).toDouble() / (stop1 - stop0).toDouble()
					colors[s] = RGBA.interpolate(color0, color1, ratio)
				}
			}
			for (n in stopN(fill.numberOfStops - 1) until NCOLORS) colors[n] = fill.colors.last()
			//println(colors.map { RGBA.toHexString(it) })
		}

		override fun fill(data: IntArray, offset: Int, x: Int, y: Int, count: Int) {
			//val mat = Matrix2d().scale(1.0 / 64.0, 1.0)
			val mat = Matrix2d().apply {
				pretranslate(fill.x0, fill.y0)
				prescale((fill.x1 - fill.x0), (fill.y1 - fill.y0))
				prerotate(Angle.betweenRad(fill.x0, fill.y0, fill.x1, fill.y1))
				setToInverse()
			}

			//println(mat)

			for (n in 0 until count) {
				val ratio = mat.transformY((x + n).toDouble(), y.toDouble()).clamp01()
				data[offset + n] = colors[(ratio * (NCOLORS - 1)).toInt()]
			}
		}
	}
}