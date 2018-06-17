# KMediaLayer

Offers an OpenGL/OpenGL|ES compatible interface and some functions for windowing, input and image loading as common.

Supports:

* Android (**NOT WORKING YET** *unsupported*)
* JS (**webgl**)
* JVM (**lwjgl3**)
* NATIVE-WINDOWS (**WIN32, GDI+, WGL without additional dependencies**)
* NATIVE-MACOS (**Cocoa without additional dependencies**)

The intended normal workflow is to prototype using the JVM version,
and then generate native executables or the JS version for the web.  

## Low level API:

```kotlin
object Kml {
    fun application(windowConfig: WindowConfig, listener: KMLWindowListener)

    fun <T> launch(context: CoroutineContext = EmptyCoroutineContext, callback: suspend () -> T): Job<T>
    fun currentTimeMillis(): Double
    suspend fun delay(ms: Int): Unit
    fun enqueue(task: () -> Unit): Unit
    suspend fun decodeImage(path: String): KmlNativeImageData
    suspend fun decodeImage(data: ByteArray): KmlNativeImageData
    suspend fun loadFileBytes(path: String, range: LongRange? = null): ByteArray
    suspend fun writeFileBytes(path: String, data: ByteArray, offset: Long? = null): Unit
}
```

The "application":

```kotlin
open class KMLWindowListener {
    open suspend fun init(gl: KmlGl): Unit
    open fun render(gl: KmlGl): Unit
    open fun keyUpdate(key: Key, pressed: Boolean)
    open fun gamepadUpdate(button: Int, pressed: Boolean, ratio: Double)
    open fun mouseUpdateMove(x: Int, y: Int)
    open fun mouseUpdateButton(button: Int, pressed: Boolean)
    open fun resized(width: Int, height: Int)
}
```

The window configuration:

```kotlin
data class WindowConfig(
    val width: Int = 640,
    val height: Int = 480,
    val title: String = "KMediaLayer"
)
```

```kotlin
// Offers the webgl API without the gl prefix:
interface KmlGl {
    // ...
    fun drawArrays(mode: Int, first: Int, count: Int): Unit
    fun drawElements(mode: Int, count: Int, type: Int, indices: Int): Unit
    fun enable(cap: Int): Unit
    fun enableVertexAttribArray(index: Int): Unit
    fun finish(): Unit
    fun flush(): Unit
    // ...
}
```

## Scene API

The Scene API offers an API similar to the Swift's SpriteKit, Flash or Unity. There is a tree of view nodes,
each node has properties and allow to attach components. There are components for handling mouse and key events

### JobQueue

JobQueue exposes an API to enqueue asynchronous processes.

```kotlin
val queue = JobQueue()
queue { view.moveBy(16.0, 16.0, time = 0.5) } 
```

Depending on the task in hand you can enqueue more tasks, or discard some of the elements in the queue.

```
queue.discard() // Discard all the tasks in the queue that has not started
queue.cancel()  // Discards all the tasks and cancels the current task 
queue.cancel(complete = true) // Discards all the tasks and cancels the current task, signaling with a complete = true
```

Signaling cancelations with `complete = true` makes some task like tweening to jump to the target state.
