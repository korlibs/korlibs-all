package com.soywiz.kmedialayer

import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL.*
import org.lwjgl.system.MemoryUtil.*
import java.awt.image.*
import java.awt.image.BufferedImage
import java.io.*
import java.nio.*
import javax.imageio.*


object KmlBaseJvm : KmlBaseNoEventLoop() {
    lateinit var keyCallback: GLFWKeyCallback
    lateinit var cursorPosCallback: GLFWCursorPosCallback
    lateinit var mouseButtonCallback: GLFWMouseButtonCallback
    lateinit var joystickCallback: GLFWJoystickCallback
    lateinit var joystickCallbackI: GLFWJoystickCallbackI
    lateinit var frameBufferSize: GLFWFramebufferSizeCallback
    var window: Long = 0L

    override fun application(windowConfig: WindowConfig, listener: KMLWindowListener) {
        System.setProperty("java.awt.headless", "true")
        // https://www.lwjgl.org/guide

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) throw IllegalStateException("Unable to initialize GLFW")

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable
        window = glfwCreateWindow(windowConfig.width, windowConfig.height, windowConfig.title, NULL, NULL)

        val pWidth = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
        val pHeight = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
        glfwGetWindowSize(window, pWidth, pHeight)
        val wwidth = pWidth[0]
        val wheight = pHeight[0]
        val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

