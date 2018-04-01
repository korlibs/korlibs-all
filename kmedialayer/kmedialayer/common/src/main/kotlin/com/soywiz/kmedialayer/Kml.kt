package com.soywiz.kmedialayer

import com.soywiz.kmedialayer.scene.util.*
import kotlin.coroutines.experimental.*

data class WindowConfig(
    val width: Int = 640,
    val height: Int = 480,
    val title: String = "KMediaLayer",
    val bgcolor: List<Float> = listOf(0.2f, 0.4f, 0.6f, 1f)
)

open class CancelException(val complete: Boolean = false) : RuntimeException()

interface Job<T> {
    fun cancel(exception: CancelException = CancelException())

    suspend fun await(): T
}

// Just like promise pipe
fun <T, R> Job<T>.then(callback: suspend (T) -> R): Job<R> = com.soywiz.kmedialayer.launch {
    callback(this@then.await())
}

class CancellationToken(val cancel: Signal<Throwable> = Signal()) : AbstractCoroutineContextElement(KEY) {
    object KEY : CoroutineContext.Key<CancellationToken>
}

public suspend inline fun <T> suspendCoroutineCancellable(crossinline block: (Continuation<T>, cancel: Signal<Throwable>) -> Unit): T {
    return suspendCoroutine<T> { c ->
        val cancelToken = c.context[CancellationToken.KEY]
        if (cancelToken == null) {
            println("WARNING: No CancellationToken in the suspendContext")
            //throw IllegalStateException("No CancellationToken in the suspendContext")
        }
        block(c, cancelToken?.cancel ?: Signal())
    }
}

class JobQueue(val context: CoroutineContext = EmptyCoroutineContext) {
    private val tasks = arrayListOf<suspend () -> Unit>()
    var running = false; private set
    private var currentJob: Job<Unit>? = null
    val size: Int get() = tasks.size + (if (running) 1 else 0)

    private suspend fun run() {
        running = true
        try {

            while (true) {
                val task = synchronized(tasks) { if (tasks.isNotEmpty()) tasks.removeAt(0) else null } ?: break
                val job = launch { task() }
                currentJob = job
                job.await()
                currentJob = null
            }
        } catch (e: Throwable) {
            println(e)
        } finally {
            currentJob = null
            running = false
        }
    }

    /**
     * Discards all the queued but non running tasks
     */
    fun discard(): JobQueue {
        synchronized(tasks) { tasks.clear() }
        return this
    }

    /**
     * Discards all the queued tasks and cancels the running one, sending a complete signal.
     * If complete=true, a tween for example will be set directly to the end step
     * If complete=false, a tween for example will stop to the current step
     */
    fun cancel(complete: Boolean = false): JobQueue {
        currentJob?.cancel(CancelException(complete))
        return this
    }

    fun cancelComplete() = cancel(true)

    fun queue(callback: suspend () -> Unit) {
        synchronized(tasks) { tasks += callback }
        if (!running) launch { run() }
    }

    operator fun invoke(callback: suspend () -> Unit) = queue(callback)
}

class Deferred<T>(bcontext: CoroutineContext, val cancellationToken: CancellationToken) : Continuation<T>, Job<T> {
    companion object {
        private val pool = Pool({ clear() }) { arrayListOf<Continuation<Any>>() }
    }

    override val context = bcontext + cancellationToken
    private var done = false
    private var value: T? = null
    private var exception: Throwable? = null
    private val cc = arrayListOf<Continuation<T?>>()

    private fun pool() = pool as Pool<ArrayList<Continuation<T?>>>

    override fun resume(value: T) {
        done = true
        this.value = value
        flush()
    }

    override fun resumeWithException(exception: Throwable) {
        done = true
        this.exception = exception
        flush()
    }

    override fun cancel(exception: CancelException) {
        cancellationToken.cancel(exception)
    }

    override suspend fun await(): T = suspendCoroutineCancellable { c, cancel ->
        val rc = (c as Continuation<T?>)
        synchronized(cc) { cc += rc }
        cancel { cc -= rc }
        flush()
    }

