@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korge.tween

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.color.RGBA.Companion.blendRGBA
import com.soywiz.korma.interpolation.*
import kotlinx.coroutines.experimental.*
import kotlin.reflect.*

class TweenComponent(
	override val view: View,
	private val vs: List<V2<*>>,
	val time: Int? = null,
	val easing: Easing = Easing.LINEAR,
	val callback: (Double) -> Unit,
	val c: CancellableContinuation<Unit>
) : UpdateComponent {
	var elapsed = 0
	val ctime = time ?: vs.map { it.endTime }.max() ?: 1000
	var cancelled = false
	var done = false

	init {
		c.invokeOnCancellation {
			cancelled = true
			//println("TWEEN CANCELLED[$this, $vs]: $elapsed")
		}
		update(0.0)
	}

	fun completeOnce() {
		if (!done) {
			done = true
			detach()
			c.resume(Unit)
			//println("TWEEN COMPLETED[$this, $vs]: $elapsed. thread=$currentThreadId")
		}
	}

	override fun update(ms: Double) {
		val dtMs = ms.toInt()
		//println("TWEEN UPDATE[$this, $vs]: $elapsed + $dtMs")
		if (cancelled) return completeOnce()
		elapsed += dtMs

		val ratio = (elapsed.toDouble() / ctime.toDouble()).clamp(0.0, 1.0)
		for (v in vs) {
			val durationInTween = (v.duration ?: (ctime - v.startTime))
			val elapsedInTween = (elapsed - v.startTime).clamp(0, durationInTween)
			val ratioInTween =
				if (durationInTween <= 0.0) 1.0 else elapsedInTween.toDouble() / durationInTween.toDouble()
			v.set(easing(ratioInTween))
		}
		callback(easing(ratio))

		if (ratio >= 1.0) return completeOnce()
	}

	override fun toString(): String = "TweenComponent($view)"
}

suspend fun View?.tween(
	vararg vs: V2<*>,
	time: TimeSpan,
	easing: Easing = Easing.LINEAR,
	callback: (Double) -> Unit = { }
): Unit {
	if (this != null) {
		withTimeout(300 + time.milliseconds * 2) {
			suspendCancellableCoroutine<Unit> { c ->
				val view = this@tween
				//println("STARTED TWEEN at thread $currentThreadId")
				TweenComponent(view, vs.toList(), time.milliseconds, easing, callback, c).attach()
			}
		}
	}
}

@Suppress("UNCHECKED_CAST")
data class V2<V>(
	internal val key: KMutableProperty0<V>,
	internal val initial: V,
	internal val end: V,
	internal val interpolator: (V, V, Double) -> V,
	internal val startTime: Int = 0,
	internal val duration: Int? = null
) {
	val endTime = startTime + (duration ?: 0)

	@Deprecated("", replaceWith = ReplaceWith("key .. (initial...end)", "com.soywiz.korge.tween.rangeTo"))
	constructor(key: KMutableProperty0<V>, initial: V, end: V) : this(key, initial, end, ::interpolateAny)

	fun set(ratio: Double) = key.set(interpolator(initial, end, ratio))

	override fun toString(): String =
		"V2(key=${key.name}, range=[$initial-$end], startTime=$startTime, duration=$duration)"
}

operator fun <V> KMutableProperty0<V>.get(end: V) = V2(this, this.get(), end, ::interpolateAny)
operator fun <V> KMutableProperty0<V>.get(initial: V, end: V) = V2(this, initial, end, ::interpolateAny)

inline operator fun KMutableProperty0<Double>.get(end: Number) = V2(this, this.get(), end.toDouble(), ::interpolate)
inline operator fun KMutableProperty0<Double>.get(initial: Number, end: Number) =
	V2(this, initial.toDouble(), end.toDouble(), ::interpolate)

@Deprecated("Use get instead", level = DeprecationLevel.ERROR)
operator fun <V> V2<V>.rangeTo(that: V) = this.copy(initial = this.end, end = that)

