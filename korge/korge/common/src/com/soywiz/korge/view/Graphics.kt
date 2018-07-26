package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

inline fun Container.graphics(callback: Graphics.() -> Unit = {}): Graphics = Graphics().addTo(this).apply(callback)

class Graphics : Image(Bitmaps.transparent) {
	private val shapes = arrayListOf<Shape>()
	private var currentPath = GraphicsPath()
	private var fill: Context2d.Paint? = null
	@PublishedApi
	internal var dirty = true

	inline fun dirty(callback: () -> Unit) = this.apply {
		this.dirty = true
		callback()
	}

	fun clear() {
		shapes.clear()
	}

	fun lineStyle(thickness: Double, color: RGBA, alpha: Double) = dirty {
	}

	fun moveTo(x: Double, y: Double) = dirty {
		currentPath.moveTo(x, y)
	}

	fun lineTo(x: Double, y: Double) = dirty {
		currentPath.lineTo(x, y)
	}

	// Inline Class ERROR: Platform declaration clash: The following declarations have the same JVM signature (beginFill(ID)Lcom/soywiz/korge/view/Graphics;):
	//fun beginFill(color: Int, alpha: Double) = beginFill(RGBA(color), alpha)

	fun beginFill(color: RGBA, alpha: Double) = dirty {
		fill = Context2d.Color(RGBA(color.r, color.g, color.b, (alpha * 255).toInt()))
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
		shapes += FillShape(currentPath, null, fill ?: Context2d.Color(Colors.RED), Matrix2d())
		currentPath = GraphicsPath()
	}

	override var sLeft = 0.0
	override var sTop = 0.0

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
			this.bitmap = image.slice()
			sLeft = bounds.x
			sTop = bounds.y
		}
		super.render(ctx)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		val lx = globalToLocalX(x, y)
		val ly = globalToLocalY(x, y)
		for (shape in shapes) {
			if (shape.containsPoint(lx, ly)) return this
		}
		return null
	}
}
