package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

class Graphics : Container() {
	private val img = Image(Bitmaps.transparent)
	private val shapes = arrayListOf<Shape>()
	private var currentPath = GraphicsPath()
	@PublishedApi
	internal var dirty = true

	inline fun dirty(callback: () -> Unit) = this.apply {
		this.dirty = true
		callback()
	}

	fun clear() {
		shapes.clear()
	}

	fun lineStyle(d: Double, i: Int, d1: Double) = dirty {
	}

	fun moveTo(x: Double, y: Double) = dirty {
		currentPath.moveTo(x, y)
	}

	fun lineTo(x: Double, y: Double) = dirty {
		currentPath.lineTo(x, y)
	}

	fun beginFill(i: Int, d: Double) = dirty {
		currentPath = GraphicsPath()
	}

	fun drawCircle(x: Double, y: Double, r: Double) = dirty {
		currentPath.circle(x, y, r)
	}

	fun drawRect(x: Double, y: Double, width: Double, height: Double) = dirty {
		currentPath.rect(x, y, width, height)
	}

	fun drawEllipse(x: Double, y: Double, rw: Double, rh: Double) = dirty {
		currentPath.ellipse(x, y, rw, rh)
	}

	fun endFill() = dirty {
		shapes += FillShape(currentPath, null, Context2d.Color(Colors.RED), Matrix2d())
		currentPath = GraphicsPath()
	}

	override fun render(ctx: RenderContext) {
		if (dirty) {
			dirty = false
			val bounds = shapes.map { it.getBounds() }.bounds()
			val image = NativeImage(bounds.width.toInt(), bounds.height.toInt())
			image.context2d {
				for (shape in shapes) {
					shape.draw(this)
				}
			}
			img.position(bounds.x, bounds.y).bitmap = image.slice()
		}
		super.render(ctx)
	}
}
