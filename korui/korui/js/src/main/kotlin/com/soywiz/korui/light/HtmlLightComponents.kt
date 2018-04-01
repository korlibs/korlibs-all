package com.soywiz.korui.light

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.CancellationException
import com.soywiz.korio.FileNotFoundException
import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.vfs.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.files.*
import kotlin.Any
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.RuntimeException
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.apply
import kotlin.arrayOf
import kotlin.browser.*
import kotlin.coroutines.experimental.*
import kotlin.isInfinite
import kotlin.isNaN

var windowInputFile: HTMLInputElement? = null
var selectedFiles = arrayOf<File>()
var mainFrame: HTMLElement? = null

@Suppress("unused")
class HtmlLightComponents : LightComponents() {
	val tDevicePixelRatio = window.devicePixelRatio.toDouble();
	val devicePixelRatio = when {
		tDevicePixelRatio <= 0.0 -> 1.0
		tDevicePixelRatio.isNaN() -> 1.0
		tDevicePixelRatio.isInfinite() -> 1.0
		else -> tDevicePixelRatio
	}

	init {
		addStyles(
			"""
			body {
				font: 11pt Arial;
			}
			.BUTTON {
				-moz-box-shadow:inset 0px 1px 0px 0px #ffffff;
				-webkit-box-shadow:inset 0px 1px 0px 0px #ffffff;
				box-shadow:inset 0px 1px 0px 0px #ffffff;
				background:linear-gradient(to bottom, #ffffff 5%, #f6f6f6 100%);
				background-color:#ffffff;
				-moz-border-radius:6px;
				-webkit-border-radius:6px;
				border-radius:6px;
				border:1px solid #dcdcdc;
				display:inline-block;
				cursor:pointer;
				color:#666666;
				font-family:Arial;
				font-size:15px;
				font-weight:bold;
				padding:6px 24px;
				text-decoration:none;
				text-shadow:0px 1px 0px #ffffff;
			}
			.BUTTON:hover {
				background:linear-gradient(to bottom, #f6f6f6 5%, #ffffff 100%);
				background-color:#f6f6f6;
			}
			.BUTTON:active {
				padding-top: 7px;
				padding-bottom: 5px;

				background:linear-gradient(to bottom, #f0f0f0 5%, #f6f6f6 100%);
				background-color:#f6f6f6;
			}
			.BUTTON:focus {
				/*outline: auto 5px -webkit-focus-ring-color;*/
				outline: auto 1px black;
			}
			.TEXT_AREA {
				white-space: nowrap;
				resize: none;
			}
		"""
		)

		document.body?.style?.background = "#f0f0f0"
		val inputFile = document.createElement("input") as HTMLInputElement
		inputFile.type = "file"
		inputFile.style.visibility = "hidden"
		windowInputFile = inputFile
		selectedFiles = arrayOf()
		document.body?.appendChild(inputFile)
	}

	fun addStyles(css: String) {
		val head: HTMLHeadElement = document.head ?: document.getElementsByTagName("head")[0] as HTMLHeadElement
		val style = document.createElement("style") as HTMLStyleElement

		style.type = "text/css"
		if (style.asDynamic().styleSheet != null) {
			style.asDynamic().styleSheet.cssText = css
		} else {
			style.appendChild(document.createTextNode(css))
		}

		head.appendChild(style)
	}

