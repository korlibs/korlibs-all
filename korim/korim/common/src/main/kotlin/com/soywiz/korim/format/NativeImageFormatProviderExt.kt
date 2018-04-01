package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.vector.*

suspend fun Bitmap.showImageAndWaitExt() = showImageAndWait(this)
suspend fun ImageData.showImagesAndWaitExt() = showImagesAndWait(this)

suspend fun showImageAndWait(image: Bitmap): Unit {
	println("Showing... $image")
	nativeImageFormatProvider.display(image)
}

suspend fun showImageAndWait(image: Context2d.SizedDrawable): Unit {
	showImageAndWait(image.render().toBmp32())
}

suspend fun showImagesAndWait(image: ImageData) {
	for (frame in image.frames) {
		showImageAndWait(frame.bitmap)
	}
}
