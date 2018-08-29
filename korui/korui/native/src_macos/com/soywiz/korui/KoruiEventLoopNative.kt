package com.soywiz.korui

import com.soywiz.korio.async.*
import com.soywiz.korui.light.*
import com.soywiz.korio.*
import com.soywiz.korio.lang.*
import com.soywiz.korag.*
import com.soywiz.korui.event.*
import com.soywiz.std.*
import com.soywiz.kds.*
import com.soywiz.kgl.*
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.Foundation.NSMakeRect
import platform.Foundation.NSMakeSize
import platform.Foundation.NSNotification
import platform.Foundation.NSTimer
import platform.darwin.NSObject
import platform.posix.PATH_MAX
import platform.posix.realpath
import kotlin.reflect.KClass
import com.soywiz.korio.async.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlinx.coroutines.timeunit.*

class MyNativeCoroutineDispatcher() : CoroutineDispatcher(), Delay, Closeable {
	override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

	class TimedTask(val ms: Long, val continuation: CancellableContinuation<Unit>)

	val tasks = Queue<Runnable>()
	val timedTasks = PriorityQueue<TimedTask>(Comparator<TimedTask> { a, b -> a.ms.compareTo(b.ms) })

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		tasks.enqueue(block)
	}

	override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>): Unit {
		val task = TimedTask(Klock.currentTimeMillis() + when (unit) {
			TimeUnit.SECONDS -> time * 1000
			TimeUnit.MILLISECONDS -> time
			else -> error("Unsupported unit $unit")
		}, continuation)
		continuation.invokeOnCancellation {
			timedTasks.remove(task)
		}
		timedTasks.add(task)
	}

	fun executeStep() {
		val now = Klock.currentTimeMillis()
		while (timedTasks.isNotEmpty() && now >= timedTasks.peek().ms) {
			timedTasks.removeHead().continuation.resume(Unit)
		}

		while (tasks.isNotEmpty()) {
			val task = tasks.dequeue()
			task.run()
		}
	}

	override fun close() {

	}

	override fun toString(): String = "MyNativeCoroutineDispatcher"
}

@ThreadLocal
val myNativeCoroutineDispatcher: MyNativeCoroutineDispatcher = MyNativeCoroutineDispatcher()

actual val KoruiDispatcher: CoroutineDispatcher get() = myNativeCoroutineDispatcher

class NativeKoruiContext(
	val ag: AG,
	val light: LightComponents
	//, val app: NSApplication
) : KoruiContext()

class NativeLightComponents(val nkcAg: AG) : LightComponents() {
	val frameHandle = Any()

	override fun create(type: LightType): LightComponentInfo {
		@Suppress("REDUNDANT_ELSE_IN_WHEN")
		val handle: Any = when (type) {
			LightType.FRAME -> frameHandle
			LightType.CONTAINER -> Any()
			LightType.BUTTON -> Any()
			LightType.IMAGE -> Any()
			LightType.PROGRESS -> Any()
			LightType.LABEL -> Any()
			LightType.TEXT_FIELD -> Any()
			LightType.TEXT_AREA -> Any()
			LightType.CHECK_BOX -> Any()
			LightType.SCROLL_PANE -> Any()
			LightType.AGCANVAS -> nkcAg.nativeComponent
			else -> throw UnsupportedOperationException("Type: $type")
		}
		return LightComponentInfo(handle).apply {
			this.ag = nkcAg
		}
	}

	val eds = arrayListOf<Pair<KClass<*>, EventDispatcher>>()

	fun <T : Event> dispatch(clazz: KClass<T>, e: T) {
		for ((eclazz, ed) in eds) {
			if (eclazz == clazz) {
				ed.dispatch(clazz, e)
			}
		}
	}

	inline fun <reified T : Event> dispatch(e: T) = dispatch(T::class, e)

	override fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
		val pair = Pair(clazz, ed)

		if (c === frameHandle || c === nkcAg.nativeComponent) {
			eds += pair
			return Closeable { eds -= pair }
		}

		return DummyCloseable
	}
}

