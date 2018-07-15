package com.soywiz.korge

import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korag.*
import com.soywiz.korge.input.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.stat.*
import com.soywiz.korge.view.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korinject.*
import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.event.*
import com.soywiz.korui.input.*
import com.soywiz.korui.ui.*
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.*
import kotlin.reflect.*

object Korge {
	val logger = Logger("Korge")

	suspend fun setupCanvas(config: Config): SceneContainer {
		logger.trace { "Korge.setupCanvas[1]" }
		val injector = config.injector
		val frame = config.frame

		val eventDispatcher = config.eventDispatcher
		val agContainer = config.container ?: error("No agContainer defined")
		val ag = agContainer.ag
		val size = config.module.size
		val moduleArgs = ModuleArgs(config.args)

		logger.trace { "pre injector" }
		injector
			// Instances
			.mapInstance(ModuleArgs::class, moduleArgs)
			.mapInstance(TimeProvider::class, config.timeProvider)
			.mapInstance(CoroutineContext::class, config.context)
			.mapInstance(Module::class, config.module)
			.mapInstance(AG::class, ag)
			.mapInstance(Config::class, config)
			// Singletons
			.mapSingleton(Stats::class) { Stats() }
			.mapSingleton(Input::class) { Input() }
			.mapSingleton(Views::class) { Views(get(), get(), get(), get(), get(), get()) }
			.mapSingleton(ResourcesRoot::class) { ResourcesRoot() }
			// Prototypes
			.mapPrototype(EmptyScene::class) { EmptyScene() }

		if (config.frame != null) {
			injector.mapInstance(Frame::class, config.frame)
			injector.mapInstance(Application::class, config.frame.app)
		}

		//println("FRAME: $frame, ${config.frame}")

		@Suppress("RemoveExplicitTypeArguments")

		ag.onReady.await()

		logger.trace { "Korge.setupCanvas[1c]. ag: $ag" }
		logger.trace { "Korge.setupCanvas[1d]. debug: ${config.debug}" }
		logger.trace { "Korge.setupCanvas[1e]. args: ${config.args.toList()}" }
		logger.trace { "Korge.setupCanvas[1f]. size: $size" }
		logger.trace { "Korge.setupCanvas[1g]" }
		val views = injector.get(Views::class)
		logger.trace { "Korge.setupCanvas[1h]" }
		val input = views.input
		logger.trace { "Korge.setupCanvas[1i]" }
		input._isTouchDeviceGen = { AGOpenglFactory.isTouchDevice }
		logger.trace { "Korge.setupCanvas[1j]" }
		views.debugViews = config.debug
		logger.trace { "Korge.setupCanvas[1k]" }
		config.constructedViews(views)
		logger.trace { "Korge.setupCanvas[1l]" }
		logger.trace { "Korge.setupCanvas[2]" }

		views.virtualWidth = size.width
		views.virtualHeight = size.height
		views.scaleAnchor = config.module.scaleAnchor
		views.scaleMode = config.module.scaleMode
		views.clipBorders = config.module.clipBorders

		logger.trace { "Korge.setupCanvas[3]" }

		logger.trace { "Korge.setupCanvas[4]" }
		config.module.init(injector)

		logger.trace { "Korge.setupCanvas[5]" }

		val downPos = MPoint2d()
		val upPos = MPoint2d()
		var downTime = 0.0
		var moveTime = 0.0
		var upTime = 0.0
		var moveMouseOutsideInNextFrame = false
		val mouseTouchId = -1

		/*

		fun AGInput.GamepadEvent.copyTo(e: GamepadUpdatedEvent) {
			e.gamepad.copyFrom(this.gamepad)
		}

		fun AGInput.GamepadEvent.copyTo(e: GamepadConnectionEvent) {
			e.gamepad.copyFrom(this.gamepad)
		}



		// MOUSE
		agInput.onMouseDown { e ->
			mouseDown("onMouseDown", e.x, e.y)
		}
		agInput.onMouseUp { e ->
			mouseUp("onMouseUp", e.x, e.y)
		}
		agInput.onMouseOver { e -> mouseMove("onMouseOver", e.x, e.y) }
		agInput.onMouseDrag { e ->
			mouseDrag("onMouseDrag", e.x, e.y)
			updateTouch(mouseTouchId, e.x, e.y, start = false, end = false)
		}
		//agInput.onMouseClick { e -> } // Triggered by mouseUp
		*/

		fun updateTouch(id: Int, x: Double, y: Double, start: Boolean, end: Boolean) {
			val touch = input.getTouch(id)
			val now = Klock.currentTimeMillisDouble()
			val sx = x * ag.devicePixelRatio
			val sy = y * ag.devicePixelRatio

			touch.id = id
			touch.active = !end

			if (start) {
				touch.startTime = now
				touch.start.setTo(sx, sy)
			}

			touch.currentTime = now
			touch.current.setTo(sx, sy)

			input.updateTouches()
		}

		fun mouseDown(type: String, x: Double, y: Double) {
			views.input.mouseButtons = 1
			views.input.mouse.setTo(x, y)
			views.mouseUpdated()
			downPos.copyFrom(views.input.mouse)
			downTime = Klock.currentTimeMillisDouble()
		}

		fun mouseUp(type: String, x: Double, y: Double) {
			//Console.log("mouseUp: $name")
			views.input.mouseButtons = 0
			views.input.mouse.setTo(x, y)
			views.mouseUpdated()
			upPos.copyFrom(views.input.mouse)

			if (type == "onTouchEnd") {
				upTime = Klock.currentTimeMillisDouble()
				if ((downTime - upTime) <= 40.0) {
					//Console.log("mouseClick: $name")
					views.dispatch(MouseEvent(MouseEvent.Type.CLICK))
				}
			}
		}

		fun mouseDrag(type: String, x: Double, y: Double) {
			views.input.mouse.setTo(x, y)
			views.mouseUpdated()
			moveTime = Klock.currentTimeMillisDouble()
		}

		fun mouseMove(type: String, x: Double, y: Double) {
			views.input.mouse.setTo(x, y)
			views.mouseUpdated()
			moveTime = Klock.currentTimeMillisDouble()
		}

		eventDispatcher.addEventListener<MouseEvent> { e ->
			logger.trace { "eventDispatcher.addEventListener<MouseEvent>:$e" }
			val x = e.x.toDouble()
			val y = e.y.toDouble()
			when (e.type) {
				MouseEvent.Type.DOWN -> {
					mouseDown("mouseDown", x, y)
					updateTouch(mouseTouchId, x, y, start = true, end = false)
				}
				MouseEvent.Type.UP -> {
					mouseUp("mouseUp", x, y)
					updateTouch(mouseTouchId, x, y, start = false, end = true)
				}
				MouseEvent.Type.MOVE -> {
					mouseDrag("mouseMove", x, y)
				}
				MouseEvent.Type.DRAG -> {
					mouseMove("onMouseDrag", x, y)
					updateTouch(mouseTouchId, x, y, start = false, end = false)
				}
			}
			views.dispatch(e)
		}

		eventDispatcher.addEventListener<KeyEvent> { e ->
			logger.trace { "eventDispatcher.addEventListener<KeyEvent>:$e" }
			when (e.type) {
				KeyEvent.Type.DOWN -> {
					views.input.setKey(e.keyCode, true)
				}
				KeyEvent.Type.UP -> {
					views.input.setKey(e.keyCode, false)

					if (e.key == Key.F12) {
						views.debugViews = !views.debugViews
					}
				}
				KeyEvent.Type.TYPE -> {
					//println("onKeyTyped: $it")
				}
			}
			views.dispatch(e)
		}


		// TOUCH
		fun touch(e: TouchEvent, start: Boolean, end: Boolean) {
			val t = e.touch
			val x = t.current.x
			val y = t.current.y
			updateTouch(t.id, x, y, start, end)
			when {
				start -> {
					mouseDown("onTouchStart", x, y)
				}
				end -> {
					mouseUp("onTouchEnd", x, y)
					moveMouseOutsideInNextFrame = true
				}
				else -> {
					mouseDrag("onTouchMove", x, y)
				}
			}
		}

		eventDispatcher.addEventListener<TouchEvent> { e ->
			logger.trace { "eventDispatcher.addEventListener<TouchEvent>:$e" }
			val ix = e.touch.current.x.toInt()
			val iy = e.touch.current.y.toInt()
			when (e.type) {
				TouchEvent.Type.START -> {
					touch(e, start = true, end = false)
					views.dispatch(MouseEvent(MouseEvent.Type.DOWN, 0, ix, iy, MouseButton.LEFT, 1))
				}
				TouchEvent.Type.MOVE -> {
					touch(e, start = false, end = false)
					views.dispatch(MouseEvent(MouseEvent.Type.DRAG, 0, ix, iy, MouseButton.LEFT, 1))
				}
				TouchEvent.Type.END -> {
					touch(e, start = false, end = true)
					views.dispatch(MouseEvent(MouseEvent.Type.UP, 0, ix, iy, MouseButton.LEFT, 0))
					//println("DISPATCH MouseEvent(MouseEvent.Type.UP)")
				}
			}
			views.dispatch(e)
		}

		fun gamepadUpdated(gamepad: GamepadInfo) {
			input.gamepads[gamepad.index].copyFrom(gamepad)
			input.updateConnectedGamepads()
		}

		//agInput.onGamepadUpdate {
		//	gamepadUpdated(it.gamepad)
		//	it.copyTo(gamepadTypedEvent)
		//	views.dispatch(gamepadTypedEvent)
		//}

		eventDispatcher.addEventListener<GamePadButtonEvent> { e ->
			logger.trace { "eventDispatcher.addEventListener<GamePadButtonEvent>:$e" }
		}

		eventDispatcher.addEventListener<GamePadStickEvent> { e ->
			logger.trace { "eventDispatcher.addEventListener<GamePadStickEvent>:$e" }
		}

		eventDispatcher.addEventListener<GamePadConnectionEvent> { e ->
			logger.trace { "eventDispatcher.addEventListener<GamePadConnectionEvent>:$e" }
			//gamepadUpdated(it.gamepad)
			//it.copyTo(gamepadConnectionEvent)
			//views.dispatch(gamepadConnectionEvent)
		}

		eventDispatcher.addEventListener<ResizedEvent> { e ->
			//println("eventDispatcher.addEventListener<ResizedEvent>:$e - backSize=(${ag.backWidth}, ${ag.backHeight}) :: frame=(${frame?.actualWidth}x${frame?.actualHeight}) :: frame=(${frame?.computedWidth}x${frame?.computedHeight})")
			views.resized(ag.backWidth, ag.backHeight)
		}

		ag.onResized { e ->
			//println("ag.onResized:$e - backSize=(${ag.backWidth}, ${ag.backHeight}) :: ${agContainer.ag} :: frame=(${frame?.width}x${frame?.height})")
			//println("ag.onResized: ${ag.backWidth},${ag.backHeight}")
			views.resized(ag.backWidth, ag.backHeight)
		}
		ag.resized()
		eventDispatcher.dispatch(ResizedEvent(100, 100))

		//println("lastTime: $lastTime")
		ag.onRender {
			views.frameUpdateAndRender(
				clear = config.module.clearEachFrame && views.clearEachFrame,
				clearColor = config.module.bgcolor
			)

			if (moveMouseOutsideInNextFrame) {
				moveMouseOutsideInNextFrame = false
				views.input.mouse.setTo(-1000, -1000)
				//views.dispatch(mouseMovedEvent)
				views.mouseUpdated()
			}
			//println("render:$delta,$adelta")
		}

		logger.trace { "Korge.setupCanvas[7]" }

		views.targetFps = config.module.targetFps

		coroutineContext.animationFrameLoop {
			logger.trace { "views.animationFrameLoop" }
			//ag.resized()
			config.container.repaint()
		}

		val sc = views.sceneContainer()
		views.stage += sc

		register(views)

		sc.changeTo(config.sceneClass, *config.sceneInjects.toTypedArray(), time = 0.seconds)

		logger.trace { "Korge.setupCanvas[8]" }

		return sc
	}