    fun flush() {
        if (!done) return
        pool().use { temp ->
            synchronized(cc) { temp.addAll(cc) }
            for (c in temp) {
                try {
                    if (exception != null) {
                        c.resumeWithException(exception!!)
                    } else {
                        c.resume(value)
                    }
                } catch (e: IllegalStateException) {
                }
            }
        }
    }
}

fun <T> launchAndForget(context: CoroutineContext = EmptyCoroutineContext, callback: suspend () -> T): Unit {
    launch(context) { callback() }
}

fun <T> launch(context: CoroutineContext = EmptyCoroutineContext, callback: suspend () -> T): Job<T> {
    return Kml.launch(context, callback)
}

suspend fun parallel(vararg callbacks: suspend () -> Unit) {
    val jobs = callbacks.map { launch { it() } }
    for (job in jobs) job.await()
}

abstract class KmlBase {
    open fun <T> launch(context: CoroutineContext = EmptyCoroutineContext, callback: suspend () -> T): Job<T> {
        val cont = Deferred<T>(context, CancellationToken())
        callback.startCoroutine(cont)
        return cont
    }

    open fun <T : Any> runBlocking(context: CoroutineContext = EmptyCoroutineContext, callback: suspend () -> T): T {
        TODO("runBlocking not implemented in this platform: $this")
    }

    open fun application(windowConfig: WindowConfig, listener: KMLWindowListener) {
        TODO("KmlBase.application()")
    }

    open fun currentTimeMillis(): Double = TODO("KmlBase.currentTimeMillis")
    open suspend fun delay(ms: Int): Unit = TODO("KmlBase.delay()")

    open fun enqueue(task: () -> Unit): Unit {
        TODO("KmlBase.delay()")
    }

    open suspend fun decodeImage(path: String): KmlNativeImageData {
        return decodeImage(loadFileBytes(path))
    }

    open suspend fun decodeImage(data: ByteArray): KmlNativeImageData {
        TODO("KmlBase.decodeImage(ByteArray)")
    }

    open suspend fun loadFileBytes(path: String, range: LongRange? = null): ByteArray {
        TODO("KmlBase.loadFileBytes")
    }

    open suspend fun writeFileBytes(path: String, data: ByteArray, offset: Long? = null): Unit {
        TODO("KmlBase.writeFileBytes")
    }
}

suspend fun KmlBase.loadFileString(path: String, charset: Charset = UTF8) = loadFileBytes(path).toString(charset)

expect val Kml: KmlBase

open class KMLWindowListener {
    open suspend fun init(gl: KmlGl): Unit = gl.run {
    }

    open fun render(gl: KmlGl): Unit = gl.run {
        clearColor(1f, 0f, 1f, 1f)
        clear(COLOR_BUFFER_BIT)
    }

    open fun keyUpdate(key: Key, pressed: Boolean) {
    }

    open fun gamepadConnection(player: Int, name: String, connected: Boolean) {
    }

    open fun gamepadButtonUpdate(player: Int, button: GameButton, ratio: Double) {
    }

    open fun gamepadStickUpdate(player: Int, stick: GameStick, x: Double, y: Double) {
    }

    open fun mouseUpdateMove(x: Int, y: Int) {
    }

    open fun mouseUpdateButton(button: Int, pressed: Boolean) {
    }

    open fun resized(width: Int, height: Int) {
    }
}

enum class GameStick(val id: Int) {
    LEFT(0), RIGHT(1);

    companion object {
        val STICKS = values()
    }
}

enum class GameButton(val id: Int) {
    UP(0), LEFT(1), RIGHT(2), DOWN(3),
    BUTTON1(4), BUTTON2(5), BUTTON3(6), BUTTON4(7), BUTTON5(8), BUTTON6(9),
    SELECT(10), START(11),
    LEFT_THUMB(12), RIGHT_THUMB(13),
    LEFT_TRIGGER1(14), LEFT_TRIGGER2(15),
    RIGHT_TRIGGER1(16), RIGHT_TRIGGER2(17);