internal actual suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit) {
	val coroutineContext = coroutineContext

	autoreleasepool {
		val app = NSApplication.sharedApplication()
		//val ctx = NativeKoruiContext(ag, app)
		val windowConfig = WindowConfig(640, 480, "Korui")

		val agNativeComponent = Any()
		val ag: AG = AGOpenglFactory.create(agNativeComponent).create(agNativeComponent)
		val light = NativeLightComponents(ag)

		app.delegate = MyAppDelegate(ag, windowConfig, object : MyAppHandler {
			override fun init(context: NSOpenGLContext?) {
				macTrace("init[a]")
				macTrace("init[b]")
				val ctx = NativeKoruiContext(ag, light)
				println("KoruiWrap.pentry[0]")
				ag.__ready()
				//launch(KoruiDispatcher) { // Doesn't work!
				println("KoruiWrap.pentry[1]")
				println("KoruiWrap.entry[0]")
				launch(KoruiDispatcher) {
					entry(ctx)
				}
				println("KoruiWrap.entry[1]")
				//}
				println("KoruiWrap.pentry[2]")
			}

			val mevent = com.soywiz.korui.event.MouseEvent()

			private fun mouseEvent(etype: com.soywiz.korui.event.MouseEvent.Type, ex: Int, ey: Int, ebutton: Int) {
				light.dispatch(mevent.apply {
					this.type = etype
					this.x = ex
					this.y = ey
					this.buttons = 1 shl ebutton
					this.isAltDown = false
					this.isCtrlDown = false
					this.isShiftDown = false
					this.isMetaDown = false
					//this.scaleCoords = false
				})
			}

			override fun mouseUp(x: Int, y: Int, button: Int) {
				mouseEvent(com.soywiz.korui.event.MouseEvent.Type.UP, x, y, button)
				mouseEvent(com.soywiz.korui.event.MouseEvent.Type.CLICK, x, y, button) // @TODO: Conditionally depending on the down x,y & time
			}
			override fun mouseDown(x: Int, y: Int, button: Int) = mouseEvent(com.soywiz.korui.event.MouseEvent.Type.DOWN, x, y, button)
			override fun mouseMoved(x: Int, y: Int) = mouseEvent(com.soywiz.korui.event.MouseEvent.Type.MOVE, x, y, 0)

			fun keyChange(keyCode: Char, pressed: Boolean) {
				println("KEY: $keyCode, ${keyCode.toInt()}, $pressed")
				//listener.keyUpdate(key, pressed)
			}

			override fun keyDown(keyCode: Char) = keyChange(keyCode, true)
			override fun keyUp(keyCode: Char) = keyChange(keyCode, false)

			val resizedEvent = com.soywiz.korui.event.ResizedEvent()
			override fun windowDidResize(width: Int, height: Int, context: NSOpenGLContext?) {
				//macTrace("windowDidResize")
				ag.setViewport(0, 0, width, height)
				light.dispatch(resizedEvent.apply {
					this.width = width
					this.height = height
				})
				render(context)
			}

			override fun render(context: NSOpenGLContext?) {
				//macTrace("render")

				myNativeCoroutineDispatcher.executeStep()

				//context?.flushBuffer()
				context?.makeCurrentContext()
				ag.onRender(ag)
				context?.flushBuffer()
			}
		})
		app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
		app.activateIgnoringOtherApps(true)
		myNativeCoroutineDispatcher.executeStep()
		app.run()
	}
}

/*
actual val KoruiDispatcher: CoroutineDispatcher get() = MyNativeDispatcher

@kotlin.native.ThreadLocal
object MyNativeDispatcher : CoroutineDispatcher(), Delay, DelayFrame {
	val ag: AG = AGOpenglFactory.create(Any()).create(Any())

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		TODO()
	}

	override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
		TODO()
	}

	override fun invokeOnTimeout(time: Long, unit: TimeUnit, block: Runnable): DisposableHandle {
		TODO()
	}

	override fun delayFrame(continuation: CancellableContinuation<Unit>) {
		TODO()
	}

	override fun toString() = "MyNativeDispatcher"
}
*/

