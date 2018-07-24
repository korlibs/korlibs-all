package com.soywiz.korge.view

import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.event.*
import kotlin.reflect.*

inline fun Container.container(callback: @ViewsDslMarker Container.() -> Unit = {}) =
	Container().addTo(this).apply(callback)

open class Container : View() {
	val children = arrayListOf<View>()
	val containerRoot: Container get() = parent?.containerRoot ?: this

	fun removeChildren() {
		for (child in children) {
			child.parent = null
			child.index = -1
		}
		children.clear()
	}

	fun addChild(view: View) = this.plusAssign(view)

	override fun invalidate() {
		dirtyVertices = true
		validGlobal = false
		for (child in children) {
			if (!child.validGlobal) continue
			child.validGlobal = false
			child.invalidate()
		}
	}

	operator fun plusAssign(view: View) {
		view.removeFromParent()
		view.index = children.size
		children += view
		view.parent = this
		view.invalidate()
	}

	operator fun minusAssign(view: View) {
		if (view.parent == this) view.removeFromParent()
	}

	private val tempMatrix = Matrix2d()
	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		val isGlobal = (m === globalMatrix)
		safeForEachChildren { child ->
			if (isGlobal) {
				child.render(ctx, child.globalMatrix)
			} else {
				tempMatrix.multiply(child.localMatrix, m)
				child.render(ctx, tempMatrix)
			}
		}
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		for (child in children.reversed().filter(View::visible)) return child.hitTest(x, y) ?: continue
		return null
	}

	override fun hitTestBoundingInternal(x: Double, y: Double): View? {
		for (child in children.reversed().filter(View::visible)) return child.hitTestBounding(x, y) ?: continue
		return null
	}

	private val bb = BoundsBuilder()
	private val tempRect = Rectangle()

	override fun getLocalBoundsInternal(out: Rectangle) {
		bb.reset()
		safeForEachChildren { child ->
			child.getBounds(child, tempRect)
			bb.add(tempRect)
		}
		bb.getBounds(out)
	}

	override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
		safeForEachChildrenReversed { child ->
			child.dispatch(clazz, event)
		}
		super.dispatch(clazz, event)
	}

	private inline fun safeForEachChildren(crossinline callback: (View) -> Unit) {
		var n = 0
		while (n < children.size) {
			callback(children[n])
			n++
		}
	}

	private inline fun safeForEachChildrenReversed(crossinline callback: (View) -> Unit) {
		var n = 0
		while (n < children.size) {
			callback(children[children.size - n - 1])
			n++
		}
	}

	override fun createInstance(): View = Container()

	override fun clone(): View {
		val out = super.clone()
		for (child in children) out += child.clone()
		return out
	}
}

fun <T : View> T.addTo(parent: Container) = this.apply { parent += this }

open class FixedSizeContainer(override var width: Double = 100.0, override var height: Double = 100.0) : Container() {
	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(0, 0, width, height)
	}

	override fun toString(): String {
		var out = super.toString()
		out += ":size=(${width.niceStr}x${height.niceStr})"
		return out
	}
}

operator fun View?.plusAssign(view: View?) {
	val container = this as? Container?
	if (view != null) container?.addChild(view)
}
