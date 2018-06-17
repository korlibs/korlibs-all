package com.soywiz.kmedialayer

import kotlinx.cinterop.*
import kotlinx.cinterop.ByteVar
import platform.AppKit.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.OpenGL.*
import platform.osx.*
import platform.posix.*

//fun main(args: Array<String>) {
//    runApp(MyAppHandler())
//}
actual val Kml: KmlBase = KmlBaseNativeMacos
private val glNative: KmlGlNative by lazy { KmlGlNative() }

object KmlBaseNativeMacos : KmlBaseNoEventLoop() {
	override fun application(windowConfig: WindowConfig, listener: KMLWindowListener) {
		runApp(object : MyAppHandler() {
			override fun init(context: NSOpenGLContext?) {
				macTrace("init[a]")
				runInitBlocking(listener)
				macTrace("init[b]")
			}

			override fun mouseUp(x: Int, y: Int, button: Int) = listener.mouseUpdateButton(button, false)
			override fun mouseDown(x: Int, y: Int, button: Int) = listener.mouseUpdateButton(button, true)
			override fun mouseMoved(x: Int, y: Int) = listener.mouseUpdateMove(x, y)

			fun keyChange(keyCode: Char, pressed: Boolean) {
				val key = KEYS[keyCode] ?: Key.UNKNOWN
				println("KEY: $keyCode, ${keyCode.toInt()}, ${keyCode.toInt().toHexString()}, $key, $pressed")
				listener.keyUpdate(key, pressed)
			}

			override fun keyDown(keyCode: Char) = keyChange(keyCode, true)
			override fun keyUp(keyCode: Char) = keyChange(keyCode, false)

			override fun windowDidResize(width: Int, height: Int, context: NSOpenGLContext?) {
				macTrace("resize[a]")
				glNative.viewport(0, 0, width, height)
				macTrace("resize[b]")
				listener.resized(width, height)
				macTrace("resize[b]")
				//println("RESIZED($width, $height)")
				//render(context)
				listener.render(glNative)
				macTrace("resize[d]")
				context?.flushBuffer()
				macTrace("resize[e]")
			}

			override fun render(context: NSOpenGLContext?) {
				//println("FRAME")
				//glClearColor(0.2f, 0.4f, 0.6f, 1f)
				//glClear(GL_COLOR_BUFFER_BIT)
				macTrace("render[a]")
				listener.render(glNative)
				macTrace("render[b]")
				context?.flushBuffer()
				macTrace("render[c]")
			}
		}, windowConfig)
	}

	fun runInitBlocking(listener: KMLWindowListener) {
		runBlocking {
			listener.init(glNative)
		}
	}

	override fun currentTimeMillis(): Double = kotlin.system.getTimeMillis().toDouble()

	override fun sleep(time: Int) {
		usleep(time * 1000)
	}

	override fun pollEvents() {
	}

	override suspend fun decodeImage(data: ByteArray): KmlNativeImageData {
		return decodeImageSync(data)
	}

	fun decodeImageSync(data: ByteArray): KmlNativeImageData {
		macTrace("decodeImageSync[a]")
		return autoreleasepool {
			macTrace("decodeImageSync[b]")
			val nsdata = data.usePinned { dataPin ->
				NSData.alloc()!!.initWithBytes(dataPin.addressOf(0), data.size.toLong())
			}
			macTrace("decodeImageSync[c]")
			val image = NSImage.alloc()!!.initWithData(nsdata)!!
			var iwidth = 0
			var iheight = 0
			val imageSize = image.size
			macTrace("decodeImageSync[d]")
			imageSize.useContents { iwidth = width.toInt(); iheight = height.toInt() }
			val imageRect = NSMakeRect(0.0, 0.0, iwidth.toDouble(), iheight.toDouble())
			macTrace("decodeImageSync[e]")
			val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB)
			macTrace("decodeImageSync[f]")
			macTrace("size($iwidth, $iheight)")
			val ctx = CGBitmapContextCreate(
				null, iwidth.toLong(), iheight.toLong(),
				//8, iwidth.toLong() * 4, colorSpace, CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
				8, 0, colorSpace, CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
			)
			macTrace("decodeImageSync[g]")
			val gctx = NSGraphicsContext.graphicsContextWithCGContext(ctx, flipped = false)
			macTrace("decodeImageSync[h]")

			NSGraphicsContext.setCurrentContext(gctx)
			image.drawInRect(imageRect)
			macTrace("decodeImageSync[i]")

			val width = CGBitmapContextGetWidth(ctx).toInt()
			val height = CGBitmapContextGetHeight(ctx).toInt()
			val pixels = CGBitmapContextGetData(ctx)?.reinterpret<IntVar>()
			val out = IntArray(width * height)
			macTrace("decodeImageSync[j]")
			out.usePinned { outPin ->
				memcpy(outPin.addressOf(0), pixels, (out.size.toLong() * 4).narrow())
			}
			macTrace("decodeImageSync[k]")
			val bytes = pixels!!.readBytes((width * height * 4).toInt())
			val idata = KmlNativeNativeImageData(width.toInt(), height.toInt(), out)
			macTrace("decodeImageSync[l]")

			NSGraphicsContext.setCurrentContext(null)
			CGContextRelease(ctx)
			CGColorSpaceRelease(colorSpace)
			macTrace("decodeImageSync[m]")
			idata
		}
	}

	override suspend fun loadFileBytes(path: String, range: LongRange?): ByteArray {
		return loadFileBytesSync(path, range)
	}

	override suspend fun writeFileBytes(path: String, data: ByteArray, offset: Long?): Unit {
		TODO("KmlBase.writeFileBytes")
	}
}


