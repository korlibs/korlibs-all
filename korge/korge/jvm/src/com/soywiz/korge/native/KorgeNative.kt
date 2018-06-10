package com.soywiz.korge.native

import java.lang.management.*
import kotlin.reflect.*

// Check in OSX -XstartOnFirstThread
// https://github.com/LWJGL/lwjgl3/blob/2a69bebff7bb03fb0c2452ce9d219668efc007e5/modules/lwjgl/glfw/src/main/java/org/lwjgl/glfw/EventLoop.java#L80
actual object KorgeNative {
	actual fun getClassSimpleName(clazz: KClass<*>): String = clazz.java.simpleName ?: ""
	actual fun checkEnvironment(): Unit {
		val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
		val arguments = runtimeMxBean.inputArguments
		//if (OS.isMac) {
		//if ()
		//}

		// Thread.currentThread().id:13
		//println("Thread.currentThread().id:" + Thread.currentThread().id)
		//println("arguments:$arguments")
		//if (arguments.joinToString(" ").toLowerCase().contains("-XstartOnFirstThread".toLowerCase())) {
		//	error("-XstartOnFirstThread would hang the application")
		//}
		//println(Thread.currentThread())
		//println("Thread.activeCount():" + Thread.activeCount())
		////Thread.dumpStack()
		//println("Thread.getAllStackTraces()")
		//println("Threads:" + Thread.getAllStackTraces().keys.size)
		//println("Threads:" + Thread.getAllStackTraces().keys)
		//println("Thread.currentThread().threadGroup:" + Thread.currentThread().threadGroup)

		// Threads:[Thread[AWT-Shutdown,5,system], Thread[Finalizer,8,system], Thread[DestroyJavaVM,5,main], Thread[Signal Dispatcher,9,system], Thread[AWT-EventQueue-0,6,main], Thread[AppKit Thread,5,system], Thread[Reference Handler,10,system]]
	}
}
