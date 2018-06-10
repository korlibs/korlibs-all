package com.soywiz.korui.input

import com.soywiz.kmem.*
import kotlin.math.*

enum class MouseButton(val id: Int) {
	LEFT(0), RIGHT(1), MIDDLE(2), BUTTON3(3), BUTTON4(4), BUTTON5(5);

	companion object {
		val BUTTONS = values()
		operator fun get(id: Int) = BUTTONS[id]
	}
}

enum class Key {
	SPACE, APOSTROPHE, COMMA, MINUS, PERIOD, SLASH,
	N0, N1, N2, N3, N4, N5, N6, N7, N8, N9,
	SEMICOLON, EQUAL,
	A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
	LEFT_BRACKET, BACKSLASH, RIGHT_BRACKET, GRAVE_ACCENT,
	WORLD_1, WORLD_2,
	ESCAPE,
	ENTER, TAB, BACKSPACE, INSERT, DELETE,
	RIGHT, LEFT, DOWN, UP,
	PAGE_UP, PAGE_DOWN,
	HOME, END,
	CAPS_LOCK, SCROLL_LOCK, NUM_LOCK,
	PRINT_SCREEN, PAUSE,
	F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
	F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25,
	KP_0, KP_1, KP_2, KP_3, KP_4, KP_5, KP_6, KP_7, KP_8, KP_9,
	KP_DECIMAL, KP_DIVIDE, KP_MULTIPLY,
	KP_SUBTRACT, KP_ADD, KP_ENTER, KP_EQUAL,
	LEFT_SHIFT, LEFT_CONTROL, LEFT_ALT, LEFT_SUPER,
	RIGHT_SHIFT, RIGHT_CONTROL, RIGHT_ALT, RIGHT_SUPER,
	MENU,

	UNDERLINE, SELECT_KEY,

	UNKNOWN
}

enum class GameStick(val id: Int) {
	LEFT(0), RIGHT(1);

	companion object {
		val STICKS = values()
	}
}

enum class GameButton(val index: Int) {
	LEFT(0), RIGHT(1), UP(2), DOWN(3),
	BUTTON0(4), BUTTON1(5), BUTTON2(6), BUTTON3(7),
	SELECT(8), START(9), SYSTEM(10),
	L1(11), R1(12),
	L2(13), R2(14),
	L3(15), R3(16),
	LX(17), LY(18),
	RX(19), RY(20),
	BUTTON4(24), BUTTON5(25), BUTTON6(26), BUTTON7(27), BUTTON8(28);

	companion object {
		val BUTTONS = values()
		val MAX = 32
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

	operator fun get(button: GameButton) = mapping.get(button, buttons, axes)
	override fun toString(): String = "Gamepad[$index][$name]" + mapping.toString(buttons, axes)
}

abstract class GamepadMapping {
	abstract val id: String
	fun Int.getButton(index: Int): Double = if (getBit(index)) 1.0 else 0.0
	abstract fun get(button: GameButton, buttons: Int, axes: DoubleArray): Double

	fun toString(buttons: Int, axes: DoubleArray) = "$id(" + GameButton.values().joinToString(", ") {
		"${it.name}=${get(it, buttons, axes)}"
	} + ")"
}

// http://blog.teamtreehouse.com/wp-content/uploads/2014/03/standardgamepad.png
object StandardGamepadMapping : GamepadMapping() {
	override val id = "Standard"

	override fun get(button: GameButton, buttons: Int, axes: DoubleArray): Double {
		return when (button) {
			GameButton.BUTTON0 -> buttons.getButton(0)
			GameButton.BUTTON1 -> buttons.getButton(1)
			GameButton.BUTTON2 -> buttons.getButton(2)
			GameButton.BUTTON3 -> buttons.getButton(3)
			GameButton.L1 -> buttons.getButton(4)
			GameButton.R1 -> buttons.getButton(5)
			GameButton.L2 -> buttons.getButton(6)
			GameButton.R2 -> buttons.getButton(7)
			GameButton.SELECT -> buttons.getButton(8)
			GameButton.START -> buttons.getButton(9)
			GameButton.L3 -> buttons.getButton(10)
			GameButton.R3 -> buttons.getButton(11)
			GameButton.UP -> buttons.getButton(12)
			GameButton.DOWN -> buttons.getButton(13)
			GameButton.LEFT -> buttons.getButton(14)
			GameButton.RIGHT -> buttons.getButton(15)
			GameButton.SYSTEM -> buttons.getButton(16)
			GameButton.LX -> axes[0]
			GameButton.LY -> axes[1]
			GameButton.RX -> axes[2]
			GameButton.RY -> axes[3]
			else -> 0.0
		}
	}
}