open class MyAppHandler {
	open fun init(context: NSOpenGLContext?) {

	}

	open fun mouseUp(x: Int, y: Int, button: Int) {
		println("MOUSE_UP $x, $y: BUTTON: $button")
	}

	open fun mouseDown(x: Int, y: Int, button: Int) {
		println("MOUSE_DOWN $x, $y: BUTTON: $button")
	}

	open fun mouseMoved(x: Int, y: Int) {
		println("MOUSE_MOVED $x, $y")
	}

	open fun keyDown(keyCode: Char) {
		println("KEY_DOWN: $keyCode")
	}

	open fun keyUp(keyCode: Char) {
		println("KEY_UP: $keyCode")
	}

	open fun windowDidResize(width: Int, height: Int, context: NSOpenGLContext?) {
		println("RESIZED($width, $height)")
		render(context)
	}

	open fun render(context: NSOpenGLContext?) {
		context?.makeCurrentContext()
		println("FRAME")
		glClearColor(0.2f, 0.4f, 0.6f, 1f)
		glClear(GL_COLOR_BUFFER_BIT)
		context?.flushBuffer()
	}
}

private fun runApp(appHandler: MyAppHandler, windowConfig: WindowConfig) {
	autoreleasepool {
		val app = NSApplication.sharedApplication()

		app.delegate = MyAppDelegate(appHandler, windowConfig)
		app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
		app.activateIgnoringOtherApps(true)

		app.run()
	}
}

fun macTrace(str: String) {
	println(str)
}

private class MyAppDelegate(val handler: MyAppHandler, val windowConfig: WindowConfig) : NSObject(),
	NSApplicationDelegateProtocol {
	private val window: NSWindow
	private val openglView: AppNSOpenGLView
	private val appDelegate: AppDelegate

	init {
		macTrace("[a]")
		val mainDisplayRect = NSScreen.mainScreen()!!.frame
		val windowRect = mainDisplayRect.useContents {
			NSMakeRect(
				(size.width * 0.5 - windowConfig.width * 0.5),
				(size.height * 0.5 - windowConfig.height * 0.5),
				windowConfig.width.toDouble(),
				windowConfig.height.toDouble()
			)
		}

		macTrace("[b]")
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

		macTrace("[c]")

		val pixelFormat = attrs.usePinned {
			NSOpenGLPixelFormat.alloc()!!.initWithAttributes(it.addressOf(0).uncheckedCast())!!
		}

		openglView = AppNSOpenGLView(handler, NSMakeRect(0.0, 0.0, 16.0, 16.0), pixelFormat)!!

		appDelegate = AppDelegate(handler, openglView, openglView?.openGLContext)

		macTrace("[d]")

		window = NSWindow(windowRect, windowStyle, NSBackingStoreBuffered, false).apply {
			title = windowConfig.title
			opaque = true
			hasShadow = true
			preferredBackingLocation = NSWindowBackingLocationVideoMemory
			hidesOnDeactivate = false
			releasedWhenClosed = false

			openglView.setFrame(contentRectForFrameRect(frame))
			delegate = appDelegate

			setContentView(openglView)
			makeFirstResponder(openglView)
			setContentMinSize(NSMakeSize(150.0, 100.0))
		}

		macTrace("[e]")
	}

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


		macTrace("[f]")
		openglView?.openGLContext?.makeCurrentContext()
		try {
			macTrace("[g]")
			handler.init(openglView?.openGLContext)
			macTrace("[h]")
			handler.render(openglView?.openGLContext)
			macTrace("[i]")
			appDelegate.timer = NSTimer.scheduledTimerWithTimeInterval(1.0 / 60.0, true, ::timer)
			macTrace("[j]")
		} catch (e: Throwable) {
			e.printStackTrace()
			window.close()
		}
	}


	private fun timer(timer: NSTimer?) {
		macTrace("[k]")
		//println("TIMER")
		handler.render(openglView?.openGLContext)
		macTrace("[l]")
	}

	override fun applicationWillTerminate(notification: NSNotification) {
		println("applicationWillTerminate")
		// Insert code here to tear down your application

	}
}

