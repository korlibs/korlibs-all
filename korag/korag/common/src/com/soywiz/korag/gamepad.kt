package com.soywiz.korag

import com.soywiz.kmem.*
import kotlin.math.*

enum class GamepadButton(val index: Int) {
	LEFT(0), RIGHT(1), UP(2), DOWN(3),
	BUTTON0(4), BUTTON1(5), BUTTON2(6), BUTTON3(7),
	SELECT(8), START(9), SYSTEM(10),
	L1(11), R1(12),
	L2(13), R2(14),
	L3(15), R3(16),
	LX(17), LY(18),
	RX(19), RY(20);

	companion object {
	    val MAX_INDEX = 32
	}
}

class GamepadInfo(
	var index: Int = 0,
	var connected: Boolean = false,
	var name: String = "unknown",
	var mapping: GamepadMapping = StandardGamepadMapping,
	var buttons: Int = 0,
	val axes: DoubleArray = DoubleArray(16)
) {
	fun copyFrom(that: GamepadInfo) {
		this.index = that.index
		this.name = that.name
		this.mapping = that.mapping
		this.buttons = that.buttons
		this.connected = that.connected
		arraycopy(that.axes, 0, this.axes, 0, min(this.axes.size, that.axes.size))
	}

	operator fun get(button: GamepadButton) = mapping.get(button, buttons, axes)
	override fun toString(): String = "Gamepad[$index][$name]" + mapping.toString(buttons, axes)
}

abstract class GamepadMapping {
	abstract val id: String
	fun Int.getButton(index: Int): Double = if (getBit(index)) 1.0 else 0.0
	abstract fun get(button: GamepadButton, buttons: Int, axes: DoubleArray): Double

	fun toString(buttons: Int, axes: DoubleArray) = "$id(" + GamepadButton.values().joinToString(", ") {
		"${it.name}=${get(it, buttons, axes)}"
	} + ")"
}

// http://blog.teamtreehouse.com/wp-content/uploads/2014/03/standardgamepad.png
object StandardGamepadMapping : GamepadMapping() {
	override val id = "Standard"

	override fun get(button: GamepadButton, buttons: Int, axes: DoubleArray): Double {
		return when (button) {
			GamepadButton.BUTTON0 -> buttons.getButton(0)
			GamepadButton.BUTTON1 -> buttons.getButton(1)
			GamepadButton.BUTTON2 -> buttons.getButton(2)
			GamepadButton.BUTTON3 -> buttons.getButton(3)
			GamepadButton.L1 -> buttons.getButton(4)
			GamepadButton.R1 -> buttons.getButton(5)
			GamepadButton.L2 -> buttons.getButton(6)
			GamepadButton.R2 -> buttons.getButton(7)
			GamepadButton.SELECT -> buttons.getButton(8)
			GamepadButton.START -> buttons.getButton(9)
			GamepadButton.L3 -> buttons.getButton(10)
			GamepadButton.R3 -> buttons.getButton(11)
			GamepadButton.UP -> buttons.getButton(12)
			GamepadButton.DOWN -> buttons.getButton(13)
			GamepadButton.LEFT -> buttons.getButton(14)
			GamepadButton.RIGHT -> buttons.getButton(15)
			GamepadButton.SYSTEM -> buttons.getButton(16)
			GamepadButton.LX -> axes[0]
			GamepadButton.LY -> axes[1]
			GamepadButton.RX -> axes[2]
			GamepadButton.RY -> axes[3]
			else -> 0.0
		}
	}
}
