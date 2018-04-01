package com.soywiz.kmedialayer.scene

import com.soywiz.kmedialayer.*

interface Component {
	val view: View
}

fun Component.removeFromView() = view.removeComponent(this)

interface MouseComponent : Component {
	fun onMouseMove(x: Int, y: Int)
	fun onMouseUp(button: Int)
	fun onMouseDown(button: Int)
	fun onMouseClick(button: Int)
}

interface KeyComponent : Component {
	fun onKeyUp(key: Key)
	fun onKeyDown(key: Key)
}

interface GamepadComponent : Component {
	fun onGamepadConnectionUpdate(player: Int, name: String, connected: Boolean)
	fun onGamepadButtonUpdate(player: Int, button: GameButton, value: Double)
	fun onGamepadStickUpdate(player: Int, stick: GameStick, x: Double, y: Double)
}

interface UpdateComponent : Component {
	fun update(ms: Double)
}

interface ResizeComponent : Component {
	fun resized(width: Int, height: Int)
}
