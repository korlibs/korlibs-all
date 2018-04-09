package com.soywiz.korge

import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korag.*
import com.soywiz.korge.input.*
import com.soywiz.korge.plugin.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.time.*
import com.soywiz.korio.vfs.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.ui.*
import kotlin.math.*
import kotlin.reflect.*

object Korge {
	val VERSION = KORGE_VERSION

	val logger = Logger("Korge")

	suspend fun setupCanvas(config: Config): SceneContainer {
		if (config.trace) println("Korge.setupCanvas[1]")
		val injector = config.injector

		val container = config.container!!
		val agInput = container.agInput
		val ag = container.ag
		val size = config.module.size

		injector
			.mapSingleton(Views::class) { Views(get(), get(), get(), get(), get()) }
			.mapSingleton(Input::class) { Input() }
			.mapInstance(KorgePlugins::class, defaultKorgePlugins)
			.mapInstance(Config::class, config)
			.mapInstance(AGContainer::class, container)
			.mapInstance(AG::class, ag)
			.mapPrototype(EmptyScene::class) { EmptyScene() }
			.mapSingleton(ResourcesRoot::class) { ResourcesRoot() }

		if (config.frame != null) {
			injector.mapInstance(Frame::class, config.frame)
		}

		// Register module plugins
		for (plugin in config.module.plugins) {
			defaultKorgePlugins.register(plugin)
		}

		NativeImageSpecialReader.instance.register()
		injector.mapInstance(AG::class, ag)
		if (config.trace) println("Korge.setupCanvas[1b]. EventLoop: ${config.eventLoop}")
		if (config.trace) println("Korge.setupCanvas[1c]. ag: $ag")
		if (config.trace) println("Korge.setupCanvas[1d]. debug: ${config.debug}")
		if (config.trace) println("Korge.setupCanvas[1e]. args: ${config.args.toList()}")
		if (config.trace) println("Korge.setupCanvas[1f]. size: $size")
		injector.mapInstance(EventLoop::class, config.eventLoop)
		val views = injector.get(Views::class)
		val input = views.input
		input._isTouchDeviceGen = { AGFactoryFactory.isTouchDevice }
		views.debugViews = config.debug
		config.constructedViews(views)
		val moduleArgs = ModuleArgs(config.args)
		if (config.trace) println("Korge.setupCanvas[2]")

		views.virtualWidth = size.width
		views.virtualHeight = size.height

		// Inject all modules
		for (plugin in defaultKorgePlugins.plugins) {
			plugin.register(views)
		}

		if (config.trace) println("Korge.setupCanvas[3]")
		ag.onReady.await()

		if (config.trace) println("Korge.setupCanvas[4]")
		injector.mapInstance(ModuleArgs::class, moduleArgs)
		injector.mapInstance(TimeProvider::class, config.timeProvider)
		injector.mapInstance<Module>(Module::class, config.module)
		config.module.init(injector)

		if (config.trace) println("Korge.setupCanvas[5]")

		val downPos = Point2d()
		val upPos = Point2d()
		val mouseMovedEvent = MouseOverEvent()
		val mouseDragEvent = MouseDragEvent()
		val mouseUpEvent = MouseUpEvent()
		val mouseClickEvent = MouseClickEvent()
		val mouseDownEvent = MouseDownEvent()
		val touchEvent = TouchEvent(input.dummyTouch, false, false)

		val keyDownEvent = KeyDownEvent()
		val keyUpEvent = KeyUpEvent()
		val keyTypedEvent = KeyTypedEvent()
		val gamepadTypedEvent = GamepadUpdatedEvent()
		val gamepadConnectionEvent = GamepadConnectionEvent()
		var downTime = 0.0
		var moveTime = 0.0
		var upTime = 0.0
		var moveMouseOutsideInNextFrame = false

		fun mouseDown(name: String, x: Int, y: Int) {
			//Console.log("mouseDown: $name")
			views.input.mouseButtons = 1
			views.input.mouse.setTo(x * ag.pixelDensity, y * ag.pixelDensity)
			views.mouseUpdated()
			downPos.copyFrom(views.input.mouse)
			views.dispatch(mouseDownEvent)
			downTime = Klock.currentTimeMillisDouble()
		}

		fun mouseMove(name: String, x: Int, y: Int) {
			//Console.log("mouseMove: $name")
			views.input.mouse.setTo(x * ag.pixelDensity, y * ag.pixelDensity)
			views.mouseUpdated()
			views.dispatch(mouseMovedEvent)
			moveTime = Klock.currentTimeMillisDouble()
		}

		fun mouseDrag(name: String, x: Int, y: Int) {
			//Console.log("mouseMove: $name")
			views.input.mouse.setTo(x * ag.pixelDensity, y * ag.pixelDensity)
			views.mouseUpdated()
			views.dispatch(mouseDragEvent)
			moveTime = Klock.currentTimeMillisDouble()
		}

		fun mouseUp(name: String, x: Int, y: Int) {
			//Console.log("mouseUp: $name")
			views.input.mouseButtons = 0
			views.input.mouse.setTo(x * ag.pixelDensity, y * ag.pixelDensity)
			views.mouseUpdated()
			upPos.copyFrom(views.input.mouse)
			views.dispatch(mouseUpEvent)
			upTime = Klock.currentTimeMillisDouble()
			if ((downTime - upTime) <= 40.0) {
				//Console.log("mouseClick: $name")
				views.dispatch(mouseClickEvent)
			}
		}

		fun AGInput.KeyEvent.copyTo(e: KeyEvent) {
			e.keyCode = this.keyCode
		}

		fun AGInput.GamepadEvent.copyTo(e: GamepadUpdatedEvent) {
			e.gamepad.copyFrom(this.gamepad)
		}

		fun AGInput.GamepadEvent.copyTo(e: GamepadConnectionEvent) {
			e.gamepad.copyFrom(this.gamepad)
		}

		fun updateTouch(id: Int, x: Int, y: Int, start: Boolean, end: Boolean) {
			val touch = input.getTouch(id)
			val now = Klock.currentTimeMillisDouble()
			val sx = x * ag.pixelDensity
			val sy = y * ag.pixelDensity

			touchEvent.touch = touch
			touchEvent.start = start
			touchEvent.end = end
			touch.id = id
			touch.active = !end

			if (start) {
				touch.startTime = now
				touch.start.setTo(sx, sy)
			}

			touch.currentTime = now
			touch.current.setTo(sx, sy)

			input.updateTouches()
			views.dispatch(touchEvent)
		}

		// MOUSE
		val mouseTouchId = -1
		agInput.onMouseDown { e ->
			mouseDown("onMouseDown", e.x, e.y)
			updateTouch(mouseTouchId, e.x, e.y, start = true, end = false)
		}
		agInput.onMouseUp { e ->
			mouseUp("onMouseUp", e.x, e.y)
			updateTouch(mouseTouchId, e.x, e.y, start = false, end = true)
		}
		agInput.onMouseOver { e -> mouseMove("onMouseOver", e.x, e.y) }
		agInput.onMouseDrag { e ->
			mouseDrag("onMouseDrag", e.x, e.y)
			updateTouch(mouseTouchId, e.x, e.y, start = false, end = false)
		}
		//agInput.onMouseClick { e -> } // Triggered by mouseUp

		// TOUCH
		fun touch(e: AGInput.TouchEvent, start: Boolean, end: Boolean) {
			updateTouch(e.id, e.x, e.y, start, end)
			when {
				start -> {
					mouseDown("onTouchStart", e.x, e.y)
				}
				end -> {
					mouseUp("onTouchEnd", e.x, e.y)
					moveMouseOutsideInNextFrame = true
				}
				else -> {
					mouseDrag("onTouchMove", e.x, e.y)
				}
			}
		}

		agInput.onTouchStart { e -> touch(e, start = true, end = false) }
		agInput.onTouchMove { e -> touch(e, start = false, end = false) }
		agInput.onTouchEnd { e -> touch(e, start = false, end = true) }

		// KEYS
		agInput.onKeyDown {
			views.input.setKey(it.keyCode, true)
			//println("onKeyDown: $it")
			it.copyTo(keyDownEvent)
			views.dispatch(keyDownEvent)
		}
		agInput.onKeyUp {
			views.input.setKey(it.keyCode, false)
			//println("onKeyUp: $it")
			it.copyTo(keyUpEvent)
			views.dispatch(keyUpEvent)

			// DEBUG!
			if (it.keyCode == Keys.F12) {
				views.debugViews = !views.debugViews
			}
		}
		agInput.onKeyTyped {
			//println("onKeyTyped: $it")
			it.copyTo(keyTypedEvent)
			views.dispatch(keyTypedEvent)
		}

		fun gamepadUpdated(gamepad: GamepadInfo) {
			input.gamepads[gamepad.index].copyFrom(gamepad)
			input.updateConnectedGamepads()
		}

		agInput.onGamepadUpdate {
			gamepadUpdated(it.gamepad)
			it.copyTo(gamepadTypedEvent)
			views.dispatch(gamepadTypedEvent)
		}

		agInput.onGamepadConnection {
			gamepadUpdated(it.gamepad)
			it.copyTo(gamepadConnectionEvent)
			views.dispatch(gamepadConnectionEvent)
		}

		ag.onResized {
			//println("ag.onResized: ${ag.backWidth},${ag.backHeight}")
			views.resized(ag.backWidth, ag.backHeight)
		}
		ag.resized()

		var lastTime = config.timeProvider.currentTimeMillis()
		//println("lastTime: $lastTime")
		ag.onRender {
			if (config.trace) println("ag.onRender")
			//println("Render")
			val currentTime = config.timeProvider.currentTimeMillis()
			//println("currentTime: $currentTime")
			val delta = (currentTime - lastTime).toInt()
			val adelta = min(delta, views.clampElapsedTimeTo)
			//println("delta: $delta")
			//println("Render($lastTime -> $currentTime): $delta")
			lastTime = currentTime
			views.update(adelta)
			views.render(
				clear = config.module.clearEachFrame && views.clearEachFrame,
				clearColor = config.module.bgcolor
			)

			//println("Dumping views:")
			//views.dump()

			if (moveMouseOutsideInNextFrame) {
				moveMouseOutsideInNextFrame = false
				views.input.mouse.setTo(-1000, -1000)
				views.dispatch(mouseMovedEvent)
				views.mouseUpdated()
			}
			//println("render:$delta,$adelta")
		}

		if (config.trace) println("Korge.setupCanvas[7]")

		views.animationFrameLoop {
			if (config.trace) println("views.animationFrameLoop")
			//ag.resized()
			config.container.repaint()
		}

		val sc = views.sceneContainer()
		views.stage += sc
		sc.changeTo(config.sceneClass, *config.sceneInjects.toTypedArray(), time = 0.seconds)

		if (config.trace) println("Korge.setupCanvas[8]")

		return sc
	}

