package com.soywiz.korag.shader.gl

import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Shader
import com.soywiz.korag.shader.ShaderType
import com.soywiz.korag.shader.VertexShader

fun Shader.toNewGlslString(gles: Boolean = true, version: Int = 100) = GlslGenerator(this.type, gles, version).generate(this.stm)
fun VertexShader.toGlSlString(gles: Boolean = true) = GlslGenerator(ShaderType.VERTEX, gles).generate(this.stm)
fun FragmentShader.toGlSlString(gles: Boolean = true) = GlslGenerator(ShaderType.FRAGMENT, gles).generate(this.stm)