	operator fun invoke(
		module: Module,
		args: Array<String> = arrayOf(),
		container: AGContainer? = null,
		eventDispatcher: EventDispatcher = DummyEventDispatcher,
		sceneClass: KClass<out Scene> = module.mainScene,
		sceneInjects: List<Any> = listOf(),
		timeProvider: TimeProvider = TimeProvider(),
		injector: AsyncInjector = AsyncInjector(),
		debug: Boolean = false,
		trace: Boolean = false,
		constructedViews: (Views) -> Unit = {},
		context: CoroutineContext = KoruiDispatcher
	) = KorioNative.asyncEntryPoint(context) {
		logger.trace { "Korge.invoke" }
		test(
			Config(
				module = module,
				args = args,
				container = container,
				eventDispatcher = eventDispatcher,
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
		val eventDispatcher: EventDispatcher = DummyEventDispatcher,
		val imageFormats: ImageFormats = defaultImageFormats,
		val frame: Frame? = null,
		val sceneClass: KClass<out Scene> = module.mainScene,
		val sceneInjects: List<Any> = listOf(),
		val timeProvider: TimeProvider = TimeProvider(),
		val injector: AsyncInjector = AsyncInjector(),
		val debug: Boolean = false,
		val trace: Boolean = false,
		val constructedViews: (Views) -> Unit = {},
		val context: CoroutineContext = KoruiDispatcher
	)

	suspend fun test(config: Config): SceneContainer {
		if (OS.isJvm) {
			logger.trace { "!!!! KORGE: if the main window doesn't appear and hangs, check that the VM option -XstartOnFirstThread is set" }
		}
		logger.trace { "Korge.test" }
		logger.trace { "Korge.test.checkEnvironment" }
		val done = CompletableDeferred<SceneContainer>(Job())
		logger.trace { "Korge.test without container" }
		val module = config.module
		logger.trace { "Korge.test loading icon" }
		val icon = try {
			when {
				module.iconImage != null -> {
					module.iconImage!!.render()
				}
				module.icon != null -> {
					ResourcesVfs[module.icon!!].readBitmapOptimized(config.imageFormats)
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

		logger.trace { "Korge.test pre CanvasApplicationEx" }
		CanvasApplicationEx(
			title = module.title,
			width = module.windowSize.width,
			height = module.windowSize.height,
			icon = icon,
			quality = module.quality
		) { container, frame ->
			logger.trace { "Korge.test [1]" }
			launch(coroutineContext) {
				logger.trace { "Korge.test [2]" }
				done.complete(
					setupCanvas(
						config.copy(
							container = container,
							frame = frame,
							eventDispatcher = container
						)
					)
				)
			}
		}

		return done.await()
	}

	data class ModuleArgs(val args: Array<String>)
}

expect fun Korge.register(views: Views)
