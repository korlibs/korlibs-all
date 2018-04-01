// WARNING: File autogenerated DO NOT modify
// https://www.khronos.org/registry/OpenGL/api/GLES2/gl2.h
@file:Suppress("unused", "RedundantUnitReturnType")

package com.soywiz.kmedialayer

open class KmlGlProxy(val parent: KmlGl) : KmlGl() {
	open fun before(name: String, params: String): Unit = Unit
	open fun after(name: String, params: String, result: String): Unit = Unit
	override fun activeTexture(texture: Int): Unit {
		val sparams = "$texture"
		before("activeTexture", sparams)
		val res = parent.activeTexture(texture)
		after("activeTexture", sparams, "$res")
		return res
	}

	override fun attachShader(program: Int, shader: Int): Unit {
		val sparams = "$program, $shader"
		before("attachShader", sparams)
		val res = parent.attachShader(program, shader)
		after("attachShader", sparams, "$res")
		return res
	}

	override fun bindAttribLocation(program: Int, index: Int, name: String): Unit {
		val sparams = "$program, $index, $name"
		before("bindAttribLocation", sparams)
		val res = parent.bindAttribLocation(program, index, name)
		after("bindAttribLocation", sparams, "$res")
		return res
	}

	override fun bindBuffer(target: Int, buffer: Int): Unit {
		val sparams = "$target, $buffer"
		before("bindBuffer", sparams)
		val res = parent.bindBuffer(target, buffer)
		after("bindBuffer", sparams, "$res")
		return res
	}

	override fun bindFramebuffer(target: Int, framebuffer: Int): Unit {
		val sparams = "$target, $framebuffer"
		before("bindFramebuffer", sparams)
		val res = parent.bindFramebuffer(target, framebuffer)
		after("bindFramebuffer", sparams, "$res")
		return res
	}

	override fun bindRenderbuffer(target: Int, renderbuffer: Int): Unit {
		val sparams = "$target, $renderbuffer"
		before("bindRenderbuffer", sparams)
		val res = parent.bindRenderbuffer(target, renderbuffer)
		after("bindRenderbuffer", sparams, "$res")
		return res
	}

	override fun bindTexture(target: Int, texture: Int): Unit {
		val sparams = "$target, $texture"
		before("bindTexture", sparams)
		val res = parent.bindTexture(target, texture)
		after("bindTexture", sparams, "$res")
		return res
	}

	override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit {
		val sparams = "$red, $green, $blue, $alpha"
		before("blendColor", sparams)
		val res = parent.blendColor(red, green, blue, alpha)
		after("blendColor", sparams, "$res")
		return res
	}

	override fun blendEquation(mode: Int): Unit {
		val sparams = "$mode"
		before("blendEquation", sparams)
		val res = parent.blendEquation(mode)
		after("blendEquation", sparams, "$res")
		return res
	}

	override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit {
		val sparams = "$modeRGB, $modeAlpha"
		before("blendEquationSeparate", sparams)
		val res = parent.blendEquationSeparate(modeRGB, modeAlpha)
		after("blendEquationSeparate", sparams, "$res")
		return res
	}

	override fun blendFunc(sfactor: Int, dfactor: Int): Unit {
		val sparams = "$sfactor, $dfactor"
		before("blendFunc", sparams)
		val res = parent.blendFunc(sfactor, dfactor)
		after("blendFunc", sparams, "$res")
		return res
	}