        glfwSetWindowPos(window, (vidmode.width() - wwidth) / 2, (vidmode.height() - wheight) / 2)
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)

        val capabilities = createCapabilities()

        glfwMakeContextCurrent(window)

        val gl = JvmKmlGl()
        runBlocking {
            listener.init(gl)
        }

        keyCallback = object : GLFWKeyCallback() {
            override fun invoke(
                window: kotlin.Long,
                key: kotlin.Int,
                scancode: kotlin.Int,
                action: kotlin.Int,
                mods: kotlin.Int
            ) {
                //if (action != GLFW_REPEAT) { // Ignore repeat events
                listener.keyUpdate(KEYS[key] ?: Key.UNKNOWN, action != GLFW_RELEASE)
                //}
            }
        }
        glfwSetKeyCallback(window, keyCallback)

        cursorPosCallback = object : GLFWCursorPosCallback() {
            override fun invoke(window: kotlin.Long, xpos: kotlin.Double, ypos: kotlin.Double) {
                listener.mouseUpdateMove(xpos.toInt(), ypos.toInt())
            }
        }
        glfwSetCursorPosCallback(window, cursorPosCallback)

        fun render() {
            gl.startFrame()
            listener.render(gl)
            gl.endFrame()
        }

        frameBufferSize = object : GLFWFramebufferSizeCallback() {
            override fun invoke(window: kotlin.Long, width: kotlin.Int, height: kotlin.Int) {
                glfwMakeContextCurrent(window)
                gl.viewport(0, 0, width, height)
                listener.resized(width, height)
                render()
                glfwSwapBuffers(window) // swap the color buffers
            }
        }
        glfwSetFramebufferSizeCallback(window, frameBufferSize)
        //glfwSetWindowIcon()

        mouseButtonCallback = object : GLFWMouseButtonCallback() {
            override fun invoke(window: kotlin.Long, button: kotlin.Int, action: kotlin.Int, mods: kotlin.Int) {
                val pressing = (action == GLFW_PRESS)
                listener.mouseUpdateButton(button, pressing)
            }
        }
        glfwSetMouseButtonCallback(window, mouseButtonCallback)

        while (!glfwWindowShouldClose(window)) {
            glfwMakeContextCurrent(window)
            updateGamepadButtons(listener)
            render()
            glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            pollEvents()

            timers.check()
        }
    }

    class GamepadInfo(val id: Int) {
        class Button(val id: Int, var ratio: Double = 0.0)

        var connected = false
        var name = "unkown"
        val buttons = (0 until 32).map { Button(it) }
    }

    val gamepads = (0 until 16).map { GamepadInfo(it) }

    fun updateGamepadButtons(listener: KMLWindowListener) {
        for (gamepad in gamepads) {
            val id = gamepad.id
            val present = glfwJoystickPresent(id)

            if (gamepad.connected != present) {
                gamepad.connected = present
                listener.gamepadConnection(id, gamepad.name, gamepad.connected)
            }

            if (present) {
                val name = glfwGetJoystickName(id) ?: "unknown"
                val axes = glfwGetJoystickAxes(id)!!.toTypedArray()
                val buttons = glfwGetJoystickButtons(id)!!.toTypedArray()
                gamepad.name = name


                if (axes.size >= 2) {
                    listener.gamepadStickUpdate(id, GameStick.LEFT, axes[0].toDouble(), axes[1].toDouble())
                }
                if (axes.size >= 4) {
                    listener.gamepadStickUpdate(id, GameStick.RIGHT, axes[2].toDouble(), axes[3].toDouble())
                }

                for (bid in buttons.indices) {
                    val gbutton = BUTTONS[bid] ?: continue
                    val button = gamepad.buttons[gbutton.id]
                    val newValue = if (buttons[bid].toInt() != 0) 1.0 else 0.0
                    if (button.ratio != newValue || newValue != 0.0) {
                        button.ratio = newValue
                        listener.gamepadButtonUpdate(id, gbutton, button.ratio)
                    }
                }
            }
        }
    }

    override fun sleep(time: Int) {
        Thread.sleep(time.toLong())
    }

    override fun pollEvents() {
        glfwPollEvents()
        if (glfwWindowShouldClose(KmlBaseJvm.window)) {
            System.exit(0)
        }
    }

    override suspend fun decodeImage(data: ByteArray): KmlNativeImageData {
        val img = ImageIO.read(ByteArrayInputStream(data))
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_4BYTE_ABGR).apply {
            graphics.apply {
                drawImage(img, 0, 0, null)
                dispose()
            }
        }
        return BufferedImageKmlNativeImageData(out)
    }

    override suspend fun loadFileBytes(path: String, range: LongRange?): ByteArray {
        val file = File(path)
        if (!file.exists()) {

            val iss = javaClass.getResourceAsStream(path)
                    ?: ClassLoader.getSystemResourceAsStream(path)
                    ?: ClassLoader.getSystemClassLoader()?.getResourceAsStream(path)
                    ?: javaClass.getResourceAsStream("/$path")
                    ?: ClassLoader.getSystemResourceAsStream("/$path")
                    ?: ClassLoader.getSystemClassLoader()?.getResourceAsStream("/$path")
                    ?: error("loadFileBytes can't find '$path' in filesystem/resources")
            val full = iss.readBytes()
            return range?.let { full.copyOfRange(it.start.toInt(), (it.endInclusive + 1).toInt()) } ?: full
        } else {
            val raf = RandomAccessFile(file, "r")
            if (range != null) raf.seek(range.start)
            val len = range?.let { (it.endInclusive - it.start) - 1 } ?: (raf.length() - raf.filePointer)
            val ba = ByteArray(len.toInt())
            val outSize = raf.read(ba)
            return ba.copyOf(outSize)
        }
    }

    override suspend fun writeFileBytes(path: String, data: ByteArray, offset: Long?) {
        val raf = RandomAccessFile(File(path), "rw")
        if (offset != null) raf.seek(offset)
        raf.write(data)
        if (offset == null) raf.setLength(raf.filePointer)
    }

    override fun currentTimeMillis(): Double = System.currentTimeMillis().toDouble()
}

private val BUTTONS = mapOf(
    GLFW_GAMEPAD_BUTTON_A to GameButton.BUTTON1,
    GLFW_GAMEPAD_BUTTON_B to GameButton.BUTTON2,
    GLFW_GAMEPAD_BUTTON_X to GameButton.BUTTON3,
    GLFW_GAMEPAD_BUTTON_Y to GameButton.BUTTON4,
    GLFW_GAMEPAD_BUTTON_LEFT_BUMPER to GameButton.LEFT_TRIGGER1,
    GLFW_GAMEPAD_BUTTON_LEFT_THUMB to GameButton.LEFT_TRIGGER2,
    GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER to GameButton.RIGHT_TRIGGER1,
    GLFW_GAMEPAD_BUTTON_RIGHT_THUMB to GameButton.RIGHT_TRIGGER2,
    GLFW_GAMEPAD_BUTTON_BACK to GameButton.SELECT,
    GLFW_GAMEPAD_BUTTON_START to GameButton.START,
    GLFW_GAMEPAD_BUTTON_DPAD_LEFT to GameButton.LEFT,
    GLFW_GAMEPAD_BUTTON_DPAD_RIGHT to GameButton.RIGHT,
    GLFW_GAMEPAD_BUTTON_DPAD_UP to GameButton.UP,
    GLFW_GAMEPAD_BUTTON_DPAD_DOWN to GameButton.DOWN
)

