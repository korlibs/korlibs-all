package com.soywiz.kmedialayer.scene.components

import com.soywiz.kmedialayer.*
import com.soywiz.kmedialayer.scene.*
import com.soywiz.kmedialayer.scene.util.*

interface GamepadEvents {
	val connection: Signal<Player>
	val connected: Signal<Player>
	val disconnected: Signal<Player>
	val stickUpdate: Signal<StickData>
	val up: Signal<GameButtonState>
	val down: Signal<GameButtonState>
	val pressing: Signal<GameButtonState>
	fun stick(player: Int, stick: GameStick, callback: View.(Double, Double) -> Unit)
	fun down(player: Int, button: GameButton, callback: View.(Double) -> Unit)
	fun up(player: Int, button: GameButton, callback: View.(Double) -> Unit)
}

data class GameButtonState(var button: GameButton, var player: Player, var value: Double) {
	val pressed: Boolean get() = value >= 0.25
	val playerId: Int get() = player.playerId
}

data class StickData(val stick: GameStick, val player: Player, var x: Double = 0.0, var y: Double = 0.0) {
	val playerId: Int get() = player.playerId
}

data class Player(
	val index: Int,
	var playerId: Int = index,
	var name: String = "unknown",
	var connected: Boolean = false
) {
	val sticks = GameStick.STICKS.map { StickData(it, this) }
	val buttons = GameButton.BUTTONS.map { GameButtonState(it, this, 0.0) }
}

class GamepadEventsComponent(override val view: View) : GamepadComponent, GamepadEvents {
	override val connection = Signal<Player>()
	override val connected = Signal<Player>()
	override val disconnected = Signal<Player>()

	override val up = Signal<GameButtonState>()
	override val down = Signal<GameButtonState>()
	override val pressing = Signal<GameButtonState>()
	override val stickUpdate = Signal<StickData>()

	override fun down(player: Int, button: GameButton, callback: View.(Double) -> Unit): Unit =
		run { down { if (it.button == button && it.playerId == player) callback(view, it.value) } }

	override fun up(player: Int, button: GameButton, callback: View.(Double) -> Unit): Unit =
		run { up { if (it.button == button && it.playerId == player) callback(view, it.value) } }

	override fun stick(player: Int, stick: GameStick, callback: View.(Double, Double) -> Unit) {
		run { stickUpdate { if (it.playerId == player && it.stick == stick) callback(view, it.x, it.y) } }
	}

	val players = (0 until 16).map { Player(it) }

	override fun onGamepadButtonUpdate(player: Int, button: GameButton, value: Double) {
		val but = players[player].buttons[button.id]
		val oldPressed = but.pressed
		but.value = value
		val newPressed = but.pressed
		if (oldPressed != newPressed) {
			if (newPressed) down(but) else up(but)
		}
		if (newPressed) pressing(but)
	}

	override fun onGamepadConnectionUpdate(player: Int, name: String, connected: Boolean) {
		val pp = players[player].also {
			it.connected = connected
			it.name = name
		}
		var nindex = 0
		for (pp in players) {
			if (pp.connected) pp.playerId = nindex++
		}
		connection(pp)
		if (connected) connected(pp) else disconnected(pp)
	}

	override fun onGamepadStickUpdate(player: Int, stick: GameStick, x: Double, y: Double) {
		var updated = false
		val s = players[player].sticks[stick.id].also {
			if (it.x != x || it.y != y || x != 0.0 || y != 0.0) {
				updated = true
			}
			it.x = x
			it.y = y
		}
		if (updated) stickUpdate(s)
	}
}

operator fun GamepadEvents.invoke(callback: GamepadEvents.() -> Unit) = this.apply(callback)
val View.gamepad get() = getOrCreateComponent { GamepadEventsComponent(it) } as GamepadEvents