class AppNSOpenGLView(val handler: MyAppHandler, frameRect: CValue<NSRect>, pixelFormat: NSOpenGLPixelFormat?) :
	NSOpenGLView(frameRect, pixelFormat) {
	private var trackingArea: NSTrackingArea? = null

	override fun acceptsFirstResponder(): Boolean {
		return true
	}

	fun getHeight(): Int = bounds.useContents { size.height.toInt() }

	override fun updateTrackingAreas() {
		trackingArea?.let { removeTrackingArea(it) }
		trackingArea = NSTrackingArea(bounds, NSTrackingActiveWhenFirstResponder or NSTrackingMouseMoved, this, null)
		addTrackingArea(trackingArea!!)
	}

	override fun mouseUp(event: NSEvent) {
		super.mouseUp(event)

		event.locationInWindow.useContents {
			handler.mouseUp(x.toInt(), getHeight() - y.toInt(), event.buttonNumber.toInt())
		}
	}

	override fun mouseDown(event: NSEvent) {
		super.mouseDown(event)
		event.locationInWindow.useContents {
			handler.mouseDown(x.toInt(), getHeight() - y.toInt(), event.buttonNumber.toInt())
		}
	}

	override fun mouseMoved(event: NSEvent) {
		super.mouseMoved(event)
		event.locationInWindow.useContents {
			handler.mouseMoved(x.toInt(), getHeight() - y.toInt())
		}
	}

	fun keyDownUp(event: NSEvent, pressed: Boolean) {
		val str = event.charactersIgnoringModifiers ?: "\u0000"
		val c = str.getOrNull(0) ?: '\u0000'
		val cc = c.toInt().toChar()
		if (pressed) {
			handler.keyDown(cc)
		} else {
			handler.keyUp(cc)
		}
	}

	override fun keyDown(event: NSEvent) = keyDownUp(event, true)
	override fun keyUp(event: NSEvent) = keyDownUp(event, false)
}

class AppDelegate(
	val handler: MyAppHandler,
	val openGLView: AppNSOpenGLView,
	var openGLContext: NSOpenGLContext? = null
) : NSObject(),
	NSWindowDelegateProtocol {
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
		openGLView.bounds.useContents {
			val bounds = this
			handler.windowDidResize(bounds.size.width.toInt(), bounds.size.height.toInt(), openGLContext)
		}
	}
}

val KEYS = mapOf(
	' ' to Key.SPACE,
	'\u000D' to Key.ENTER,
	'\u007F' to Key.BACKSPACE,
	'\u001B' to Key.ESCAPE,
	'\uf700' to Key.UP,
	'\uf701' to Key.DOWN,
	'\uf702' to Key.LEFT,
	'\uf703' to Key.RIGHT,
	'a' to Key.A, 'A' to Key.A,
	'b' to Key.B, 'B' to Key.B,
	'c' to Key.C, 'C' to Key.C,
	'd' to Key.D, 'D' to Key.D,
	'e' to Key.E, 'E' to Key.E,
	'f' to Key.F, 'F' to Key.F,
	'g' to Key.G, 'G' to Key.G,
	'h' to Key.H, 'H' to Key.H,
	'i' to Key.I, 'I' to Key.I,
	'j' to Key.J, 'J' to Key.J,
	'k' to Key.K, 'K' to Key.K,
	'l' to Key.L, 'L' to Key.L,
	'm' to Key.M, 'M' to Key.M,
	'n' to Key.N, 'N' to Key.N,
	'o' to Key.O, 'O' to Key.O,
	'p' to Key.P, 'P' to Key.P,
	'q' to Key.Q, 'Q' to Key.Q,
	'r' to Key.R, 'R' to Key.R,
	's' to Key.S, 'S' to Key.S,
	't' to Key.T, 'T' to Key.T,
	'u' to Key.U, 'U' to Key.U,
	'v' to Key.V, 'V' to Key.V,
	'w' to Key.W, 'W' to Key.W,
	'x' to Key.X, 'X' to Key.X,
	'y' to Key.Y, 'Y' to Key.Y,
	'z' to Key.Z, 'Z' to Key.Z,
	'0' to Key.N0,
	'1' to Key.N1,
	'2' to Key.N2,
	'3' to Key.N3,
	'4' to Key.N4,
	'5' to Key.N5,
	'6' to Key.N6,
	'7' to Key.N7,
	'8' to Key.N8,
	'9' to Key.N9
)