/*
// @TOOD: kotlin-native if not ThreadLocal by lazy crashes. And If not by lazy, it crashes in depthFirstTraversal/FreezeSubgraph/initSharedInstance
actual object KoruiEventLoop {
	//actual fun create(): EventLoop = MacosNativeEventLoop()
	actual fun create(): EventLoop = MacosNativeEventLoop
}

actual val KoruiDispatcher: CoroutineDispatcher get() = MyNativeDispatcher

@ThreadLocal
//open class MacosNativeEventLoop : EventLoop() {
object MacosNativeEventLoop : EventLoop() {
	//var app: NSApplication? by atomicRef<NSApplication?>(null)

	override fun loop() {

	}
}
*/

// AGOpenglFactory.create(Any()).create(Any())

class WindowConfig(val width: Int, val height: Int, val title: String)

private class MyAppDelegate(val ag: AG, val windowConfig: WindowConfig, val handler: MyAppHandler) : NSObject(),
	NSApplicationDelegateProtocol {
	val mainDisplayRect = NSScreen.mainScreen()!!.frame
	val windowRect = mainDisplayRect.useContents<CGRect, CValue<CGRect>> {
		NSMakeRect(
			(size.width * 0.5 - windowConfig.width * 0.5),
			(size.height * 0.5 - windowConfig.height * 0.5),
			windowConfig.width.toDouble(),
			windowConfig.height.toDouble()
		)
	}

	val windowStyle = NSWindowStyleMaskTitled or NSWindowStyleMaskMiniaturizable or
			NSWindowStyleMaskClosable or NSWindowStyleMaskResizable

	val attrs = intArrayOf(
		//NSOpenGLPFAOpenGLProfile,
		//NSOpenGLProfileVersion4_1Core,
		NSOpenGLPFAColorSize, 24,
		NSOpenGLPFAAlphaSize, 8,
		NSOpenGLPFADoubleBuffer,
		NSOpenGLPFADepthSize, 32,
		0
	)

	val pixelFormat = attrs.usePinned {
		NSOpenGLPixelFormat.alloc()!!.initWithAttributes(it.addressOf(0).uncheckedCast())!!
	}

	private val openglView: NSOpenGLView = NSOpenGLView(NSMakeRect(0.0, 0.0, 16.0, 16.0), pixelFormat)
	private val appDelegate: AppDelegate = AppDelegate(handler, openglView, openglView?.openGLContext)

	private val window: NSWindow = NSWindow(windowRect, windowStyle, NSBackingStoreBuffered, false).apply {
		title = windowConfig.title
		opaque = true
		hasShadow = true
		preferredBackingLocation = NSWindowBackingLocationVideoMemory
		hidesOnDeactivate = false
		releasedWhenClosed = false

		openglView.setFrame(contentRectForFrameRect(frame))
		delegate = appDelegate

		setAcceptsMouseMovedEvents(true)
		setContentView(openglView)
		setContentMinSize(NSMakeSize(150.0, 100.0))
		//openglView.resignFirstResponder()
		openglView.setNextResponder(MyResponder(handler, openglView))
		//makeFirstResponder(MyResponder(handler, openglView))
		setNextResponder(MyResponder(handler, openglView))
	}
	//private val openglView: AppNSOpenGLView

	override fun applicationShouldTerminateAfterLastWindowClosed(app: NSApplication): Boolean {
		println("applicationShouldTerminateAfterLastWindowClosed")
		return true
	}

	override fun applicationWillFinishLaunching(notification: NSNotification) {
		println("applicationWillFinishLaunching")
		window.makeKeyAndOrderFront(this)
	}

	override fun applicationDidFinishLaunching(notification: NSNotification) {
		//val data = decodeImageData(readBytes("icon.jpg"))
		//println("${data.width}, ${data.height}")

		openglView.openGLContext?.makeCurrentContext()
		try {
			handler.init(openglView.openGLContext)
			handler.render(openglView.openGLContext)
			appDelegate.timer = NSTimer.scheduledTimerWithTimeInterval(1.0 / 60.0, true, ::timer)
		} catch (e: Throwable) {
			e.printStackTrace()
			window.close()
		}
	}


	private fun timer(timer: NSTimer?) {
		//println("TIMER")
		handler.render(openglView?.openGLContext)
	}

	override fun applicationWillTerminate(notification: NSNotification) {
		println("applicationWillTerminate")
		// Insert code here to tear down your application

	}
}