	override fun create(type: LightType): LightComponentInfo {
		var agg: AG? = null
		val handle: HTMLElement = when (type) {
			LightType.FRAME -> {
				(document.createElement("article") as HTMLElement).apply {
					this.className = "FRAME"
					document.body?.appendChild(this)
					mainFrame = this
					mainFrame?.style?.visibility = "hidden"
				}
			}
			LightType.CONTAINER -> {
				(document.createElement("div") as HTMLElement).apply {
					this.className = "CONTAINER"
				}
			}
			LightType.SCROLL_PANE -> {
				(document.createElement("div") as HTMLElement).apply {
					this.className = "SCROLL_PANE"
				}
			}
			LightType.BUTTON -> {
				(document.createElement("input") as HTMLInputElement).apply {
					this.className = "BUTTON"
					this.type = "button"
				}
			}
			LightType.PROGRESS -> {
				(document.createElement("progress") as HTMLElement).apply {
					this.className = "PROGRESS"
				}
			}
			LightType.IMAGE -> {
				(document.createElement("canvas") as HTMLCanvasElement)!!.apply {
					this.className = "IMAGE"
					this.style.imageRendering = "pixelated"
				}
			}
			LightType.LABEL -> {
				(document.createElement("label") as HTMLElement).apply {
					this.className = "LABEL"
				}
			}
			LightType.TEXT_FIELD -> {
				(document.createElement("input") as HTMLInputElement)!!.apply {
					this.className = "TEXT_FIELD"
					this.type = "text"
				}
			}
			LightType.TEXT_AREA -> {
				(document.createElement("textarea") as HTMLElement).apply {
					this.className = "TEXT_AREA"
					//this["type"] = "text"
				}
			}
			LightType.CHECK_BOX -> {
				(document.createElement("label") as HTMLElement).apply {
					this.className = "CHECK_BOX"
					this.asDynamic()["data-type"] = "checkbox"
					val input: HTMLInputElement = document.createElement("input") as HTMLInputElement
					input.apply {
						this.className = "TEXT_FIELD"
						this.type = "checkbox"
					}
					this.appendChild(input)
					this.appendChild(document.createElement("span")!!)
				}
			}
			LightType.AGCANVAS -> {
				agg = agFactory.create()
				val cc = agg.nativeComponent as HTMLCanvasElement
				cc.tabIndex = 1
				cc.style.outline = "none"
				cc
			}
			else -> {
				(document.createElement("div") as HTMLElement).apply {
					this.className = "UNKNOWN"
				}
			}
		}

		handle.apply {
			val style = this.style
			style.position = "absolute"

			val overflow = when (type) {
				LightType.SCROLL_PANE, LightType.TEXT_AREA, LightType.TEXT_FIELD -> true
				else -> false
			}

			style.overflowY = if (overflow) "auto" else "hidden"
			style.overflowX = if (overflow) "auto" else "hidden"
			style.left = "0px"
			style.top = "0px"
			style.width = "100px"
			style.height = "100px"
		}

		return LightComponentInfo(handle).apply {
			if (agg != null) this.ag = agg
		}
	}

	override fun setParent(c: Any, parent: Any?) {
		val child = c as HTMLElement
		child.parentNode?.removeChild(child)
		if (parent != null) {
			(parent as HTMLElement).appendChild(child)
		}
	}

	private fun EventTarget.addCloseableEventListener(name: String, func: (Event) -> Unit): Closeable {
		this.addEventListener(name, func)
		return Closeable { this.removeEventListener(name, func) }
	}

	override fun addHandler(c: Any, listener: LightMouseHandler): Closeable {
		val node = c as HTMLElement

		val info = LightMouseHandler.Info()
		fun process(e: MouseEvent, buttons: Int) = info.apply {
			this.x = (e.offsetX.toInt())
			this.y = (e.offsetY.toInt())
			this.buttons = buttons
		}

		return listOf(
			node.addCloseableEventListener("click", { listener.click2(process(it as MouseEvent, 1)) }),
			node.addCloseableEventListener("mouseover", { listener.over2(process(it as MouseEvent, 0)) }),
			node.addCloseableEventListener("mousemove", { listener.over2(process(it as MouseEvent, 0)) }),
			node.addCloseableEventListener("mouseup", { listener.up2(process(it as MouseEvent, 0)) }),
			node.addCloseableEventListener("mousedown", { listener.down2(process(it as MouseEvent, 0)) })
		).closeable()
	}

	override fun addHandler(c: Any, listener: LightChangeHandler): Closeable {
		val node = c as HTMLElement
		val info = LightChangeHandler.Info()

		return listOf(
			node.addCloseableEventListener("change", { listener.changed2(info) }),
			node.addCloseableEventListener("keypress", { listener.changed2(info) }),
			node.addCloseableEventListener("input", { listener.changed2(info) }),
			node.addCloseableEventListener("textInput", { listener.changed2(info) }),
			node.addCloseableEventListener("paste", { listener.changed2(info) })
		).closeable()
	}

