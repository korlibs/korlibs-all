package com.soywiz.korui

import com.soywiz.korio.async.*
import com.soywiz.korui.light.*
import com.soywiz.korio.*

actual object KoruiEventLoop {
	actual val instance: EventLoop by lazy { SdlEventLoop() }
}

class SdlEventLoop : BaseEventLoopNative() {
	override fun start() {

	}

	override fun nativeSleep(time: Int) {
		KorioNative.Thread_sleep(time.toLong())
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
