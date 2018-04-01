package com.soywiz.kmedialayer.scene

import com.soywiz.kmedialayer.*

class SceneTexture(
	val tex: KmlGlTex,
	val leftInt: Int = 0,
	val topInt: Int = 0,
	val rightInt: Int = tex.width,
	val bottomInt: Int = tex.height
) {
	val fleft = leftInt.toFloat() / tex.width.toFloat()
	val fright = rightInt.toFloat() / tex.width.toFloat()

	val ftop = topInt.toFloat() / tex.height.toFloat()
	val fbottom = bottomInt.toFloat() / tex.height.toFloat()

	val fwidth = fright - fleft
	val fheight = fbottom - ftop

	val width get() = rightInt - leftInt
	val height get() = bottomInt - topInt

	fun sliceBounds(left: Int, top: Int, right: Int, bottom: Int): SceneTexture =
		SceneTexture(tex, leftInt + left, topInt + top, leftInt + right, topInt + bottom)

	fun sliceSize(x: Int, y: Int, width: Int, height: Int): SceneTexture = sliceBounds(x, y, x + width, y + height)
}