	override fun addHandler(c: Any, listener: LightResizeHandler): Closeable {
		val node = window
		val info = LightResizeHandler.Info()

		fun send() {
			if (mainFrame != null) {

				mainFrame?.style?.width = "${window.innerWidth}px"
				mainFrame?.style?.height = "${window.innerHeight}px"
			}

			listener.resized2(info.apply {
				width = window.innerWidth.toInt()
				height = window.innerHeight.toInt()
			})
		}

		send()

		return listOf(
			node.addCloseableEventListener("resize", { send() })
		).closeable()
	}

	override fun addHandler(c: Any, listener: LightKeyHandler): Closeable {
		val node = c as HTMLElement
		val info = LightKeyHandler.Info()

		fun process(e: KeyboardEvent) = info.apply {
			this.keyCode = e.keyCode
		}

		return listOf(
			node.addCloseableEventListener("keydown", { listener.down2(process(it as KeyboardEvent)) }),
			node.addCloseableEventListener("keyup", { listener.up2(process(it as KeyboardEvent)) }),
			node.addCloseableEventListener("keypress", { listener.typed2(process(it as KeyboardEvent)) })
		).closeable()
	}

	override fun addHandler(c: Any, listener: LightGamepadHandler): Closeable {
		return super.addHandler(c, listener)
	}

	override fun addHandler(c: Any, listener: LightTouchHandler): Closeable {
		val node = c as HTMLElement

		fun process(e: Event, preventDefault: Boolean): List<LightTouchHandler.Info> {
			val out = arrayListOf<LightTouchHandler.Info>()

			val touches = e.unsafeCast<dynamic>().changedTouches
			val touchesLength: Int = touches.length.unsafeCast<Int>()
			for (n in 0 until touchesLength) {
				val touch = touches[n].unsafeCast<dynamic>()
				out += LightTouchHandler.Info().apply {
					this.x = (touch.pageX * devicePixelRatio)
					this.y = (touch.pageY * devicePixelRatio)
					this.id = touch.identifier
				}
			}
			if (preventDefault) e.preventDefault()
			return out
		}

		return listOf(
			node.addCloseableEventListener(
				"touchstart",
				{ for (info in process(it, preventDefault = true)) listener.start2(info) }),
			node.addCloseableEventListener(
				"touchend",
				{ for (info in process(it, preventDefault = true)) listener.end2(info) }),
			node.addCloseableEventListener(
				"touchmove",
				{ for (info in process(it, preventDefault = true)) listener.move2(info) })
		).closeable()
	}

	override fun addHandler(c: Any, listener: LightDropHandler): Closeable {
		val node = c as HTMLElement

		fun ondrop(e: DragEvent) {
			e.preventDefault()
			//console.log("ondrop", e)
			val dt = e.dataTransfer ?: return
			val files = arrayListOf<File>()
			for (n in 0 until dt.items.length) {
				val item = dt.items[n] ?: continue
				val file = item.getAsFile() ?: continue
				files += file
				//console.log("ondrop", file)
			}
			//jsEmptyArray()
			val fileSystem = JsFilesVfs(files)
			listener.files(LightDropHandler.FileInfo(files.map { fileSystem[it.name] }))
		}

		fun ondragenter(e: DragEvent) {
			e.preventDefault()
			listener.enter(LightDropHandler.EnterInfo())
		}

		fun ondragexit(e: DragEvent) {
			e.preventDefault()
			listener.exit()
		}

		return listOf(
			node.addCloseableEventListener("drop") {
				//console.log("html5drop")
				ondrop(it.unsafeCast<DragEvent>())
			},
			node.addCloseableEventListener("dragenter") { ondragenter(it.unsafeCast<DragEvent>()) },
			node.addCloseableEventListener("dragover") { it.preventDefault() },
			node.addCloseableEventListener("dragleave") { ondragexit(it.unsafeCast<DragEvent>()) }
		).closeable()
	}

