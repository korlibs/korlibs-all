package com.soywiz.korui.event

import com.soywiz.kds.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.input.*

data class MouseEvent(
	var type: Type = Type.MOVE,
	var id: Int = 0,
	var x: Int = 0,
	var y: Int = 0,
	var button: MouseButton = MouseButton.LEFT,
	var buttons: Int = 0,
	var isShiftDown: Boolean = false,
	var isCtrlDown: Boolean = false,
	var isAltDown: Boolean = false,
	var isMetaDown: Boolean = false
) :
	Event() {
	enum class Type { MOVE, OVER, OUT, UP, DOWN, CLICK }
}

data class MouseScrollEvent(
	var id: Int = 0,
	var x: Int = 0,
	var y: Int = 0,
	var delta: Double = 0.0,
	var isShiftDown: Boolean = false,
	var isCtrlDown: Boolean = false,
	var isAltDown: Boolean = false,
	var isMetaDown: Boolean = false
) : Event() {
}

data class Touch(
	val index: Int,
	var active: Boolean = false,
	var id: Int = -1,
	var startTime: Double = 0.0,
	var currentTime: Double = 0.0,
	var start: Vector2 = Vector2(),
	var current: Vector2 = Vector2()
) : Extra by Extra.Mixin() {
	companion object {
		val dummy = Touch(-1)
	}
}


data class TouchEvent(var type: Type, var screen: Int, var touch: Touch) : Event() {
	enum class Type { START, END, MOVE }
}

data class KeyEvent(var type: Type, var id: Int, var key: Key, var keyCode: Int, var char: Char) : Event() {
	enum class Type { UP, DOWN, PRESS }
}

data class GamePadConnectionEvent(var type: Type, var gamepad: Int) : Event() {
	enum class Type { CONNECTED, DISCONNECTED }
}

data class GamePadButtonEvent(var type: Type, var gamepad: Int, var button: GameButton, var value: Double) : Event() {
	enum class Type { UP, DOWN }
}

data class GamePadStickEvent(var gamepad: Int, var stick: GameStick, var x: Double, var y: Double) : Event() {
}

data class ChangeEvent(var oldValue: Any?, var newValue: Any?) : Event() {
}

data class ResizedEvent(var width: Int, var height: Int) : Event() {
}

data class DropFileEvent(var type: Type = Type.ENTER, var files: List<VfsFile>? = null) : Event() {
	enum class Type { ENTER, EXIT, DROP }
}

class MouseEvents(val ed: EventDispatcher) : Closeable {
	fun click(callback: () -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.CLICK) callback() }
	fun up(callback: () -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.UP) callback() }
	fun down(callback: () -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.DOWN) callback() }
	fun over(callback: () -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.OVER) callback() }
	fun out(callback: () -> Unit) = ed.addEventListener<MouseEvent> { if (it.type == MouseEvent.Type.OUT) callback() }
	override fun close() {
	}
}

class KeysEvents(val ed: EventDispatcher) : Closeable {
	fun down(callback: KeyEvent.() -> Unit) = ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.DOWN) callback(it) }
	fun up(callback: KeyEvent.() -> Unit) = ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.UP) callback(it) }
	fun press(callback: KeyEvent.() -> Unit) = ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.PRESS) callback(it) }

	fun down(key: Key, callback: KeyEvent.() -> Unit) = ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.DOWN && it.key == key) callback(it) }
	fun up(key: Key, callback: KeyEvent.() -> Unit) = ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.UP && it.key == key) callback(it) }
	fun press(key: Key, callback: KeyEvent.() -> Unit) = ed.addEventListener<KeyEvent> { if (it.type == KeyEvent.Type.PRESS && it.key == key) callback(it) }
	override fun close() {
	}
}

fun EventDispatcher.mouse(callback: MouseEvents.() -> Unit) = MouseEvents(this).apply(callback)
fun EventDispatcher.keys(callback: KeysEvents.() -> Unit) = KeysEvents(this).apply(callback)