class MyResponder(val handler: MyAppHandler, val openGLView: NSOpenGLView) : NSResponder() {
	override fun acceptsFirstResponder(): Boolean {
		return true
	}

	fun getHeight(): Int = openGLView.bounds.useContents<CGRect, Int> { size.height.toInt() }

	override fun mouseUp(event: NSEvent) {
		super.mouseUp(event)
		event.locationInWindow.useContents<CGPoint, Unit> {
			val rx = x.toInt()
			val ry = getHeight() - y.toInt()
			//println("mouseUp($rx,$ry)")
			handler.mouseUp(rx, ry, event.buttonNumber.toInt())
		}
	}

	override fun mouseDown(event: NSEvent) {
		super.mouseDown(event)
		event.locationInWindow.useContents<CGPoint, Unit> {
			val rx = x.toInt()
			val ry = getHeight() - y.toInt()
			//println("mouseDown($rx,$ry)")
			handler.mouseDown(rx, ry, event.buttonNumber.toInt())
		}
	}

	override fun mouseMoved(event: NSEvent) {
		super.mouseMoved(event)
		event.locationInWindow.useContents<CGPoint, Unit> {
			val rx = x.toInt()
			val ry = getHeight() - y.toInt()
			//println("mouseMoved($rx,$ry)")
			handler.mouseMoved(rx, ry)
		}
	}

	override fun mouseDragged(event: NSEvent) {
		super.mouseDragged(event)
		event.locationInWindow.useContents<CGPoint, Unit> {
			val rx = x.toInt()
			val ry = getHeight() - y.toInt()
			//println("mouseDragged($rx,$ry)")
			handler.mouseMoved(rx, ry)
		}
	}

	fun keyDownUp(event: NSEvent, pressed: Boolean) {
		val str = event.charactersIgnoringModifiers ?: "\u0000"
		val c = str.getOrNull(0) ?: '\u0000'
		val cc = c.toInt().toChar()
		//println("keyDownUp")
		if (pressed) {
			handler.keyDown(cc)
		} else {
			handler.keyUp(cc)
		}
	}

	override fun keyDown(event: NSEvent) {
		super.keyDown(event)
		keyDownUp(event, true)
	}

	override fun keyUp(event: NSEvent) {
		super.keyUp(event)
		keyDownUp(event, false)
	}
}

fun macTrace(str: String) {
	println(str)
}

interface MyAppHandler {
	fun init(context: NSOpenGLContext?)
	fun mouseUp(x: Int, y: Int, button: Int)
	fun mouseDown(x: Int, y: Int, button: Int)
	fun mouseMoved(x: Int, y: Int)
	fun keyDown(keyCode: Char)
	fun keyUp(keyCode: Char)
	fun windowDidResize(width: Int, height: Int, context: NSOpenGLContext?)
	fun render(context: NSOpenGLContext?)
}

class AppDelegate(
	val handler: MyAppHandler,
	val openGLView: NSOpenGLView,
	var openGLContext: NSOpenGLContext? = null
) : NSObject(), NSWindowDelegateProtocol {
	var timer: NSTimer? = null

	override fun windowShouldClose(sender: NSWindow): Boolean {
		println("windowShouldClose")
		return true
	}

	override fun windowWillClose(notification: NSNotification) {
		println("windowWillClose")
		//openGLContext = null
//
		//timer?.invalidate()
		//timer = null
//
		//NSApplication.sharedApplication().stop(this)
	}

	override fun windowDidResize(notification: NSNotification) {
		println("windowDidResize")
		openGLView.bounds.useContents<CGRect, Unit> {
			val bounds = this
			handler.windowDidResize(bounds.size.width.toInt(), bounds.size.height.toInt(), openGLContext)
			Unit
		}
	}
}
