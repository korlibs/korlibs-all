package com.soywiz.korui

import com.soywiz.korio.async.*
import com.soywiz.korui.light.*
import com.soywiz.korio.*
import com.soywiz.korag.*
import com.soywiz.std.*
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
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KClass
import com.soywiz.korio.async.*
import konan.worker.*
import konan.worker.atomicLazy

//fun <T> T.ensureNeverFrozen(): T = this.apply { ensureNeverFrozen() }

// @TOOD: kotlin-native if not ThreadLocal by lazy crashes. And If not by lazy, it crashes in depthFirstTraversal/FreezeSubgraph/initSharedInstance
actual object KoruiEventLoop {
	actual fun create(): EventLoop = MacosNativeEventLoop().apply { ensureNeverFrozen() }
	//actual fun create(): EventLoop = MacosNativeEventLoop
}

//@ThreadLocal
open class MacosNativeEventLoop : EventLoop() {
	init {
		ensureNeverFrozen()
	}
//object MacosNativeEventLoop : EventLoop() {
	var app: NSApplication? by atomicRef<NSApplication?>(null)

	val ag: AG by atomicLazy { AGOpenglFactory.create(this).create(this) }
	//val ag: AG by atomicLazy { AGOpenglFactory.create(this).create(this) }
	//val ag: AG = AGOpenglFactory.create(this).create(this)
	var listener = object : KMLWindowListener() {
	}
	//val listener = KMLWindowListener()

	override fun loop() {
		autoreleasepool {
			//val app = NSApplication.sharedApplication()
			app = NSApplication.sharedApplication()
			val windowConfig = WindowConfig(640, 480, "Korui")
			app?.delegate = MyAppDelegate(ag, windowConfig, object : MyAppHandler {
				override fun init(context: NSOpenGLContext?) {
					macTrace("init[a]")
					//runInitBlocking(listener)
				}

				override fun mouseUp(x: Int, y: Int, button: Int) = listener.mouseUpdateButton(button, false)
				override fun mouseDown(x: Int, y: Int, button: Int) = listener.mouseUpdateButton(button, true)
				override fun mouseMoved(x: Int, y: Int) = listener.mouseUpdateMove(x, y)

				fun keyChange(keyCode: Char, pressed: Boolean) {
					println("KEY: $keyCode, ${keyCode.toInt()}, $pressed")
					//listener.keyUpdate(key, pressed)
				}

				override fun keyDown(keyCode: Char) = keyChange(keyCode, true)
				override fun keyUp(keyCode: Char) = keyChange(keyCode, false)

				override fun windowDidResize(width: Int, height: Int, context: NSOpenGLContext?) {
					macTrace("windowDidResize")
					listener.resized(width, height)
					render(context)
				}

				override fun render(context: NSOpenGLContext?) {
					macTrace("render")
					step()
					//context?.flushBuffer()
					context?.makeCurrentContext()
					ag.onRender(ag)
					context?.flushBuffer()
				}
			})
			app?.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
			app?.activateIgnoringOtherApps(true)
			app?.run()
		}
	}
}

class WindowConfig(val width: Int, val height: Int, val title: String)

private class MyAppDelegate(val ag: AG, val windowConfig: WindowConfig, val handler: MyAppHandler) : NSObject(), NSApplicationDelegateProtocol {
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


open class KMLWindowListener {
	open suspend fun init(gl: KmlGl): Unit = gl.run {
	}

	open fun render(gl: KmlGl): Unit = gl.run {
	}

	open fun keyUpdate(key: Int, pressed: Boolean) {
	}

	open fun gamepadConnection(player: Int, name: String, connected: Boolean) {
	}

	open fun gamepadButtonUpdate(player: Int, button: Int, ratio: Double) {
	}

	open fun gamepadStickUpdate(player: Int, stick: Int, x: Double, y: Double) {
	}

	open fun mouseUpdateMove(x: Int, y: Int) {
	}

	open fun mouseUpdateButton(button: Int, pressed: Boolean) {
	}

	open fun resized(width: Int, height: Int) {
	}
}