	override fun blendFuncSeparate(sfactorRGB: Int, dfactorRGB: Int, sfactorAlpha: Int, dfactorAlpha: Int): Unit {
		val sparams = "$sfactorRGB, $dfactorRGB, $sfactorAlpha, $dfactorAlpha"
		before("blendFuncSeparate", sparams)
		val res = parent.blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha)
		after("blendFuncSeparate", sparams, "$res")
		return res
	}

	override fun bufferData(target: Int, size: Int, data: KmlNativeBuffer, usage: Int): Unit {
		val sparams = "$target, $size, $data, $usage"
		before("bufferData", sparams)
		val res = parent.bufferData(target, size, data, usage)
		after("bufferData", sparams, "$res")
		return res
	}

	override fun bufferSubData(target: Int, offset: Int, size: Int, data: KmlNativeBuffer): Unit {
		val sparams = "$target, $offset, $size, $data"
		before("bufferSubData", sparams)
		val res = parent.bufferSubData(target, offset, size, data)
		after("bufferSubData", sparams, "$res")
		return res
	}

	override fun checkFramebufferStatus(target: Int): Int {
		val sparams = "$target"
		before("checkFramebufferStatus", sparams)
		val res = parent.checkFramebufferStatus(target)
		after("checkFramebufferStatus", sparams, "$res")
		return res
	}

	override fun clear(mask: Int): Unit {
		val sparams = "$mask"
		before("clear", sparams)
		val res = parent.clear(mask)
		after("clear", sparams, "$res")
		return res
	}

	override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit {
		val sparams = "$red, $green, $blue, $alpha"
		before("clearColor", sparams)
		val res = parent.clearColor(red, green, blue, alpha)
		after("clearColor", sparams, "$res")
		return res
	}

	override fun clearDepthf(d: Float): Unit {
		val sparams = "$d"
		before("clearDepthf", sparams)
		val res = parent.clearDepthf(d)
		after("clearDepthf", sparams, "$res")
		return res
	}

	override fun clearStencil(s: Int): Unit {
		val sparams = "$s"
		before("clearStencil", sparams)
		val res = parent.clearStencil(s)
		after("clearStencil", sparams, "$res")
		return res
	}

	override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit {
		val sparams = "$red, $green, $blue, $alpha"
		before("colorMask", sparams)
		val res = parent.colorMask(red, green, blue, alpha)
		after("colorMask", sparams, "$res")
		return res
	}

	override fun compileShader(shader: Int): Unit {
		val sparams = "$shader"
		before("compileShader", sparams)
		val res = parent.compileShader(shader)
		after("compileShader", sparams, "$res")
		return res
	}

	override fun compressedTexImage2D(
		target: Int,
		level: Int,
		internalformat: Int,
		width: Int,
		height: Int,
		border: Int,
		imageSize: Int,
		data: KmlNativeBuffer
	): Unit {
		val sparams = "$target, $level, $internalformat, $width, $height, $border, $imageSize, $data"
		before("compressedTexImage2D", sparams)
		val res = parent.compressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data)
		after("compressedTexImage2D", sparams, "$res")
		return res
	}

	override fun compressedTexSubImage2D(
		target: Int,
		level: Int,
		xoffset: Int,
		yoffset: Int,
		width: Int,
		height: Int,
		format: Int,
		imageSize: Int,
		data: KmlNativeBuffer
	): Unit {
		val sparams = "$target, $level, $xoffset, $yoffset, $width, $height, $format, $imageSize, $data"
		before("compressedTexSubImage2D", sparams)
		val res =
			parent.compressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data)
		after("compressedTexSubImage2D", sparams, "$res")
		return res
	}

	override fun copyTexImage2D(
		target: Int,
		level: Int,
		internalformat: Int,
		x: Int,
		y: Int,
		width: Int,
		height: Int,
		border: Int
	): Unit {
		val sparams = "$target, $level, $internalformat, $x, $y, $width, $height, $border"
		before("copyTexImage2D", sparams)
		val res = parent.copyTexImage2D(target, level, internalformat, x, y, width, height, border)
		after("copyTexImage2D", sparams, "$res")
		return res
	}

	override fun copyTexSubImage2D(
		target: Int,
		level: Int,
		xoffset: Int,
		yoffset: Int,
		x: Int,
		y: Int,
		width: Int,
		height: Int
	): Unit {
		val sparams = "$target, $level, $xoffset, $yoffset, $x, $y, $width, $height"
		before("copyTexSubImage2D", sparams)
		val res = parent.copyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
		after("copyTexSubImage2D", sparams, "$res")
		return res
	}

	override fun createProgram(): Int {
		val sparams = ""
		before("createProgram", sparams)
		val res = parent.createProgram()
		after("createProgram", sparams, "$res")
		return res
	}

	override fun createShader(type: Int): Int {
		val sparams = "$type"
		before("createShader", sparams)
		val res = parent.createShader(type)
		after("createShader", sparams, "$res")
		return res
	}

	override fun cullFace(mode: Int): Unit {
		val sparams = "$mode"
		before("cullFace", sparams)
		val res = parent.cullFace(mode)
		after("cullFace", sparams, "$res")
		return res
	}

	override fun deleteBuffers(n: Int, items: KmlNativeBuffer): Unit {
		val sparams = "$n, $items"
		before("deleteBuffers", sparams)
		val res = parent.deleteBuffers(n, items)
		after("deleteBuffers", sparams, "$res")
		return res
	}

	override fun deleteFramebuffers(n: Int, items: KmlNativeBuffer): Unit {
		val sparams = "$n, $items"
		before("deleteFramebuffers", sparams)
		val res = parent.deleteFramebuffers(n, items)
		after("deleteFramebuffers", sparams, "$res")
		return res
	}

	override fun deleteProgram(program: Int): Unit {
		val sparams = "$program"
		before("deleteProgram", sparams)
		val res = parent.deleteProgram(program)
		after("deleteProgram", sparams, "$res")
		return res
	}

	override fun deleteRenderbuffers(n: Int, items: KmlNativeBuffer): Unit {
		val sparams = "$n, $items"
		before("deleteRenderbuffers", sparams)
		val res = parent.deleteRenderbuffers(n, items)
		after("deleteRenderbuffers", sparams, "$res")
		return res
	}

	override fun deleteShader(shader: Int): Unit {
		val sparams = "$shader"
		before("deleteShader", sparams)
		val res = parent.deleteShader(shader)
		after("deleteShader", sparams, "$res")
		return res
	}

	override fun deleteTextures(n: Int, items: KmlNativeBuffer): Unit {
		val sparams = "$n, $items"
		before("deleteTextures", sparams)
		val res = parent.deleteTextures(n, items)
		after("deleteTextures", sparams, "$res")
		return res
	}

	override fun depthFunc(func: Int): Unit {
		val sparams = "$func"
		before("depthFunc", sparams)
		val res = parent.depthFunc(func)
		after("depthFunc", sparams, "$res")
		return res
	}

	override fun depthMask(flag: Boolean): Unit {
		val sparams = "$flag"
		before("depthMask", sparams)
		val res = parent.depthMask(flag)
		after("depthMask", sparams, "$res")
		return res
	}

	override fun depthRangef(n: Float, f: Float): Unit {
		val sparams = "$n, $f"
		before("depthRangef", sparams)
		val res = parent.depthRangef(n, f)
		after("depthRangef", sparams, "$res")
		return res
	}

	override fun detachShader(program: Int, shader: Int): Unit {
		val sparams = "$program, $shader"
		before("detachShader", sparams)
		val res = parent.detachShader(program, shader)
		after("detachShader", sparams, "$res")
		return res
	}

	override fun disable(cap: Int): Unit {
		val sparams = "$cap"
		before("disable", sparams)
		val res = parent.disable(cap)
		after("disable", sparams, "$res")
		return res
	}

	override fun disableVertexAttribArray(index: Int): Unit {
		val sparams = "$index"
		before("disableVertexAttribArray", sparams)
		val res = parent.disableVertexAttribArray(index)
		after("disableVertexAttribArray", sparams, "$res")
		return res
	}

	override fun drawArrays(mode: Int, first: Int, count: Int): Unit {
		val sparams = "$mode, $first, $count"
		before("drawArrays", sparams)
		val res = parent.drawArrays(mode, first, count)
		after("drawArrays", sparams, "$res")
		return res
	}

	override fun drawElements(mode: Int, count: Int, type: Int, indices: Int): Unit {
		val sparams = "$mode, $count, $type, $indices"
		before("drawElements", sparams)
		val res = parent.drawElements(mode, count, type, indices)
		after("drawElements", sparams, "$res")
		return res
	}

	override fun enable(cap: Int): Unit {
		val sparams = "$cap"
		before("enable", sparams)
		val res = parent.enable(cap)
		after("enable", sparams, "$res")
		return res
	}

	override fun enableVertexAttribArray(index: Int): Unit {
		val sparams = "$index"
		before("enableVertexAttribArray", sparams)
		val res = parent.enableVertexAttribArray(index)
		after("enableVertexAttribArray", sparams, "$res")
		return res
	}

	override fun finish(): Unit {
		val sparams = ""
		before("finish", sparams)
		val res = parent.finish()
		after("finish", sparams, "$res")
		return res
	}

	override fun flush(): Unit {
		val sparams = ""
		before("flush", sparams)
		val res = parent.flush()
		after("flush", sparams, "$res")
		return res
	}

	override fun framebufferRenderbuffer(
		target: Int,
		attachment: Int,
		renderbuffertarget: Int,
		renderbuffer: Int
	): Unit {
		val sparams = "$target, $attachment, $renderbuffertarget, $renderbuffer"
		before("framebufferRenderbuffer", sparams)
		val res = parent.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
		after("framebufferRenderbuffer", sparams, "$res")
		return res
	}

	override fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit {
		val sparams = "$target, $attachment, $textarget, $texture, $level"
		before("framebufferTexture2D", sparams)
		val res = parent.framebufferTexture2D(target, attachment, textarget, texture, level)
		after("framebufferTexture2D", sparams, "$res")
		return res
	}

	override fun frontFace(mode: Int): Unit {
		val sparams = "$mode"
		before("frontFace", sparams)
		val res = parent.frontFace(mode)
		after("frontFace", sparams, "$res")
		return res
	}

	override fun genBuffers(n: Int, buffers: KmlNativeBuffer): Unit {
		val sparams = "$n, $buffers"
		before("genBuffers", sparams)
		val res = parent.genBuffers(n, buffers)
		after("genBuffers", sparams, "$res")
		return res
	}

	override fun generateMipmap(target: Int): Unit {
		val sparams = "$target"
		before("generateMipmap", sparams)
		val res = parent.generateMipmap(target)
		after("generateMipmap", sparams, "$res")
		return res
	}

	override fun genFramebuffers(n: Int, framebuffers: KmlNativeBuffer): Unit {
		val sparams = "$n, $framebuffers"
		before("genFramebuffers", sparams)
		val res = parent.genFramebuffers(n, framebuffers)
		after("genFramebuffers", sparams, "$res")
		return res
	}

	override fun genRenderbuffers(n: Int, renderbuffers: KmlNativeBuffer): Unit {
		val sparams = "$n, $renderbuffers"
		before("genRenderbuffers", sparams)
		val res = parent.genRenderbuffers(n, renderbuffers)
		after("genRenderbuffers", sparams, "$res")
		return res
	}

	override fun genTextures(n: Int, textures: KmlNativeBuffer): Unit {
		val sparams = "$n, $textures"
		before("genTextures", sparams)
		val res = parent.genTextures(n, textures)
		after("genTextures", sparams, "$res")
		return res
	}

	override fun getActiveAttrib(
		program: Int,
		index: Int,
		bufSize: Int,
		length: KmlNativeBuffer,
		size: KmlNativeBuffer,
		type: KmlNativeBuffer,
		name: KmlNativeBuffer
	): Unit {
		val sparams = "$program, $index, $bufSize, $length, $size, $type, $name"
		before("getActiveAttrib", sparams)
		val res = parent.getActiveAttrib(program, index, bufSize, length, size, type, name)
		after("getActiveAttrib", sparams, "$res")
		return res
	}

	override fun getActiveUniform(
		program: Int,
		index: Int,
		bufSize: Int,
		length: KmlNativeBuffer,
		size: KmlNativeBuffer,
		type: KmlNativeBuffer,
		name: KmlNativeBuffer
	): Unit {
		val sparams = "$program, $index, $bufSize, $length, $size, $type, $name"
		before("getActiveUniform", sparams)
		val res = parent.getActiveUniform(program, index, bufSize, length, size, type, name)
		after("getActiveUniform", sparams, "$res")
		return res
	}

	override fun getAttachedShaders(
		program: Int,
		maxCount: Int,
		count: KmlNativeBuffer,
		shaders: KmlNativeBuffer
	): Unit {
		val sparams = "$program, $maxCount, $count, $shaders"
		before("getAttachedShaders", sparams)
		val res = parent.getAttachedShaders(program, maxCount, count, shaders)
		after("getAttachedShaders", sparams, "$res")
		return res
	}

	override fun getAttribLocation(program: Int, name: String): Int {
		val sparams = "$program, $name"
		before("getAttribLocation", sparams)
		val res = parent.getAttribLocation(program, name)
		after("getAttribLocation", sparams, "$res")
		return res
	}

	override fun getUniformLocation(program: Int, name: String): Int {
		val sparams = "$program, $name"
		before("getUniformLocation", sparams)
		val res = parent.getUniformLocation(program, name)
		after("getUniformLocation", sparams, "$res")
		return res
	}

	override fun getBooleanv(pname: Int, data: KmlNativeBuffer): Unit {
		val sparams = "$pname, $data"
		before("getBooleanv", sparams)
		val res = parent.getBooleanv(pname, data)
		after("getBooleanv", sparams, "$res")
		return res
	}

	override fun getBufferParameteriv(target: Int, pname: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$target, $pname, $params"
		before("getBufferParameteriv", sparams)
		val res = parent.getBufferParameteriv(target, pname, params)
		after("getBufferParameteriv", sparams, "$res")
		return res
	}

	override fun getError(): Int {
		val sparams = ""
		before("getError", sparams)
		val res = parent.getError()
		after("getError", sparams, "$res")
		return res
	}

	override fun getFloatv(pname: Int, data: KmlNativeBuffer): Unit {
		val sparams = "$pname, $data"
		before("getFloatv", sparams)
		val res = parent.getFloatv(pname, data)
		after("getFloatv", sparams, "$res")
		return res
	}

	override fun getFramebufferAttachmentParameteriv(
		target: Int,
		attachment: Int,
		pname: Int,
		params: KmlNativeBuffer
	): Unit {
		val sparams = "$target, $attachment, $pname, $params"
		before("getFramebufferAttachmentParameteriv", sparams)
		val res = parent.getFramebufferAttachmentParameteriv(target, attachment, pname, params)
		after("getFramebufferAttachmentParameteriv", sparams, "$res")
		return res
	}

	override fun getIntegerv(pname: Int, data: KmlNativeBuffer): Unit {
		val sparams = "$pname, $data"
		before("getIntegerv", sparams)
		val res = parent.getIntegerv(pname, data)
		after("getIntegerv", sparams, "$res")
		return res
	}

	override fun getProgramInfoLog(
		program: Int,
		bufSize: Int,
		length: KmlNativeBuffer,
		infoLog: KmlNativeBuffer
	): Unit {
		val sparams = "$program, $bufSize, $length, $infoLog"
		before("getProgramInfoLog", sparams)
		val res = parent.getProgramInfoLog(program, bufSize, length, infoLog)
		after("getProgramInfoLog", sparams, "$res")
		return res
	}

	override fun getRenderbufferParameteriv(target: Int, pname: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$target, $pname, $params"
		before("getRenderbufferParameteriv", sparams)
		val res = parent.getRenderbufferParameteriv(target, pname, params)
		after("getRenderbufferParameteriv", sparams, "$res")
		return res
	}

	override fun getProgramiv(program: Int, pname: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$program, $pname, $params"
		before("getProgramiv", sparams)
		val res = parent.getProgramiv(program, pname, params)
		after("getProgramiv", sparams, "$res")
		return res
	}

	override fun getShaderiv(shader: Int, pname: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$shader, $pname, $params"
		before("getShaderiv", sparams)
		val res = parent.getShaderiv(shader, pname, params)
		after("getShaderiv", sparams, "$res")
		return res
	}

	override fun getShaderInfoLog(shader: Int, bufSize: Int, length: KmlNativeBuffer, infoLog: KmlNativeBuffer): Unit {
		val sparams = "$shader, $bufSize, $length, $infoLog"
		before("getShaderInfoLog", sparams)
		val res = parent.getShaderInfoLog(shader, bufSize, length, infoLog)
		after("getShaderInfoLog", sparams, "$res")
		return res
	}

	override fun getShaderPrecisionFormat(
		shadertype: Int,
		precisiontype: Int,
		range: KmlNativeBuffer,
		precision: KmlNativeBuffer
	): Unit {
		val sparams = "$shadertype, $precisiontype, $range, $precision"
		before("getShaderPrecisionFormat", sparams)
		val res = parent.getShaderPrecisionFormat(shadertype, precisiontype, range, precision)
		after("getShaderPrecisionFormat", sparams, "$res")
		return res
	}

	override fun getShaderSource(shader: Int, bufSize: Int, length: KmlNativeBuffer, source: KmlNativeBuffer): Unit {
		val sparams = "$shader, $bufSize, $length, $source"
		before("getShaderSource", sparams)
		val res = parent.getShaderSource(shader, bufSize, length, source)
		after("getShaderSource", sparams, "$res")
		return res
	}

	override fun getString(name: Int): String {
		val sparams = "$name"
		before("getString", sparams)
		val res = parent.getString(name)
		after("getString", sparams, "$res")
		return res
	}

	override fun getTexParameterfv(target: Int, pname: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$target, $pname, $params"
		before("getTexParameterfv", sparams)
		val res = parent.getTexParameterfv(target, pname, params)
		after("getTexParameterfv", sparams, "$res")
		return res
	}

	override fun getTexParameteriv(target: Int, pname: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$target, $pname, $params"
		before("getTexParameteriv", sparams)
		val res = parent.getTexParameteriv(target, pname, params)
		after("getTexParameteriv", sparams, "$res")
		return res
	}

	override fun getUniformfv(program: Int, location: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$program, $location, $params"
		before("getUniformfv", sparams)
		val res = parent.getUniformfv(program, location, params)
		after("getUniformfv", sparams, "$res")
		return res
	}

	override fun getUniformiv(program: Int, location: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$program, $location, $params"
		before("getUniformiv", sparams)
		val res = parent.getUniformiv(program, location, params)
		after("getUniformiv", sparams, "$res")
		return res
	}

	override fun getVertexAttribfv(index: Int, pname: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$index, $pname, $params"
		before("getVertexAttribfv", sparams)
		val res = parent.getVertexAttribfv(index, pname, params)
		after("getVertexAttribfv", sparams, "$res")
		return res
	}

	override fun getVertexAttribiv(index: Int, pname: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$index, $pname, $params"
		before("getVertexAttribiv", sparams)
		val res = parent.getVertexAttribiv(index, pname, params)
		after("getVertexAttribiv", sparams, "$res")
		return res
	}

	override fun getVertexAttribPointerv(index: Int, pname: Int, pointer: KmlNativeBuffer): Unit {
		val sparams = "$index, $pname, $pointer"
		before("getVertexAttribPointerv", sparams)
		val res = parent.getVertexAttribPointerv(index, pname, pointer)
		after("getVertexAttribPointerv", sparams, "$res")
		return res
	}

	override fun hint(target: Int, mode: Int): Unit {
		val sparams = "$target, $mode"
		before("hint", sparams)
		val res = parent.hint(target, mode)
		after("hint", sparams, "$res")
		return res
	}

	override fun isBuffer(buffer: Int): Boolean {
		val sparams = "$buffer"
		before("isBuffer", sparams)
		val res = parent.isBuffer(buffer)
		after("isBuffer", sparams, "$res")
		return res
	}

	override fun isEnabled(cap: Int): Boolean {
		val sparams = "$cap"
		before("isEnabled", sparams)
		val res = parent.isEnabled(cap)
		after("isEnabled", sparams, "$res")
		return res
	}

	override fun isFramebuffer(framebuffer: Int): Boolean {
		val sparams = "$framebuffer"
		before("isFramebuffer", sparams)
		val res = parent.isFramebuffer(framebuffer)
		after("isFramebuffer", sparams, "$res")
		return res
	}

	override fun isProgram(program: Int): Boolean {
		val sparams = "$program"
		before("isProgram", sparams)
		val res = parent.isProgram(program)
		after("isProgram", sparams, "$res")
		return res
	}

	override fun isRenderbuffer(renderbuffer: Int): Boolean {
		val sparams = "$renderbuffer"
		before("isRenderbuffer", sparams)
		val res = parent.isRenderbuffer(renderbuffer)
		after("isRenderbuffer", sparams, "$res")
		return res
	}

	override fun isShader(shader: Int): Boolean {
		val sparams = "$shader"
		before("isShader", sparams)
		val res = parent.isShader(shader)
		after("isShader", sparams, "$res")
		return res
	}

	override fun isTexture(texture: Int): Boolean {
		val sparams = "$texture"
		before("isTexture", sparams)
		val res = parent.isTexture(texture)
		after("isTexture", sparams, "$res")
		return res
	}

	override fun lineWidth(width: Float): Unit {
		val sparams = "$width"
		before("lineWidth", sparams)
		val res = parent.lineWidth(width)
		after("lineWidth", sparams, "$res")
		return res
	}

	override fun linkProgram(program: Int): Unit {
		val sparams = "$program"
		before("linkProgram", sparams)
		val res = parent.linkProgram(program)
		after("linkProgram", sparams, "$res")
		return res
	}

	override fun pixelStorei(pname: Int, param: Int): Unit {
		val sparams = "$pname, $param"
		before("pixelStorei", sparams)
		val res = parent.pixelStorei(pname, param)
		after("pixelStorei", sparams, "$res")
		return res
	}

	override fun polygonOffset(factor: Float, units: Float): Unit {
		val sparams = "$factor, $units"
		before("polygonOffset", sparams)
		val res = parent.polygonOffset(factor, units)
		after("polygonOffset", sparams, "$res")
		return res
	}

	override fun readPixels(
		x: Int,
		y: Int,
		width: Int,
		height: Int,
		format: Int,
		type: Int,
		pixels: KmlNativeBuffer
	): Unit {
		val sparams = "$x, $y, $width, $height, $format, $type, $pixels"
		before("readPixels", sparams)
		val res = parent.readPixels(x, y, width, height, format, type, pixels)
		after("readPixels", sparams, "$res")
		return res
	}

	override fun releaseShaderCompiler(): Unit {
		val sparams = ""
		before("releaseShaderCompiler", sparams)
		val res = parent.releaseShaderCompiler()
		after("releaseShaderCompiler", sparams, "$res")
		return res
	}

	override fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit {
		val sparams = "$target, $internalformat, $width, $height"
		before("renderbufferStorage", sparams)
		val res = parent.renderbufferStorage(target, internalformat, width, height)
		after("renderbufferStorage", sparams, "$res")
		return res
	}

	override fun sampleCoverage(value: Float, invert: Boolean): Unit {
		val sparams = "$value, $invert"
		before("sampleCoverage", sparams)
		val res = parent.sampleCoverage(value, invert)
		after("sampleCoverage", sparams, "$res")
		return res
	}

	override fun scissor(x: Int, y: Int, width: Int, height: Int): Unit {
		val sparams = "$x, $y, $width, $height"
		before("scissor", sparams)
		val res = parent.scissor(x, y, width, height)
		after("scissor", sparams, "$res")
		return res
	}

	override fun shaderBinary(
		count: Int,
		shaders: KmlNativeBuffer,
		binaryformat: Int,
		binary: KmlNativeBuffer,
		length: Int
	): Unit {
		val sparams = "$count, $shaders, $binaryformat, $binary, $length"
		before("shaderBinary", sparams)
		val res = parent.shaderBinary(count, shaders, binaryformat, binary, length)
		after("shaderBinary", sparams, "$res")
		return res
	}

	override fun shaderSource(shader: Int, string: String): Unit {
		val sparams = "$shader, $string"
		before("shaderSource", sparams)
		val res = parent.shaderSource(shader, string)
		after("shaderSource", sparams, "$res")
		return res
	}

	override fun stencilFunc(func: Int, ref: Int, mask: Int): Unit {
		val sparams = "$func, $ref, $mask"
		before("stencilFunc", sparams)
		val res = parent.stencilFunc(func, ref, mask)
		after("stencilFunc", sparams, "$res")
		return res
	}

	override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit {
		val sparams = "$face, $func, $ref, $mask"
		before("stencilFuncSeparate", sparams)
		val res = parent.stencilFuncSeparate(face, func, ref, mask)
		after("stencilFuncSeparate", sparams, "$res")
		return res
	}

	override fun stencilMask(mask: Int): Unit {
		val sparams = "$mask"
		before("stencilMask", sparams)
		val res = parent.stencilMask(mask)
		after("stencilMask", sparams, "$res")
		return res
	}

	override fun stencilMaskSeparate(face: Int, mask: Int): Unit {
		val sparams = "$face, $mask"
		before("stencilMaskSeparate", sparams)
		val res = parent.stencilMaskSeparate(face, mask)
		after("stencilMaskSeparate", sparams, "$res")
		return res
	}

	override fun stencilOp(fail: Int, zfail: Int, zpass: Int): Unit {
		val sparams = "$fail, $zfail, $zpass"
		before("stencilOp", sparams)
		val res = parent.stencilOp(fail, zfail, zpass)
		after("stencilOp", sparams, "$res")
		return res
	}

	override fun stencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int): Unit {
		val sparams = "$face, $sfail, $dpfail, $dppass"
		before("stencilOpSeparate", sparams)
		val res = parent.stencilOpSeparate(face, sfail, dpfail, dppass)
		after("stencilOpSeparate", sparams, "$res")
		return res
	}

	override fun texImage2D(
		target: Int,
		level: Int,
		internalformat: Int,
		width: Int,
		height: Int,
		border: Int,
		format: Int,
		type: Int,
		pixels: KmlNativeBuffer
	): Unit {
		val sparams = "$target, $level, $internalformat, $width, $height, $border, $format, $type, $pixels"
		before("texImage2D", sparams)
		val res = parent.texImage2D(target, level, internalformat, width, height, border, format, type, pixels)
		after("texImage2D", sparams, "$res")
		return res
	}

	override fun texImage2D(
		target: Int,
		level: Int,
		internalformat: Int,
		format: Int,
		type: Int,
		data: KmlNativeImageData
	): Unit {
		val sparams = "$target, $level, $internalformat, $format, $type, $data"
		before("texImage2D", sparams)
		val res = parent.texImage2D(target, level, internalformat, format, type, data)
		after("texImage2D", sparams, "$res")
		return res
	}

	override fun texParameterf(target: Int, pname: Int, param: Float): Unit {
		val sparams = "$target, $pname, $param"
		before("texParameterf", sparams)
		val res = parent.texParameterf(target, pname, param)
		after("texParameterf", sparams, "$res")
		return res
	}

	override fun texParameterfv(target: Int, pname: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$target, $pname, $params"
		before("texParameterfv", sparams)
		val res = parent.texParameterfv(target, pname, params)
		after("texParameterfv", sparams, "$res")
		return res
	}

	override fun texParameteri(target: Int, pname: Int, param: Int): Unit {
		val sparams = "$target, $pname, $param"
		before("texParameteri", sparams)
		val res = parent.texParameteri(target, pname, param)
		after("texParameteri", sparams, "$res")
		return res
	}

	override fun texParameteriv(target: Int, pname: Int, params: KmlNativeBuffer): Unit {
		val sparams = "$target, $pname, $params"
		before("texParameteriv", sparams)
		val res = parent.texParameteriv(target, pname, params)
		after("texParameteriv", sparams, "$res")
		return res
	}

	override fun texSubImage2D(
		target: Int,
		level: Int,
		xoffset: Int,
		yoffset: Int,
		width: Int,
		height: Int,
		format: Int,
		type: Int,
		pixels: KmlNativeBuffer
	): Unit {
		val sparams = "$target, $level, $xoffset, $yoffset, $width, $height, $format, $type, $pixels"
		before("texSubImage2D", sparams)
		val res = parent.texSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels)
		after("texSubImage2D", sparams, "$res")
		return res
	}

	override fun uniform1f(location: Int, v0: Float): Unit {
		val sparams = "$location, $v0"
		before("uniform1f", sparams)
		val res = parent.uniform1f(location, v0)
		after("uniform1f", sparams, "$res")
		return res
	}

	override fun uniform1fv(location: Int, count: Int, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $value"
		before("uniform1fv", sparams)
		val res = parent.uniform1fv(location, count, value)
		after("uniform1fv", sparams, "$res")
		return res
	}

	override fun uniform1i(location: Int, v0: Int): Unit {
		val sparams = "$location, $v0"
		before("uniform1i", sparams)
		val res = parent.uniform1i(location, v0)
		after("uniform1i", sparams, "$res")
		return res
	}

	override fun uniform1iv(location: Int, count: Int, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $value"
		before("uniform1iv", sparams)
		val res = parent.uniform1iv(location, count, value)
		after("uniform1iv", sparams, "$res")
		return res
	}

	override fun uniform2f(location: Int, v0: Float, v1: Float): Unit {
		val sparams = "$location, $v0, $v1"
		before("uniform2f", sparams)
		val res = parent.uniform2f(location, v0, v1)
		after("uniform2f", sparams, "$res")
		return res
	}

	override fun uniform2fv(location: Int, count: Int, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $value"
		before("uniform2fv", sparams)
		val res = parent.uniform2fv(location, count, value)
		after("uniform2fv", sparams, "$res")
		return res
	}

	override fun uniform2i(location: Int, v0: Int, v1: Int): Unit {
		val sparams = "$location, $v0, $v1"
		before("uniform2i", sparams)
		val res = parent.uniform2i(location, v0, v1)
		after("uniform2i", sparams, "$res")
		return res
	}

	override fun uniform2iv(location: Int, count: Int, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $value"
		before("uniform2iv", sparams)
		val res = parent.uniform2iv(location, count, value)
		after("uniform2iv", sparams, "$res")
		return res
	}

	override fun uniform3f(location: Int, v0: Float, v1: Float, v2: Float): Unit {
		val sparams = "$location, $v0, $v1, $v2"
		before("uniform3f", sparams)
		val res = parent.uniform3f(location, v0, v1, v2)
		after("uniform3f", sparams, "$res")
		return res
	}

	override fun uniform3fv(location: Int, count: Int, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $value"
		before("uniform3fv", sparams)
		val res = parent.uniform3fv(location, count, value)
		after("uniform3fv", sparams, "$res")
		return res
	}

	override fun uniform3i(location: Int, v0: Int, v1: Int, v2: Int): Unit {
		val sparams = "$location, $v0, $v1, $v2"
		before("uniform3i", sparams)
		val res = parent.uniform3i(location, v0, v1, v2)
		after("uniform3i", sparams, "$res")
		return res
	}

	override fun uniform3iv(location: Int, count: Int, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $value"
		before("uniform3iv", sparams)
		val res = parent.uniform3iv(location, count, value)
		after("uniform3iv", sparams, "$res")
		return res
	}

	override fun uniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit {
		val sparams = "$location, $v0, $v1, $v2, $v3"
		before("uniform4f", sparams)
		val res = parent.uniform4f(location, v0, v1, v2, v3)
		after("uniform4f", sparams, "$res")
		return res
	}

	override fun uniform4fv(location: Int, count: Int, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $value"
		before("uniform4fv", sparams)
		val res = parent.uniform4fv(location, count, value)
		after("uniform4fv", sparams, "$res")
		return res
	}

	override fun uniform4i(location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit {
		val sparams = "$location, $v0, $v1, $v2, $v3"
		before("uniform4i", sparams)
		val res = parent.uniform4i(location, v0, v1, v2, v3)
		after("uniform4i", sparams, "$res")
		return res
	}

	override fun uniform4iv(location: Int, count: Int, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $value"
		before("uniform4iv", sparams)
		val res = parent.uniform4iv(location, count, value)
		after("uniform4iv", sparams, "$res")
		return res
	}

	override fun uniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $transpose, $value"
		before("uniformMatrix2fv", sparams)
		val res = parent.uniformMatrix2fv(location, count, transpose, value)
		after("uniformMatrix2fv", sparams, "$res")
		return res
	}

	override fun uniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $transpose, $value"
		before("uniformMatrix3fv", sparams)
		val res = parent.uniformMatrix3fv(location, count, transpose, value)
		after("uniformMatrix3fv", sparams, "$res")
		return res
	}

	override fun uniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: KmlNativeBuffer): Unit {
		val sparams = "$location, $count, $transpose, $value"
		before("uniformMatrix4fv", sparams)
		val res = parent.uniformMatrix4fv(location, count, transpose, value)
		after("uniformMatrix4fv", sparams, "$res")
		return res
	}

	override fun useProgram(program: Int): Unit {
		val sparams = "$program"
		before("useProgram", sparams)
		val res = parent.useProgram(program)
		after("useProgram", sparams, "$res")
		return res
	}

	override fun validateProgram(program: Int): Unit {
		val sparams = "$program"
		before("validateProgram", sparams)
		val res = parent.validateProgram(program)
		after("validateProgram", sparams, "$res")
		return res
	}

	override fun vertexAttrib1f(index: Int, x: Float): Unit {
		val sparams = "$index, $x"
		before("vertexAttrib1f", sparams)
		val res = parent.vertexAttrib1f(index, x)
		after("vertexAttrib1f", sparams, "$res")
		return res
	}

	override fun vertexAttrib1fv(index: Int, v: KmlNativeBuffer): Unit {
		val sparams = "$index, $v"
		before("vertexAttrib1fv", sparams)
		val res = parent.vertexAttrib1fv(index, v)
		after("vertexAttrib1fv", sparams, "$res")
		return res
	}

	override fun vertexAttrib2f(index: Int, x: Float, y: Float): Unit {
		val sparams = "$index, $x, $y"
		before("vertexAttrib2f", sparams)
		val res = parent.vertexAttrib2f(index, x, y)
		after("vertexAttrib2f", sparams, "$res")
		return res
	}

	override fun vertexAttrib2fv(index: Int, v: KmlNativeBuffer): Unit {
		val sparams = "$index, $v"
		before("vertexAttrib2fv", sparams)
		val res = parent.vertexAttrib2fv(index, v)
		after("vertexAttrib2fv", sparams, "$res")
		return res
	}

	override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit {
		val sparams = "$index, $x, $y, $z"
		before("vertexAttrib3f", sparams)
		val res = parent.vertexAttrib3f(index, x, y, z)
		after("vertexAttrib3f", sparams, "$res")
		return res
	}

	override fun vertexAttrib3fv(index: Int, v: KmlNativeBuffer): Unit {
		val sparams = "$index, $v"
		before("vertexAttrib3fv", sparams)
		val res = parent.vertexAttrib3fv(index, v)
		after("vertexAttrib3fv", sparams, "$res")
		return res
	}

	override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit {
		val sparams = "$index, $x, $y, $z, $w"
		before("vertexAttrib4f", sparams)
		val res = parent.vertexAttrib4f(index, x, y, z, w)
		after("vertexAttrib4f", sparams, "$res")
		return res
	}

	override fun vertexAttrib4fv(index: Int, v: KmlNativeBuffer): Unit {
		val sparams = "$index, $v"
		before("vertexAttrib4fv", sparams)
		val res = parent.vertexAttrib4fv(index, v)
		after("vertexAttrib4fv", sparams, "$res")
		return res
	}

	override fun vertexAttribPointer(
		index: Int,
		size: Int,
		type: Int,
		normalized: Boolean,
		stride: Int,
		pointer: Int
	): Unit {
		val sparams = "$index, $size, $type, $normalized, $stride, $pointer"
		before("vertexAttribPointer", sparams)
		val res = parent.vertexAttribPointer(index, size, type, normalized, stride, pointer)
		after("vertexAttribPointer", sparams, "$res")
		return res
	}

	override fun viewport(x: Int, y: Int, width: Int, height: Int): Unit {
		val sparams = "$x, $y, $width, $height"
		before("viewport", sparams)
		val res = parent.viewport(x, y, width, height)
		after("viewport", sparams, "$res")
		return res
	}
}

class LogKmlGlProxy(parent: KmlGl) : KmlGlProxy(parent) {
	override fun before(name: String, params: String): Unit = run { println("before: $name ($params)") }
	override fun after(name: String, params: String, result: String): Unit =
		run { println("after: $name ($params) = $result") }
}

class CheckErrorsKmlGlProxy(parent: KmlGl) : KmlGlProxy(parent) {
	override fun after(name: String, params: String, result: String): Unit = run {
		val error = parent.getError(); if (error != NO_ERROR) {
		println("glError: $error ${getErrorString(error)} calling $name($params) = $result"); throw RuntimeException(
			"glError: $error ${getErrorString(error)} calling $name($params) = $result"
		)
	}
	}
}
