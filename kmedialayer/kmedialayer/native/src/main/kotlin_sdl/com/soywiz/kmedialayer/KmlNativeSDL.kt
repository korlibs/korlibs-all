package com.soywiz.kmedialayer

import kotlinx.cinterop.*
import platform.posix.*
import sdl2.*
import sdl2_image.*

private lateinit var globalListener: KMLWindowListener
private val glNative: KmlGlNative by lazy { KmlGlNative() }

object KmlBaseNative : KmlBaseNoEventLoop() {
    fun get_SDL_Error() = SDL_GetError()!!.toKString()
    var window: CPointer<SDL_Window>? = null
    var glcontext: SDL_GLContext? = null
    var running = true

    override fun application(windowConfig: WindowConfig, listener: KMLWindowListener) {
        globalListener = listener

        if (SDL_Init(SDL_INIT_EVERYTHING) != 0) {
            println("SDL_Init Error: ${get_SDL_Error()}")
            throw Error()
        }

        val platform = SDL_GetPlatform()!!.toKString()
        var displayWidth = 0
        var displayHeight = 0

        memScoped {
            val displayMode = alloc<SDL_DisplayMode>()
            if (SDL_GetCurrentDisplayMode(0, displayMode.ptr.reinterpret()) != 0) {
                println("SDL_GetCurrentDisplayMode Error: ${get_SDL_Error()}")
                SDL_Quit()
                throw Error()
            }
            displayWidth = displayMode.w
            displayHeight = displayMode.h
        }

        window = SDL_CreateWindow(
            windowConfig.title,
            SDL_WINDOWPOS_CENTERED,
            SDL_WINDOWPOS_CENTERED,
            windowConfig.width,
            windowConfig.height,
            SDL_WINDOW_OPENGL or SDL_WINDOW_RESIZABLE
        )
        if (window == null) {
            println("SDL_CreateWindow Error: ${get_SDL_Error()}")
            SDL_Quit()
            throw Error()
        }

        glcontext = SDL_GL_CreateContext(window);

        runBlocking {
            listener.init(glNative)
        }

        // now you can make GL calls.
        try {
            loop@ while (running) {
                globalListener.render(glNative)
                SDL_GL_SwapWindow(window);
                SDL_Delay(1000 / 60)
                pollEvents()
                timers.check()
            }
        } finally {
            SDL_GL_DeleteContext(glcontext);
            SDL_DestroyWindow(window)
            SDL_Quit()
        }
    }

    override fun currentTimeMillis(): Double = kotlin.system.getTimeMillis().toDouble()

    override fun sleep(time: Int) {
        SDL_Delay(time)
    }

    override fun pollEvents() {
        memScoped {
            val event = alloc<SDL_Event>()
            loop@ while (SDL_PollEvent(event.ptr.reinterpret()) != 0) {
                val eventType = event.type
                when (eventType) {
                    SDL_QUIT -> {
                        running = false
                    }
                    SDL_KEYDOWN, SDL_KEYUP -> {
                        val keyboardEvent = event.ptr.reinterpret<SDL_KeyboardEvent>().pointed
                        val mod = keyboardEvent.keysym.mod
                        val scancode = keyboardEvent.keysym.scancode
                        val keycode = keyboardEvent.keysym.sym
                        val key = KEYS[keycode] ?: Key.UNKNOWN
                        val pressed = eventType == SDL_KEYDOWN
                        //println("key: $scancode: $key (pressed=$pressed)")
                        globalListener.keyUpdate(key, pressed)
                    }
                    SDL_MOUSEBUTTONDOWN, SDL_MOUSEBUTTONUP, SDL_MOUSEMOTION -> {
                        val mouseEvent = event.ptr.reinterpret<SDL_MouseButtonEvent>().pointed
                        val x = mouseEvent.x.toInt()
                        val y = mouseEvent.y.toInt()
                        val button = mouseEvent.button.toInt()
                        when (eventType) {
                            SDL_MOUSEMOTION -> globalListener.mouseUpdateMove(x, y)
                            SDL_MOUSEBUTTONUP -> globalListener.mouseUpdateButton(button, false)
                            SDL_MOUSEBUTTONDOWN -> globalListener.mouseUpdateButton(button, true)
                        }
                    }
                    SDL_WINDOWEVENT -> {
                        val windowEventType = event.window.event
                        println("SDL_WINDOWEVENT: $windowEventType")
                        when (windowEventType.toInt()) {
                            SDL_WindowEventID.SDL_WINDOWEVENT_RESIZED.value -> {
                                println("RESIZED")
                                val width = event.window.data1
                                val height = event.window.data2
                                glNative.viewport(0, 0, width, height)
                                globalListener.resized(width, height)
                            }
                        }
                    }
                }
                //println("EVENT: $eventType")
            }
        }
    }

