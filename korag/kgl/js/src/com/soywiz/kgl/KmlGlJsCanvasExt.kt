@file:Suppress("USELESS_CAST")

package com.soywiz.kgl

import com.soywiz.kmem.*
import org.khronos.webgl.*
import org.w3c.dom.*

val KmlNativeBuffer.arrayBuffer: org.khronos.webgl.ArrayBuffer get() = (this.mem as org.khronos.webgl.ArrayBuffer)
val KmlNativeBuffer.arrayUByte: Uint8Array get() = Uint8Array(this.mem)

class KmlImgNativeImageData(val img: HTMLImageElement) : KmlNativeImageData {
	override val width: Int get() = img.width
	override val height: Int get() = img.height
}