    companion object {
        val BUTTONS = values()
        val MAX = BUTTONS.size
    }
}

enum class Key {
    SPACE, APOSTROPHE, COMMA, MINUS, PERIOD, SLASH,
    N0, N1, N2, N3, N4, N5, N6, N7, N8, N9,
    SEMICOLON, EQUAL,
    A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
    LEFT_BRACKET, BACKSLASH, RIGHT_BRACKET, GRAVE_ACCENT,
    WORLD_1, WORLD_2,
    ESCAPE,
    ENTER, TAB, BACKSPACE, INSERT, DELETE,
    RIGHT, LEFT, DOWN, UP,
    PAGE_UP, PAGE_DOWN,
    HOME, END,
    CAPS_LOCK, SCROLL_LOCK, NUM_LOCK,
    PRINT_SCREEN, PAUSE,
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
    F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25,
    KP_0, KP_1, KP_2, KP_3, KP_4, KP_5, KP_6, KP_7, KP_8, KP_9,
    KP_DECIMAL, KP_DIVIDE, KP_MULTIPLY,
    KP_SUBTRACT, KP_ADD, KP_ENTER, KP_EQUAL,
    LEFT_SHIFT, LEFT_CONTROL, LEFT_ALT, LEFT_SUPER,
    RIGHT_SHIFT, RIGHT_CONTROL, RIGHT_ALT, RIGHT_SUPER,
    MENU,

    UNDERLINE, SELECT_KEY,

    UNKNOWN
}

interface KmlNativeImageData {
    val width: Int
    val height: Int
}

abstract class KmlBaseNoEventLoop : KmlBase() {
    val timers = Timers()

    override suspend fun delay(ms: Int): Unit = suspendCoroutineCancellable { c, cancel ->
        val timer = timers.add(ms) { c.resume(Unit) }
        cancel { timers.remove(timer) }
    }

    override fun enqueue(task: () -> Unit) {
        timers.add(task)
    }

    override fun <T : Any> runBlocking(context: CoroutineContext, callback: suspend () -> T): T {
        var done = false
        lateinit var resultValue: T
        var resultException: Throwable? = null
        //println("runBlocking[a]")
        callback.startCoroutine(object : Continuation<T> {
            override val context: CoroutineContext = context
            override fun resume(value: T) {
                resultValue = value
                done = true
            }

            override fun resumeWithException(exception: Throwable) {
                println(exception)
                resultException = exception
                done = true
            }
        })
        //println("runBlocking[b]")
        while (!done) {
            //println("runBlocking[c]")
            sleep(1)
            //Timers.check()
            //println("runBlocking[d]")
            pollEvents()
            //println("runBlocking[e]")
            timers.check()
            //println("runBlocking[f]")
        }
        if (resultException != null) throw resultException!!
        return resultValue
    }

    abstract fun sleep(time: Int)
    abstract fun pollEvents()

    inner class Timers {
        inner class Timer(val start: Double, val callback: () -> Unit)

        private val tempTimers = arrayListOf<Timer>()
        private val timers = arrayListOf<Timer>()

        private val tempTasks = arrayListOf<() -> Unit>()
        private val tasks = arrayListOf<() -> Unit>()

        fun add(ms: Int, callback: () -> Unit): Timer {
            return Timer(currentTimeMillis() + ms, callback).apply { timers += this }
        }

        fun add(callback: () -> Unit) {
            tasks += callback
        }

        fun remove(timer: Timer) {
            timers -= timer
        }

        fun check() {
            // Timer events
            val now = currentTimeMillis()
            tempTimers.clear()
            tempTimers.addAll(timers)
            for (timer in tempTimers) {
                if (now >= timer.start) {
                    timer.callback()
                    timers.remove(timer)
                }
            }
            // Queued tasks
            tempTasks.clear()
            tempTasks.addAll(tasks)
            tasks.clear()
            for (task in tempTasks) {
                task()
            }
        }
    }
}