private val KEYS = mapOf(
    GLFW_KEY_SPACE to Key.SPACE,
    GLFW_KEY_APOSTROPHE to Key.APOSTROPHE,
    GLFW_KEY_COMMA to Key.COMMA,
    GLFW_KEY_MINUS to Key.MINUS,
    GLFW_KEY_PERIOD to Key.PERIOD,
    GLFW_KEY_SLASH to Key.SLASH,
    GLFW_KEY_0 to Key.N0,
    GLFW_KEY_1 to Key.N1,
    GLFW_KEY_2 to Key.N2,
    GLFW_KEY_3 to Key.N3,
    GLFW_KEY_4 to Key.N4,
    GLFW_KEY_5 to Key.N5,
    GLFW_KEY_6 to Key.N6,
    GLFW_KEY_7 to Key.N7,
    GLFW_KEY_8 to Key.N8,
    GLFW_KEY_9 to Key.N9,
    GLFW_KEY_SEMICOLON to Key.SEMICOLON,
    GLFW_KEY_EQUAL to Key.EQUAL,
    GLFW_KEY_A to Key.A,
    GLFW_KEY_B to Key.B,
    GLFW_KEY_C to Key.C,
    GLFW_KEY_D to Key.D,
    GLFW_KEY_E to Key.E,
    GLFW_KEY_F to Key.F,
    GLFW_KEY_G to Key.G,
    GLFW_KEY_H to Key.H,
    GLFW_KEY_I to Key.I,
    GLFW_KEY_J to Key.J,
    GLFW_KEY_K to Key.K,
    GLFW_KEY_L to Key.L,
    GLFW_KEY_M to Key.M,
    GLFW_KEY_N to Key.N,
    GLFW_KEY_O to Key.O,
    GLFW_KEY_P to Key.P,
    GLFW_KEY_Q to Key.Q,
    GLFW_KEY_R to Key.R,
    GLFW_KEY_S to Key.S,
    GLFW_KEY_T to Key.T,
    GLFW_KEY_U to Key.U,
    GLFW_KEY_V to Key.V,
    GLFW_KEY_W to Key.W,
    GLFW_KEY_X to Key.X,
    GLFW_KEY_Y to Key.Y,
    GLFW_KEY_Z to Key.Z,
    GLFW_KEY_LEFT_BRACKET to Key.LEFT_BRACKET,
    GLFW_KEY_BACKSLASH to Key.BACKSLASH,
    GLFW_KEY_RIGHT_BRACKET to Key.RIGHT_BRACKET,
    GLFW_KEY_GRAVE_ACCENT to Key.GRAVE_ACCENT,
    GLFW_KEY_WORLD_1 to Key.WORLD_1,
    GLFW_KEY_WORLD_2 to Key.WORLD_2,
    GLFW_KEY_ESCAPE to Key.ESCAPE,
    GLFW_KEY_ENTER to Key.ENTER,
    GLFW_KEY_TAB to Key.TAB,
    GLFW_KEY_BACKSPACE to Key.BACKSPACE,
    GLFW_KEY_INSERT to Key.INSERT,
    GLFW_KEY_DELETE to Key.DELETE,
    GLFW_KEY_RIGHT to Key.RIGHT,
    GLFW_KEY_LEFT to Key.LEFT,
    GLFW_KEY_DOWN to Key.DOWN,
    GLFW_KEY_UP to Key.UP,
    GLFW_KEY_PAGE_UP to Key.PAGE_UP,
    GLFW_KEY_PAGE_DOWN to Key.PAGE_DOWN,
    GLFW_KEY_HOME to Key.HOME,
    GLFW_KEY_END to Key.END,
    GLFW_KEY_CAPS_LOCK to Key.CAPS_LOCK,
    GLFW_KEY_SCROLL_LOCK to Key.SCROLL_LOCK,
    GLFW_KEY_NUM_LOCK to Key.NUM_LOCK,
    GLFW_KEY_PRINT_SCREEN to Key.PRINT_SCREEN,
    GLFW_KEY_PAUSE to Key.PAUSE,
    GLFW_KEY_F1 to Key.F1,
    GLFW_KEY_F2 to Key.F2,
    GLFW_KEY_F3 to Key.F3,
    GLFW_KEY_F4 to Key.F4,
    GLFW_KEY_F5 to Key.F5,
    GLFW_KEY_F6 to Key.F6,
    GLFW_KEY_F7 to Key.F7,
    GLFW_KEY_F8 to Key.F8,
    GLFW_KEY_F9 to Key.F9,
    GLFW_KEY_F10 to Key.F10,
    GLFW_KEY_F11 to Key.F11,
    GLFW_KEY_F12 to Key.F12,
    GLFW_KEY_F13 to Key.F13,
    GLFW_KEY_F14 to Key.F14,
    GLFW_KEY_F15 to Key.F15,
    GLFW_KEY_F16 to Key.F16,
    GLFW_KEY_F17 to Key.F17,
    GLFW_KEY_F18 to Key.F18,
    GLFW_KEY_F19 to Key.F19,
    GLFW_KEY_F20 to Key.F20,
    GLFW_KEY_F21 to Key.F21,
    GLFW_KEY_F22 to Key.F22,
    GLFW_KEY_F23 to Key.F23,
    GLFW_KEY_F24 to Key.F24,
    GLFW_KEY_F25 to Key.F25,
    GLFW_KEY_KP_0 to Key.KP_0,
    GLFW_KEY_KP_1 to Key.KP_1,
    GLFW_KEY_KP_2 to Key.KP_2,
    GLFW_KEY_KP_3 to Key.KP_3,
    GLFW_KEY_KP_4 to Key.KP_4,
    GLFW_KEY_KP_5 to Key.KP_5,
    GLFW_KEY_KP_6 to Key.KP_6,
    GLFW_KEY_KP_7 to Key.KP_7,
    GLFW_KEY_KP_8 to Key.KP_8,
    GLFW_KEY_KP_9 to Key.KP_9,
    GLFW_KEY_KP_DECIMAL to Key.KP_DECIMAL,
    GLFW_KEY_KP_DIVIDE to Key.KP_DIVIDE,
    GLFW_KEY_KP_MULTIPLY to Key.KP_MULTIPLY,
    GLFW_KEY_KP_SUBTRACT to Key.KP_SUBTRACT,
    GLFW_KEY_KP_ADD to Key.KP_ADD,
    GLFW_KEY_KP_ENTER to Key.KP_ENTER,
    GLFW_KEY_KP_EQUAL to Key.KP_EQUAL,
    GLFW_KEY_LEFT_SHIFT to Key.LEFT_SHIFT,
    GLFW_KEY_LEFT_CONTROL to Key.LEFT_CONTROL,
    GLFW_KEY_LEFT_ALT to Key.LEFT_ALT,
    GLFW_KEY_LEFT_SUPER to Key.LEFT_SUPER,
    GLFW_KEY_RIGHT_SHIFT to Key.RIGHT_SHIFT,
    GLFW_KEY_RIGHT_CONTROL to Key.RIGHT_CONTROL,
    GLFW_KEY_RIGHT_ALT to Key.RIGHT_ALT,
    GLFW_KEY_RIGHT_SUPER to Key.RIGHT_SUPER,
    GLFW_KEY_MENU to Key.MENU
)