    override suspend fun decodeImage(data: ByteArray): KmlNativeImageData {
        //println("Decoding image of size: ${data.size}")
        if (data.size == 0) throw RuntimeException("Can't decode image of size 0")
        return memScoped {
            val dataPin = data.pin()
            val rw = SDL_RWFromConstMem(dataPin.addressOf(0), data.size)
            val imgRaw = IMG_Load_RW(rw.uncheckedCast(), 1)
            //val img = SDL_ConvertSurfaceFormat(imgRaw.uncheckedCast(), SDL_PIXELFORMAT_RGBA8888, 0)!!
            //val img = SDL_ConvertSurfaceFormat(imgRaw.uncheckedCast(), SDL_PIXELFORMAT_ARGB8888, 0)!!
            val img = SDL_ConvertSurfaceFormat(imgRaw.uncheckedCast(), SDL_PIXELFORMAT_ABGR8888, 0)!!
            SDL_FreeSurface(img)
            val width = img[0].w
            val height = img[0].h
            val pixels = img[0].pixels!!
            val out = IntArray(width * height)
            out.usePinned { outPinned ->
                memcpy(outPinned.addressOf(0), pixels, out.size * 4)
            }
            SDL_FreeSurface(imgRaw.uncheckedCast())
            dataPin.unpin()
            KmlNativeNativeImageData(width, height, out)
        }
    }

    override suspend fun loadFileBytes(path: String, range: LongRange?): ByteArray {
        return readBytes(path, range)
    }

    override suspend fun writeFileBytes(path: String, data: ByteArray, offset: Long?): Unit {
        TODO("KmlBase.writeFileBytes")
    }
}

actual val Kml: KmlBase = KmlBaseNative

private val KEYS = mapOf(
    SDLK_SPACE to Key.SPACE,
    SDLK_BACKSPACE to Key.BACKSPACE,
    SDLK_TAB to Key.TAB,
    SDLK_RETURN to Key.ENTER,
    -1 to Key.LEFT_SHIFT,
    -1 to Key.LEFT_CONTROL,
    -1 to Key.LEFT_ALT,
    -1 to Key.PAUSE,
    -1 to Key.CAPS_LOCK,
    SDLK_ESCAPE to Key.ESCAPE,
    SDLK_PAGEUP to Key.PAGE_UP,
    SDLK_PAGEDOWN to Key.PAGE_DOWN,
    SDLK_END to Key.END,
    SDLK_HOME to Key.HOME,
    SDLK_LEFT to Key.LEFT,
    SDLK_UP to Key.UP,
    SDLK_RIGHT to Key.RIGHT,
    SDLK_DOWN to Key.DOWN,
    SDLK_INSERT to Key.INSERT,
    SDLK_DELETE to Key.DELETE,
    SDLK_0 to Key.N0,
    SDLK_1 to Key.N1,
    SDLK_2 to Key.N2,
    SDLK_3 to Key.N3,
    SDLK_4 to Key.N4,
    SDLK_5 to Key.N5,
    SDLK_6 to Key.N6,
    SDLK_7 to Key.N7,
    SDLK_8 to Key.N8,
    SDLK_9 to Key.N9,
    SDLK_a to Key.A,
    SDLK_b to Key.B,
    SDLK_c to Key.C,
    SDLK_d to Key.D,
    SDLK_e to Key.E,
    SDLK_f to Key.F,
    SDLK_g to Key.G,
    SDLK_h to Key.H,
    SDLK_i to Key.I,
    SDLK_j to Key.J,
    SDLK_k to Key.K,
    SDLK_l to Key.L,
    SDLK_m to Key.M,
    SDLK_n to Key.N,
    SDLK_o to Key.O,
    SDLK_p to Key.P,
    SDLK_q to Key.Q,
    SDLK_r to Key.R,
    SDLK_s to Key.S,
    SDLK_t to Key.T,
    SDLK_u to Key.U,
    SDLK_v to Key.V,
    SDLK_w to Key.W,
    SDLK_x to Key.X,
    SDLK_y to Key.Y,
    SDLK_z to Key.Z,
    -1 to Key.LEFT_SUPER,
    -1 to Key.RIGHT_SUPER,
    -1 to Key.SELECT_KEY,
    SDLK_KP_0 to Key.KP_0,
    SDLK_KP_1 to Key.KP_1,
    SDLK_KP_2 to Key.KP_2,
    SDLK_KP_3 to Key.KP_3,
    SDLK_KP_4 to Key.KP_4,
    SDLK_KP_5 to Key.KP_5,
    SDLK_KP_6 to Key.KP_6,
    SDLK_KP_7 to Key.KP_7,
    SDLK_KP_8 to Key.KP_8,
    SDLK_KP_9 to Key.KP_9,
    -1 to Key.KP_MULTIPLY,
    -1 to Key.KP_ADD,
    -1 to Key.KP_SUBTRACT,
    -1 to Key.KP_DECIMAL,
    -1 to Key.KP_DIVIDE,
    SDLK_F1 to Key.F1,
    SDLK_F2 to Key.F2,
    SDLK_F3 to Key.F3,
    SDLK_F4 to Key.F4,
    SDLK_F5 to Key.F5,
    SDLK_F6 to Key.F6,
    SDLK_F7 to Key.F7,
    SDLK_F8 to Key.F8,
    SDLK_F9 to Key.F9,
    SDLK_F10 to Key.F10,
    SDLK_F11 to Key.F11,
    SDLK_F12 to Key.F12,
    -1 to Key.NUM_LOCK,
    -1 to Key.SCROLL_LOCK,
    -1 to Key.SEMICOLON,
    -1 to Key.EQUAL,
    -1 to Key.COMMA,
    -1 to Key.UNDERLINE,
    -1 to Key.PERIOD,
    -1 to Key.SLASH,
    -1 to Key.GRAVE_ACCENT,
    -1 to Key.LEFT_BRACKET,
    -1 to Key.BACKSLASH,
    -1 to Key.RIGHT_BRACKET,
    -1 to Key.APOSTROPHE
)
