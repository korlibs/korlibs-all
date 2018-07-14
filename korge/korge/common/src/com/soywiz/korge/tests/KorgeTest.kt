package com.soywiz.korge.tests

import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.plugin.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.stat.*
import com.soywiz.korge.view.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.event.*
import kotlin.math.*
import kotlin.reflect.*

@Suppress("unused")
open class KorgeTest {
	val plugins = KorgePlugins()
	val eventLoop = EventLoopTest()
	val injector: AsyncInjector = AsyncInjector()
	val ag: AG = DummyAG()
	val input: Input = Input()
	val eventDispatcher = EventDispatcher.Mixin()
	val timeProvider = TimeProvider()
	val stats = Stats()
	val views = Views(eventLoop, ag, injector, input, plugins, timeProvider, stats).apply {
		syncTest {
			init()
		}
	}
	var testTime = 0L
	val canvas = DummyAGContainer(ag)

	fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit {
		sync(el = eventLoop, step = 10, block = block)
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Scene> testScene(
		module: Module,
		sceneClass: KClass<T>,
		vararg injects: Any,
		callback: suspend T.() -> Unit
	) = suspendTest {
		//disableNativeImageLoading {
		val sc = Korge.test(
			Korge.Config(
				module,
				sceneClass = sceneClass,
				sceneInjects = injects.toList(),
				container = canvas,
				eventDispatcher = eventDispatcher,
				timeProvider = TimeProvider {
					//println("Requested Time: $testTime")
					testTime
				})
		)
		callback(sc.currentScene as T)
		//}
	}

	suspend fun Scene.updateTime(dtMs: Int = 20) {
		testTime += dtMs
		views.clampElapsedTimeTo = Int.MAX_VALUE
		//println("updateTime: $dtMs :: $testTime")
		//println("updateTime: $dtMs")
		ag.onRender(ag)
		eventLoop.sleepNextFrame()
		//views.update(dtMs)
	}

	suspend fun Scene.updateTimeSteps(time: Int, step: Int = 20) {
		var remainingTime = time
		while (remainingTime > 0) {
			val elapsed = min(step, remainingTime)
			updateTime(elapsed)
			remainingTime -= elapsed
		}
	}

	suspend fun Scene.updateMousePosition(x: Int, y: Int) {
		eventDispatcher.dispatch(MouseEvent(
			type = MouseEvent.Type.MOVE,
			id = 0,
			x = x,
			y = y
		))
		updateTime(0)
	}

	suspend fun View.simulateClick() {
		this.mouse.onClick(this.mouse)
		ag.onRender(ag)
		eventLoop.sleepNextFrame()
	}

	suspend fun View.simulateOver() {
		this.mouse.onOver(this.mouse)
		ag.onRender(ag)
		eventLoop.sleepNextFrame()
	}

	suspend fun View.simulateOut() {
		this.mouse.onOut(this.mouse)
		ag.onRender(ag)
		eventLoop.sleepNextFrame()
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

	class DummyAG : AG() {
		override val nativeComponent: Any get() = Any()

		init {
			ready()
		}
	}

	class DummyAGContainer(override val ag: AG) : AGContainer {
		override fun repaint(): Unit {
			ag.onRender(ag)
		}
	}
}
