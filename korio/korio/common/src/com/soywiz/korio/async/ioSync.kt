import com.soywiz.korio.error.*
import kotlin.coroutines.experimental.*

/**
 * Allows to execute a suspendable block as long as you can ensure no suspending will happen at all..
 */
fun <T : Any> ioSync(callback: suspend () -> T): T {
	var completed = false
	lateinit var result: T
	var resultEx: Throwable? = null
	callback.startCoroutine(object : Continuation<T> {
		override val context: CoroutineContext = EmptyCoroutineContext
		override fun resume(value: T) = run { result = value; completed = true }
		override fun resumeWithException(exception: Throwable) = run { resultEx = exception; completed = true }
	})
	if (!completed) invalidOp("ioSync was not completed synchronously!")
	if (resultEx != null) throw resultEx!!
	return result
}
