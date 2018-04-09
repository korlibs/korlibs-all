package com.soywiz.korui.ui

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korui.light.*

private var Component.mouseEventOnce by Extra.Property { Once() }
private var Component.keyEventOnce by Extra.Property { Once() }
private var Component.touchEventOnce by Extra.Property { Once() }
private var Component.gamepadEventOnce by Extra.Property { Once() }
private var Component.changeEventOnce by Extra.Property { Once() }

private fun <T> createMyHandler(init: Component.() -> Unit) =
	Extra.PropertyThis<Component, Signal<T>> { Signal { init() } }

private fun createMouseHandler() = createMyHandler<LightMouseHandler.Info> { registerMouseEventOnce() }
private fun createKeyHandler() = createMyHandler<LightKeyHandler.Info> { registerKeyEventOnce() }
private fun createTouchHandler() = createMyHandler<LightTouchHandler.Info> { registerTouchEventOnce() }
private fun createGamepadHandler() = createMyHandler<LightGamepadHandler.Info> { registerGamepadEventOnce() }
private fun createChangeHandler() = createMyHandler<LightChangeHandler.Info> { registerChangeEventOnce() }

val Component.onMouseUp by createMouseHandler()
val Component.onMouseDown by createMouseHandler()
val Component.onMouseClick by createMouseHandler()
val Component.onMouseOver by createMouseHandler()
val Component.onMouseDrag by createMouseHandler()
val Component.onMouseEnter by createMouseHandler()
val Component.onMouseExit by createMouseHandler()

val Component.onKeyTyped by createKeyHandler()
val Component.onKeyDown by createKeyHandler()
val Component.onKeyUp by createKeyHandler()

val Component.onTouchStart by createTouchHandler()
val Component.onTouchEnd by createTouchHandler()
val Component.onTouchMove by createTouchHandler()

val Component.onGamepadUpdate by createGamepadHandler()
val Component.onGamepadConnection by createGamepadHandler()

val Component.onChange by createChangeHandler()

private fun Component.registerMouseEventOnce(): Unit = mouseEventOnce {
	fun LightMouseHandler.Info.handle(): LightMouseHandler.Info = this.apply {
		this@registerMouseEventOnce.mouseX = this@apply.x
		this@registerMouseEventOnce.mouseY = this@apply.y
	}
	lc.addHandler(handle, object : LightMouseHandler() {
		override fun enter(info: Info) = onMouseEnter(info.handle())
		override fun exit(info: Info) = onMouseExit(info.handle())
		override fun over(info: Info) = onMouseOver(info.handle())
		override fun drag(info: Info) = onMouseDrag(info.handle())
		override fun up(info: Info) = onMouseUp(info.handle())
		override fun down(info: Info) = onMouseDown(info.handle())
		override fun click(info: Info) = onMouseClick(info.handle())
	})
}

private fun Component.registerKeyEventOnce(): Unit = keyEventOnce {
	lc.addHandler(handle, object : LightKeyHandler() {
		override fun typed(info: Info) = onKeyTyped(info)
		override fun down(info: Info) = onKeyDown(info)
		override fun up(info: Info) = onKeyUp(info)
	})
}

private fun Component.registerTouchEventOnce(): Unit = touchEventOnce {
	lc.addHandler(handle, object : LightTouchHandler() {
		override fun start(info: Info) = onTouchStart(info)
		override fun end(info: Info) = onTouchEnd(info)
		override fun move(info: Info) = onTouchMove(info)
	})
}

private fun Component.registerGamepadEventOnce(): Unit = gamepadEventOnce {
	lc.addHandler(handle, object : LightGamepadHandler() {
		override fun update(info: Info) = onGamepadUpdate(info)
		override fun connection(info: Info) = onGamepadConnection(info)
	})
}

private fun Component.registerChangeEventOnce(): Unit = changeEventOnce {
	lc.addHandler(handle, object : LightChangeHandler() {
		override fun changed(info: Info) = onChange(info)
	})
}