	operator fun invoke(
		module: Module,
		args: Array<String> = arrayOf(),
		container: AGContainer? = null,
		sceneClass: KClass<out Scene> = module.mainScene,
		sceneInjects: List<Any> = listOf(),
		timeProvider: TimeProvider = TimeProvider(),
		injector: AsyncInjector = AsyncInjector(),
		debug: Boolean = false,
		trace: Boolean = false,
		constructedViews: (Views) -> Unit = {},
		eventLoop: EventLoop = KoruiEventLoop.instance
	) = EventLoop.main(eventLoop) {
		test(
			Config(
				module = module,
				args = args,
				container = container,
				sceneClass = sceneClass,
				sceneInjects = sceneInjects,
				injector = injector,
				timeProvider = timeProvider,
				debug = debug,
				trace = trace,
				constructedViews = constructedViews
			)
		)
	}

	data class Config(
		val module: Module,
		val args: Array<String> = arrayOf(),
		val container: AGContainer? = null,
		val frame: Frame? = null,
		val sceneClass: KClass<out Scene> = module.mainScene,
		val sceneInjects: List<Any> = listOf(),
		val timeProvider: TimeProvider = TimeProvider(),
		val injector: AsyncInjector = AsyncInjector(),
		val debug: Boolean = false,
		val trace: Boolean = false,
		val constructedViews: (Views) -> Unit = {},
		val eventLoop: EventLoop = KoruiEventLoop.instance
	)

	suspend fun test(config: Config): SceneContainer {
		val done = Promise.Deferred<SceneContainer>()
		if (config.container != null) {
			done.resolve(setupCanvas(config))

		} else {
			val module = config.module
			val icon = try {
				when {
					module.iconImage != null -> {
						module.iconImage!!.render()
					}
					module.icon != null -> {
						ResourcesVfs[module.icon!!].readBitmap()
					}
					else -> {
						null
					}
				}
			} catch (e: Throwable) {
				logger.error { "Couldn't get the application icon" }
				e.printStackTrace()
				null
			}
			CanvasApplicationEx(
				config.module.title,
				config.module.windowSize.width,
				config.module.windowSize.height,
				icon
			) { container, frame ->
				go {
					done.resolve(setupCanvas(config.copy(container = container, frame = frame)))
				}
			}
		}
		return done.promise.await()
	}

	data class ModuleArgs(val args: Array<String>)
}
