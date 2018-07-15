package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korma.*
import com.soywiz.korui.event.*

val View.drag by Extra.PropertyThis<View, DragComponent> { this.getOrCreateComponent { DragComponent(this) } }

inline fun <T : View?> T?.onDragStart(noinline handler: suspend (DragComponent.Info) -> Unit) =
	this.apply { this?.drag?.onDragStart?.addSuspend(this.views.coroutineContext, handler) }

inline fun <T : View?> T?.onDragEnd(noinline handler: suspend (DragComponent.Info) -> Unit) =
	this.apply { this?.drag?.onDragEnd?.addSuspend(this.views.coroutineContext, handler) }

inline fun <T : View?> T?.onDragMove(noinline handler: suspend (DragComponent.Info) -> Unit) =
	this.apply { this?.drag?.onDragMove?.addSuspend(this.views.coroutineContext, handler) }

class DragComponent(view: View) : Component(view) {
	data class Info(
		var touch: Touch = Touch.dummy,
		var gstart: MVector2 = MVector2(),
		var gend: MVector2 = MVector2(),
		var delta: MVector2 = MVector2()
	) {
		val id get() = touch.id
	}

	var Touch.dragging by extraProperty("DragComponent.dragging") { false }

	val info = Info()
	val onDragStart = Signal<Info>()
	val onDragMove = Signal<Info>()
	val onDragEnd = Signal<Info>()

	private fun updateStartEndPos(touch: Touch) {
		info.gstart.copyFrom(touch.current)
		info.gend.copyFrom(touch.current)
		info.delta.setToSub(info.gend, info.gstart)
	}

	private fun updateEndPos(touch: Touch) {
		info.gend.copyFrom(touch.current)
		info.delta.setToSub(info.gend, info.gstart)
	}

	init {
		addEventListener<TouchEvent> { e ->
			val touch = e.touch
			info.touch = touch
			when (e.type) {
				TouchEvent.Type.START -> {
					if (view.hitTest(touch.current.x, touch.current.y) != null) {
						touch.dragging = true
						updateStartEndPos(touch)
						onDragStart(info)
					}
				}
				TouchEvent.Type.END -> {
					if (touch.dragging) {
						touch.dragging = false
						updateEndPos(touch)
						onDragEnd(info)
					}
				}
				else -> {
					if (touch.dragging) {
						updateEndPos(touch)
						onDragMove(info)
					}
				}
			}
		}
	}
}
