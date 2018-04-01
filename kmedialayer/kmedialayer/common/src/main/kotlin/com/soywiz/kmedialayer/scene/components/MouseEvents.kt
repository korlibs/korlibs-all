package com.soywiz.kmedialayer.scene.components

import com.soywiz.kmedialayer.scene.*
import com.soywiz.kmedialayer.scene.util.*

abstract class MouseEvents {
	abstract val x: Double
	abstract val y: Double
	val click: Signal<View> = Signal()
	val clickOutside: Signal<View> = Signal()
	val down: Signal<View> = Signal()
	val downOutside: Signal<View> = Signal()
	val up: Signal<View> = Signal()
	val upOutside: Signal<View> = Signal()
	val out: Signal<View> = Signal()
	val over: Signal<View> = Signal()
}

private class MouseEventsComponent(override val view: View) : MouseEvents(), MouseComponent, UpdateComponent {
	var globalX = 0.0
	var globalY = 0.0

	override val x: Double get() = view.globalToLocalX(globalX, globalY)
	override val y: Double get() = view.globalToLocalY(globalX, globalY)

	var lastInside: Boolean? = null

	override fun onMouseMove(x: Int, y: Int) {
		globalX = x.toDouble()
		globalY = y.toDouble()
	}

	override fun onMouseUp(button: Int) {
		if (lastInside == true) {
			up(view)
		} else {
			upOutside(view)
		}
	}

	override fun onMouseDown(button: Int) {
		if (lastInside == true) {
			down(view)
		} else {
			downOutside(view)
		}
	}

	override fun onMouseClick(button: Int) {
		if (lastInside == true) {
			click(view)
		} else {
			clickOutside(view)
		}
	}

	override fun update(ms: Double) {
		val mouseX = view.scene?.application?.mouse?.x ?: 0
		val mouseY = view.scene?.application?.mouse?.y ?: 0
		val nowInside = view.viewInGlobal(mouseX.toDouble(), mouseY.toDouble()) != null
		if (lastInside != nowInside) {
			this.lastInside = nowInside
			if (nowInside) {
				over(view)
			} else {
				out(view)
			}
		}
	}
}

val View.mouse get() = getOrCreateComponent { MouseEventsComponent(it) } as MouseEvents

fun View.mouse(callback: MouseEvents.() -> Unit) = this.mouse.apply(callback)