	override fun <T> callAction(c: Any, key: LightAction<T>, param: T) {
		when (key) {
			LightAction.FOCUS -> {
				val child = c.asDynamic()
				child.focus()
			}
		}
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		val child = c as HTMLElement
		val childOrDocumentBody = if (child.nodeName.toLowerCase() == "article") document.body else child
		val nodeName = child.nodeName.toLowerCase()
		when (key) {
			LightProperty.TEXT -> {
				val v = key[value]
				if (nodeName == "article") {
					document.title = v
				} else if (nodeName == "input" || nodeName == "textarea") {
					(child as HTMLInputElement).value = v
				} else {
					if ((child.asDynamic()["data-type"]) == "checkbox") {
						(child.querySelector("span") as HTMLSpanElement)?.innerText = v
					} else {
						child.innerText = v
					}
				}
			}
			LightProperty.PROGRESS_CURRENT -> {
				val v = key[value]
				(child as HTMLInputElement).value = "$v"
			}
			LightProperty.PROGRESS_MAX -> {
				val v = key[value]
				(child as HTMLInputElement).max = "$v"
			}
			LightProperty.BGCOLOR -> {
				val v = key[value]
				childOrDocumentBody?.style?.background = colorString(v)
			}
			LightProperty.IMAGE_SMOOTH -> {
				val v = key[value]
				child.style.imageRendering = if (v) "auto" else "pixelated"
			}
			LightProperty.ICON -> {
				val v = key[value]
				if (v != null) {
					val href = HtmlImage.htmlCanvasToDataUrl(HtmlImage.bitmapToHtmlCanvas(v.toBMP32()))

					var link: HTMLLinkElement? =
						document.querySelector("link[rel*='icon']").unsafeCast<HTMLLinkElement>()
					if (link == null) {
						link = document.createElement("link") as HTMLLinkElement
					}
					link.type = "image/x-icon"
					link.rel = "shortcut icon"
					link.href = href
					document.getElementsByTagName("head")[0]?.appendChild(link)
				}
			}
			LightProperty.IMAGE -> {
				val bmp = key[value]
				if (bmp is CanvasNativeImage) {
					setCanvas(c, bmp.canvas)
				} else {
					setImage32(c, bmp?.toBMP32())
				}
			}
			LightProperty.VISIBLE -> {
				val v = key[value]
				if (child != null) child.style.display = if (v) "block" else "none"
			}
			LightProperty.CHECKED -> {
				val v = key[value]
				(child.querySelector("input[type=checkbox]") as HTMLInputElement).checked = v
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T> getProperty(c: Any, key: LightProperty<T>): T {
		val child = c as HTMLElement

		when (key) {
			LightProperty.TEXT -> {
				return (child as HTMLInputElement).value as T
			}
			LightProperty.CHECKED -> {
				val input = (child as HTMLInputElement).querySelector("input[type=checkbox]")
				val checked: Boolean = input.asDynamic().checked
				return checked as T
			}
		}
		return super.getProperty(c, key)
	}


	fun colorString(c: Int) = "RGBA(${RGBA.getR(c)},${RGBA.getG(c)},${RGBA.getB(c)},${RGBA.getAf(c)})"

	private fun setCanvas(c: Any, bmp: HTMLCanvasElement?) {
		val targetCanvas = c as HTMLCanvasElement
		if (bmp != null) {
			targetCanvas.width = bmp.width
			targetCanvas.height = bmp.height
			val ctx = targetCanvas.getContext("2d") as CanvasRenderingContext2D
			HtmlImage.htmlCanvasClear(targetCanvas)
			ctx.drawImage(bmp, 0.0, 0.0)
		} else {
			HtmlImage.htmlCanvasClear(targetCanvas)
		}
	}

	private fun setImage32(c: Any, bmp: Bitmap32?) {
		val canvas = c as HTMLCanvasElement
		if (bmp != null) {
			HtmlImage.htmlCanvasSetSize(canvas, bmp.width, bmp.height)
			HtmlImage.renderToHtmlCanvas(bmp, canvas)
		} else {
			HtmlImage.htmlCanvasClear(canvas)
		}
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
		val child = c as HTMLElement
		val childStyle = child.style
		childStyle.left = "${x}px"
		childStyle.top = "${y}px"
		childStyle.width = "${width}px"
		childStyle.height = "${height}px"

		if (child is HTMLCanvasElement) {
			child.width = (width * devicePixelRatio).toInt()
			child.height = (height * devicePixelRatio).toInt()
		}
	}

	override fun repaint(c: Any) {
		mainFrame?.style?.visibility = "visible"
	}

	suspend override fun dialogAlert(c: Any, message: String) = korioSuspendCoroutine<Unit> { c ->
		window.alert(message)
		window.setTimeout({
			c.resume(Unit)
		}, 0)
	}

	suspend override fun dialogPrompt(c: Any, message: String, initialValue: String): String =
		korioSuspendCoroutine { c ->
			val result = window.prompt(message, initialValue)
			window.setTimeout({
				if (result == null) {
					c.resumeWithException(CancellationException("cancelled"))
				} else {
					c.resume(result)
				}
			}, 0)
		}

	override suspend fun dialogOpenFile(c: Any, filter: String): VfsFile = korioSuspendCoroutine { continuation ->
		val inputFile = windowInputFile
		var completedOnce = false
		var files = arrayOf<File>()

		val completed = {
			if (!completedOnce) {
				completedOnce = true

				selectedFiles = files

				//console.log('completed', files);
				if (files.size > 0.0) {
					val fileName = files[0].name
					val ff = arrayListOf<File>()
					for (n in 0 until selectedFiles.asDynamic().length) ff += selectedFiles[n].unsafeCast<File>()
					val sf = JsFilesVfs(ff)
					continuation.resume(sf[fileName])
				} else {
					continuation.resumeWithException(CancellationException("cancel"))
				}
			}
		}

		windowInputFile?.value = ""

		windowInputFile?.onclick = {
			document.body?.onfocus = {
				document.body?.onfocus = null
				window.setTimeout({
					completed()
				}, 2000)
			}
			Unit
		}

		windowInputFile?.onchange = { e ->
			files = e?.target.asDynamic()["files"]
			//var v = this.value;
			//console.log(v);
			completed()
		}

		inputFile?.click()
	}

	override fun openURL(url: String): Unit {
		window.open(url, "_blank")
	}

	override fun getDpi(): Double {
		return (window.devicePixelRatio.toInt() * 96).toDouble()
	}
}

class JsFileAsyncStreamBase(val jsfile: File) : AsyncStreamBase() {
	//val alength = jsfile.size.unsafeCast<Double>().toLong()
//
	//init {
	//	console.log("JsFileAsyncStreamBase.Opened ", jsfile)
	//	console.log("JsFileAsyncStreamBase.Length: " + alength)
	//}

	override suspend fun getLength(): Long {
		return jsfile.size.unsafeCast<Double>().toLong()
	}

	suspend fun readBytes(position: Double, len: Int): ByteArray = suspendCoroutine { c ->
		val reader = FileReader()
		// @TODO: Blob.slice should use Double
		val djsfile = jsfile.asDynamic()
		val slice = djsfile.slice(position, (position + len))

		reader.onload = {
			val result = reader.result
			c.resume(Int8Array(result.unsafeCast<ArrayBuffer>()).unsafeCast<ByteArray>())
		}

		reader.onerror = {
			c.resumeWithException(RuntimeException("error reading file"))
		}
		reader.readAsArrayBuffer(slice)
	}

	override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val data = readBytes(position.toDouble(), len)
		arraycopy(data, 0, buffer, offset, data.size)
		//console.log("JsFileAsyncStreamBase.read.requested", buffer)
		//console.log("JsFileAsyncStreamBase.read.requested", position, offset, len)
		//console.log("JsFileAsyncStreamBase.read", data)
		//console.log("JsFileAsyncStreamBase.read.result:", data.size)
		return data.size
	}
}

internal class JsFilesVfs(val files: List<File>) : Vfs() {
	private fun _locate(name: String): File? {
		val length = files.size
		for (n in 0 until length) {
			val file = files[n]
			if (file.name == name) {
				return file
			}
		}
		return null
	}

	private fun locate(path: String): File? = _locate(path.trim('/'))

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val jsfile = locate(path) ?: throw FileNotFoundException(path)
		return JsFileAsyncStreamBase(jsfile).toAsyncStream()
	}

	override suspend fun stat(path: String): VfsStat {
		val file = locate(path) ?: return createNonExistsStat(path)
		return createExistsStat(path, isDirectory = false, size = file.size.toDouble().toLong())
	}

	override suspend fun list(path: String): SuspendingSequence<VfsFile> {
		return this.files.map { this[it.name] }.toAsync()
	}
}