actual val Kml: KmlBase = KmlBaseJvm

class BufferedImageKmlNativeImageData(val buffered: BufferedImage) : KmlNativeImageData {
    override val width: Int get() = buffered.width
    override val height: Int get() = buffered.height
    val bytes = (buffered.raster.dataBuffer as DataBufferByte).data

    init {
        //for (y in 0 until 32) {
        //    val rowSize = 32 * 4
        //    val index = y * rowSize
        //    println(bytes.sliceArray(index + 32 until index + 64).toList())
        //}

        for (n in 0 until bytes.size step 4) {
            val v0 = bytes[n + 0]
            val v1 = bytes[n + 1]
            val v2 = bytes[n + 2]
            val v3 = bytes[n + 3]
            bytes[n + 0] = v3
            bytes[n + 1] = v2
            bytes[n + 2] = v1
            bytes[n + 3] = v0
        }
    }

    val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
        clear()
        put(bytes)
        //println("BYTES: ${bytes.size}")
        flip()
    }
}

fun ByteBuffer.toTypedArray() = ByteArray(this.limit()).also { this.get(it) }
fun FloatBuffer.toTypedArray() = FloatArray(this.limit()).also { this.get(it) }
fun IntBuffer.toTypedArray() = IntArray(this.limit()).also { this.get(it) }

