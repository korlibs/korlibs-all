package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korma.*

interface Renderable {
	fun render(ctx: RenderContext, m: Matrix2d): Unit
}
