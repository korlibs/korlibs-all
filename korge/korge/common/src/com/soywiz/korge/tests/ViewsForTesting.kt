package com.soywiz.korge.tests

import com.soywiz.korag.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.event.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.timeunit.*

open class ViewsForTesting(val frameTime: Int = 10) {
	val testDispatcher = TestCoroutineDispatcher(frameTime)
	val viewsLog = ViewsLog(testDispatcher)
	val injector get() = viewsLog.injector
	val ag get() = viewsLog.ag
	val input get() = viewsLog.input
	val views get() = viewsLog.views
	val eventDispatcher = EventDispatcher.Mixin()
	val timeProvider get() = views.timeProvider
	val stats get() = views.stats
	val canvas = DummyAGContainer(ag)

	suspend fun mouseMoveTo(x: Number, y: Number, ms: Int = 10) {
		input.mouse.setTo(x, y)
		delay(ms)
	}

	//@Suppress("UNCHECKED_CAST")
	//fun <T : Scene> testScene(
	//	module: Module,
	//	sceneClass: KClass<T>,
	//	vararg injects: Any,
	//	callback: suspend T.() -> Unit
	//) = viewsTest {
	//	//disableNativeImageLoading {
	//	val sc = Korge.test(
	//		Korge.Config(
	//			module,
	//			sceneClass = sceneClass,
	//			sceneInjects = injects.toList(),
	//			container = canvas,
	//			eventDispatcher = eventDispatcher,
	//			timeProvider = TimeProvider { testDispatcher.time })
	//	)
	//	callback(sc.currentScene as T)
	//	//}
	//}

	suspend fun Scene.updateMousePosition(x: Int, y: Int) {
		eventDispatcher.dispatch(
			MouseEvent(
				type = MouseEvent.Type.MOVE,
				id = 0,
				x = x,
				y = y
			)
		)
		delay(0)
	}

	suspend fun View.simulateClick() {
		this.mouse.onClick(this.mouse)
		ag.onRender(ag)
		delayNextFrame()
	}

	suspend fun View.simulateOver() {
		this.mouse.onOver(this.mouse)
		ag.onRender(ag)
		delayNextFrame()
	}

	suspend fun View.simulateOut() {
		this.mouse.onOut(this.mouse)
		ag.onRender(ag)
		delayNextFrame()
	}

	suspend fun View.isVisibleToUser(): Boolean {
		if (!this.visible) return false
		if (this.alpha <= 0.0) return false
		val bounds = this.getGlobalBounds()
		if (bounds.area <= 0.0) return false
		val module = injector.get<Module>()
		val visibleBounds = Rectangle(0, 0, module.windowSize.width, module.windowSize.height)
		if (!bounds.intersects(visibleBounds)) return false
		return true
	}

	class DummyAGContainer(override val ag: AG) : AGContainer {
		override fun repaint(): Unit {
			ag.onRender(ag)
		}
	}

	// @TODO: Run a faster eventLoop where timers happen much faster
	fun viewsTest(block: suspend () -> Unit) {
		val context = KorioDefaultDispatcher
		//val context = TestCoroutineDispatcher()
		Korio(context) {
			val el = context.animationFrameLoop {
				views.update(frameTime)
			}
			val bb = async(context) {
				withTimeout(10, TimeUnit.SECONDS) {
					block()
				}
			}
			try {
				bb.await()
			} finally {
				el.close()
			}
		}
	}
}