@Deprecated("Use get instead", ReplaceWith("this[this.get()]"), DeprecationLevel.ERROR)
operator fun <V> KMutableProperty0<V>.rangeTo(that: V) = this[this.get()]

@Deprecated("Use get instead", ReplaceWith("this[that.start, that.endInclusive]"), DeprecationLevel.ERROR)
operator fun <V : Comparable<V>> KMutableProperty0<V>.rangeTo(that: ClosedRange<V>) =
	this[that.start, that.endInclusive]

@Deprecated("Use get instead", ReplaceWith("this[that.first, that.second]"), DeprecationLevel.ERROR)
operator fun <V> KMutableProperty0<V>.rangeTo(that: Pair<V, V>) = this[that.first, that.second]

fun <V> V2<V>.withEasing(easing: Easing): V2<V> =
	this.copy(interpolator = { a, b, ratio -> this.interpolator(a, b, easing(ratio)) })

fun V2<Int>.color(): V2<Int> = this.copy(interpolator = RGBA.Companion::blendRGBA)

fun <V> V2<V>.easing(easing: Easing): V2<V> =
	this.copy(interpolator = { a, b, ratio -> this.interpolator(a, b, easing(ratio)) })

inline fun <V> V2<V>.delay(startTime: TimeSpan) = this.copy(startTime = startTime.milliseconds)
inline fun <V> V2<V>.duration(duration: TimeSpan) = this.copy(duration = duration.milliseconds)

inline fun <V> V2<V>.linear() = this
inline fun <V> V2<V>.easeIn() = this.withEasing(Easings.EASE_IN)
inline fun <V> V2<V>.easeOut() = this.withEasing(Easings.EASE_OUT)
inline fun <V> V2<V>.easeInOut() = this.withEasing(Easings.EASE_IN_OUT)
inline fun <V> V2<V>.easeOutIn() = this.withEasing(Easings.EASE_OUT_IN)
inline fun <V> V2<V>.easeInBack() = this.withEasing(Easings.EASE_IN_BACK)
inline fun <V> V2<V>.easeOutBack() = this.withEasing(Easings.EASE_OUT_BACK)
inline fun <V> V2<V>.easeInOutBack() = this.withEasing(Easings.EASE_IN_OUT_BACK)
inline fun <V> V2<V>.easeOutInBack() = this.withEasing(Easings.EASE_OUT_IN_BACK)

inline fun <V> V2<V>.easeInElastic() = this.withEasing(Easings.EASE_IN_ELASTIC)
inline fun <V> V2<V>.easeOutElastic() = this.withEasing(Easings.EASE_OUT_ELASTIC)
inline fun <V> V2<V>.easeInOutElastic() = this.withEasing(Easings.EASE_IN_OUT_ELASTIC)
inline fun <V> V2<V>.easeOutInElastic() = this.withEasing(Easings.EASE_OUT_IN_ELASTIC)

inline fun <V> V2<V>.easeInBounce() = this.withEasing(Easings.EASE_IN_BOUNCE)
inline fun <V> V2<V>.easeOutBounce() = this.withEasing(Easings.EASE_OUT_BOUNCE)
inline fun <V> V2<V>.easeInOutBounce() = this.withEasing(Easings.EASE_IN_OUT_BOUNCE)
inline fun <V> V2<V>.easeOutInBounce() = this.withEasing(Easings.EASE_OUT_IN_BOUNCE)

inline fun <V> V2<V>.easeInQuad() = this.withEasing(Easings.EASE_IN_QUAD)
inline fun <V> V2<V>.easeOutQuad() = this.withEasing(Easings.EASE_OUT_QUAD)
inline fun <V> V2<V>.easeInOutQuad() = this.withEasing(Easings.EASE_IN_OUT_QUAD)

inline fun <V> V2<V>.easeSine() = this.withEasing(Easings.EASE_SINE)
