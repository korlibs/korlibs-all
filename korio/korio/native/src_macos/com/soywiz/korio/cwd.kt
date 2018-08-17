package com.soywiz.korio

fun nativeCwd(): String {
	//val out = platform.Foundation.NSBundle.mainBundle.bundleURL.path ?: "."
	//return if (out.endsWith(".app/")) "$out/Contents/Resources" else out
	return platform.Foundation.NSBundle.mainBundle.resourcePath ?: "."
}

//fun nativeCwd(): String {
//	return memScoped {
//		val data = allocArray<ByteVar>(1024)
//		getcwd(data, 1024)
//		data.toKString()
//	}
//}
