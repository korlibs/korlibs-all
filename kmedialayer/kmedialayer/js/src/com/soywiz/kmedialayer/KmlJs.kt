package com.soywiz.kmedialayer

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.url.*
import org.w3c.files.*
import kotlin.browser.*
import kotlin.coroutines.experimental.*
import kotlin.js.*

object KmlBaseJs : KmlBase() {
	override fun application(windowConfig: WindowConfig, listener: KMLWindowListener): Unit = launchAndForget {
		document.title = windowConfig.title
		var mustAppendCanvas = false
		val canvas =
			(document.getElementById("kml-canvas") ?: ((document.createElement("canvas") as HTMLCanvasElement).apply {
				id = "kml-canvas"
				document.body?.style?.padding = "0"
				document.body?.style?.margin = "0"
				mustAppendCanvas = true
			})) as HTMLCanvasElement

		fun mouseUpdate(me: MouseEvent) {
			val pos = canvas.getBoundingClientRect()
			val mouseX = me.clientX - pos.left.toInt()
			val mouseY = me.clientY - pos.top.toInt()
			listener.mouseUpdateMove(mouseX, mouseY)
		}

		fun mouseUpdateButtons(me: MouseEvent, pressed: Boolean) {
			listener.mouseUpdateButton(me.button.toInt(), pressed)
		}

		val gl = KmlGlJsCanvas(canvas)

		listener.init(gl)

		window.addEventListener("mousemove", { e: Event -> mouseUpdate(e as MouseEvent) })
		window.addEventListener("mousedown", { e: Event -> mouseUpdateButtons(e as MouseEvent, true) })
		window.addEventListener("mouseup", { e: Event -> mouseUpdateButtons(e as MouseEvent, false) })

		window.addEventListener("keydown", { e: Event ->
			listener.keyUpdate(KEYS[(e as KeyboardEvent).keyCode] ?: Key.UNKNOWN, true)
		})

		window.addEventListener("keyup", { e: Event ->
			listener.keyUpdate(KEYS[(e as KeyboardEvent).keyCode] ?: Key.UNKNOWN, false)
		})

		if (mustAppendCanvas) {
			fun resize() {
				val width = window.innerWidth
				val height = window.innerHeight
				canvas.width = width
				canvas.height = height
				canvas.style.width = "${width}px"
				canvas.style.height = "${height}px"
				document.body?.style?.width = "${width}px"
				document.body?.style?.height = "100%"
				document.body?.style?.overflowX = "hidden"
				document.body?.style?.overflowY = "hidden"
				gl.viewport(0, 0, width, height)
				listener.resized(width, height)
			}
			window.onresize = {
				resize()
			}
			resize()
			document.body?.appendChild(canvas)
		}

		fun frame(ms: Double) {
			window.requestAnimationFrame(::frame)
			gl.startFrame()
			listener.render(gl)
			gl.endFrame()
		}

		frame(0.0)
	}

	override suspend fun decodeImage(path: String): KmlNativeImageData = suspendCoroutine { c ->
		val image = document.createElement("img").unsafeCast<HTMLImageElement>()
		image.src = path
		image.onerror = { _, msg, _, _, _ ->
			c.resumeWithException(Exception("Error loading image: $msg"))
		}
		image.onload = {
			c.resume(KmlImgNativeImageData(image))
		}
	}

	override suspend fun decodeImage(data: ByteArray): KmlNativeImageData {
		val url = URL.createObjectURL(Blob(arrayOf(data.unsafeCast<Int8Array>())))
		try {
			return decodeImage(url)
		} finally {
			URL.revokeObjectURL(url)
		}
	}

	override suspend fun delay(ms: Int): Unit = suspendCoroutine { c ->
		window.setTimeout({ c.resume(Unit) }, ms)
	}

	override fun enqueue(task: () -> Unit) {
		// @TODO: Set immediate o direct execution!
		window.setTimeout({ task() }, 0)
	}

	override fun currentTimeMillis(): Double = Date.now()
}

actual val Kml: KmlBase = KmlBaseJs

class KmlImgNativeImageData(val img: HTMLImageElement) : KmlNativeImageData {
	override val width get() = img.width
	override val height get() = img.width
}

private val KEYS = mapOf(
	8 to Key.BACKSPACE,
	9 to Key.TAB,
	13 to Key.ENTER,
	16 to Key.LEFT_SHIFT,
	17 to Key.LEFT_CONTROL,
	18 to Key.LEFT_ALT,
	19 to Key.PAUSE,
	20 to Key.CAPS_LOCK,
	27 to Key.ESCAPE,
	33 to Key.PAGE_UP,
	34 to Key.PAGE_DOWN,
	35 to Key.END,
	36 to Key.HOME,
	37 to Key.LEFT,
	38 to Key.UP,
	39 to Key.RIGHT,
	40 to Key.DOWN,
	45 to Key.INSERT,
	46 to Key.DELETE,
	48 to Key.N0,
	49 to Key.N1,
	50 to Key.N2,
	51 to Key.N3,
	52 to Key.N4,
	53 to Key.N5,
	54 to Key.N6,
	55 to Key.N7,
	56 to Key.N8,
	57 to Key.N9,
	65 to Key.A,
	66 to Key.B,
	67 to Key.C,
	68 to Key.D,
	69 to Key.E,
	70 to Key.F,
	71 to Key.G,
	72 to Key.H,
	73 to Key.I,
	74 to Key.J,
	75 to Key.K,
	76 to Key.L,
	77 to Key.M,
	78 to Key.N,
	79 to Key.O,
	80 to Key.P,
	81 to Key.Q,
	82 to Key.R,
	83 to Key.S,
	84 to Key.T,
	85 to Key.U,
	86 to Key.V,
	87 to Key.W,
	88 to Key.X,
	89 to Key.Y,
	90 to Key.Z,
	91 to Key.LEFT_SUPER,
	92 to Key.RIGHT_SUPER,
	93 to Key.SELECT_KEY,
	96 to Key.KP_0,
	97 to Key.KP_1,
	98 to Key.KP_2,
	99 to Key.KP_3,
	100 to Key.KP_4,
	101 to Key.KP_5,
	102 to Key.KP_6,
	103 to Key.KP_7,
	104 to Key.KP_8,
	105 to Key.KP_9,
	106 to Key.KP_MULTIPLY,
	107 to Key.KP_ADD,
	109 to Key.KP_SUBTRACT,
	110 to Key.KP_DECIMAL,
	111 to Key.KP_DIVIDE,
	112 to Key.F1,
	113 to Key.F2,
	114 to Key.F3,
	115 to Key.F4,
	116 to Key.F5,
	117 to Key.F6,
	118 to Key.F7,
	119 to Key.F8,
	120 to Key.F9,
	121 to Key.F10,
	122 to Key.F11,
	123 to Key.F12,
	144 to Key.NUM_LOCK,
	145 to Key.SCROLL_LOCK,
	186 to Key.SEMICOLON,
	187 to Key.EQUAL,
	188 to Key.COMMA,
	189 to Key.UNDERLINE,
	190 to Key.PERIOD,
	191 to Key.SLASH,
	192 to Key.GRAVE_ACCENT,
	219 to Key.LEFT_BRACKET,
	220 to Key.BACKSLASH,
	221 to Key.RIGHT_BRACKET,
	222 to Key.APOSTROPHE
)