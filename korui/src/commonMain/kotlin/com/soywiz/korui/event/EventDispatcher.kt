package com.soywiz.korui.event

import com.soywiz.kds.*
import com.soywiz.korio.lang.*
import kotlin.reflect.*

interface EventDispatcher {
	fun <T : Event> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Closeable
	fun <T : Event> dispatch(clazz: KClass<T>, event: T)
	fun copyFrom(other: EventDispatcher) = Unit

	open class Mixin : EventDispatcher {
		private val handlers = LinkedHashMap<KClass<out Event>, ArrayList<(Event) -> Unit>>()

		private fun <T : Event> getHandlersFor(clazz: KClass<T>): ArrayList<(T) -> Unit> {
			@Suppress("UNCHECKED_CAST")
			return handlers.getOrPut(clazz) { arrayListOf() } as ArrayList<(T) -> Unit>
		}

		override fun <T : Event> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Closeable {
			getHandlersFor(clazz) += handler
			return Closeable { getHandlersFor(clazz) -= handler }
		}

		override fun copyFrom(other: EventDispatcher) {
			handlers.clear()
			if (other is EventDispatcher.Mixin) {
				for ((clazz, events) in other.handlers) {
					//println("EventDispatcher.copyFrom($clazz, $events)")
					for (event in events) {
						addEventListener(clazz, event)
					}
				}
			}
		}

		private val tempHandlers = Pool<ArrayList<(Event) -> Unit>>(reset = { it.clear() }) { arrayListOf() }

		override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
			tempHandlers.alloc { temp ->
				//try {
					@Suppress("UNCHECKED_CAST")
					val rtemp = temp as ArrayList<(T) -> Unit>
					rtemp += getHandlersFor(clazz)
					for (handler in rtemp) {
						handler(event)
					}
				//} catch (e: PreventDefaultException) {
				//	// Do nothing
				//}
			}
		}
	}

	companion object {
		operator fun invoke(): EventDispatcher = EventDispatcher.Mixin()
	}
}

object DummyEventDispatcher : EventDispatcher, Closeable {
	override fun close() {
	}

	override fun <T : Event> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Closeable {
		return this
	}

	override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
	}
}


inline fun <reified T : Event> EventDispatcher.addEventListener(noinline handler: (T) -> Unit) = addEventListener(T::class, handler)
inline fun <reified T : Event> EventDispatcher.dispatch(event: T) = dispatch(T::class, event)

open class Event {
	var target: Any? = null
}

fun preventDefault(reason: Any? = null): Nothing = throw PreventDefaultException(reason)

class PreventDefaultException(val reason: Any? = null) : Exception()

/*

//interface Cancellable

interface EventDispatcher {
	class Mixin : EventDispatcher {
		val events = hashMapOf<KClass<*>, ArrayList<(Any) -> Unit>>()

		override fun <T : Any> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Cancellable {
			val handlers = events.getOrPut(clazz) { arrayListOf() }
			val chandler = handler as ((Any) -> Unit)
			handlers += chandler
			return Cancellable { handlers -= chandler }
		}

		override fun <T : Any> dispatch(event: T, clazz: KClass<out T>) {
			val handlers = events[clazz]
			if (handlers != null) {
				for (handler in handlers.toList()) {
					handler(event)
				}
			}
		}

	}

	fun <T : Any> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Cancellable
	fun <T : Any> dispatch(event: T, clazz: KClass<out T> = event::class): Unit
}

interface Event

inline fun <reified T : Any> EventDispatcher.addEventListener(noinline handler: (T) -> Unit): Cancellable =
	this.addEventListener(T::class, handler)

inline suspend fun <reified T : Any> EventDispatcher.addEventListenerSuspend(noinline handler: suspend (T) -> Unit): Cancellable {
	val context = getCoroutineContext()
	return this.addEventListener(T::class) { event ->
		context.go {
			handler(event)
		}
	}
}

/*
class ED : EventDispatcher by EventDispatcher.Mixin() {
	override fun dispatch(event: Any) {
		//super.dispatch(event) // ERROR!
		println("dispatched: $event!")
	}
}

open class ED1 : EventDispatcher by EventDispatcher.Mixin()
open class ED2 : ED1() {
	override fun dispatch(event: Any) {
		super.dispatch(event) // WORKS
		println("dispatched: $event!")
	}
}



class ED2(val ed: EventDispatcher = EventDispatcher.Mixin()) : EventDispatcher by ed {
	override fun dispatch(event: Any) {
		ed.dispatch(event)
		println("dispatched: $event!")
	}
}
*/

 */