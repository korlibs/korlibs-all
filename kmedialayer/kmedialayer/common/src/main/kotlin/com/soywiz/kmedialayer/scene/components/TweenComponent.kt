package com.soywiz.kmedialayer.scene.components

import com.soywiz.kmedialayer.*
import com.soywiz.kmedialayer.scene.*
import com.soywiz.kmedialayer.scene.geom.*
import com.soywiz.kmedialayer.scene.util.*
import kotlin.reflect.*

class TweenComponent(
	override val view: View,
	props: Array<out PropertyTransition<*>>,
	val time: Double,
	val easing: Easing = Easing.LINEAR
) : UpdateComponent {
	val onDone = Signal<Unit>()
	val totalMs = time * 1000.0
	val src = Array(props.size) { props[it].src ?: props[it].prop.get() }
	val dst = Array(props.size) { props[it].dst }
	val props = Array(props.size) { props[it].prop as KMutableProperty0<Any> }

	var elapsed = 0.0
	override fun update(ms: Double) {
		elapsed += ms
		//println("elapsed: $elapsed, $totalMs : $ms")
		val ratio = clamp(elapsed / totalMs, 0.0, 1.0)
		updateRatio(ratio)
	}

	fun updateRatio(ratio: Double) {
		val eratio = easing(ratio)

		for (n in 0 until props.size) {
			val prop = props[n]
			val src = src[n]
			val dst = dst[n]
			val res = interpolate(eratio, src, dst)
			//println("$prop: $src :  $dst :: $res ($ratio)")
			prop.set(res)
		}

		if (ratio >= 1.0) {
			this.removeFromView()
			onDone(Unit)
		}
	}
}

data class PropertyTransition<R : Any>(val prop: KMutableProperty0<R>, val src: R?, val dst: R)

operator fun <R : Any> KMutableProperty0<R>.get(dst: R) = PropertyTransition(this, null, dst)
operator fun <R : Any> KMutableProperty0<R>.get(src: R, dst: R) = PropertyTransition(this, src, dst)

suspend fun <T : View> T.tween(
	vararg props: PropertyTransition<*>,
	time: Double = 1.0,
	easing: Easing = Easing.LINEAR
) {
	val tween = TweenComponent(this, props, time, easing)
	tween.update(0.0)
	addComponent(tween)
	try {
		tween.onDone.awaitOne()
	} catch (e: CancelException) {
		if (e.complete) tween.updateRatio(1.0)
		tween.removeFromView()
	}
}

suspend fun View.moveBy(dx: Double, dy: Double, time: Double = 1.0, easing: Easing = Easing.LINEAR) =
	tween(this::x[this.x + dx], this::y[this.y + dy], time = time, easing = easing)

suspend fun View.rotateBy(angle: Double, time: Double = 1.0, easing: Easing = Easing.LINEAR) =
	tween(this::rotationDegrees[this.rotationDegrees + angle], time = time, easing = easing)

suspend fun View.moveTo(x: Double, y: Double, time: Double = 1.0, easing: Easing = Easing.LINEAR) =
	tween(this::x[x], this::y[y], time = time, easing = easing)

suspend fun View.show(time: Double = 1.0, easing: Easing = Easing.LINEAR) =
	tween(this::alpha[1.0], time = time, easing = easing)

suspend fun View.hide(time: Double = 1.0, easing: Easing = Easing.LINEAR) =
	tween(this::alpha[0.0], time = time, easing = easing)
