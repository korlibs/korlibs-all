package com.soywiz.korio.file.std

import com.soywiz.korio.file.*
import com.soywiz.korio.file.*

fun JailVfs(jailRoot: VfsFile): VfsFile = object : Vfs.Proxy() {
	val baseJail = VfsUtil.normalize(jailRoot.path)

	override suspend fun access(path: String): VfsFile = jailRoot[VfsUtil.normalize(
		path
	).trim('/')]

	override suspend fun VfsFile.transform(): VfsFile {
		val outPath = VfsUtil.normalize(this.path)
		if (!outPath.startsWith(baseJail)) throw UnsupportedOperationException("Jail not base root : ${this.path} | $baseJail")
		return file(outPath.substring(baseJail.length))
	}

	override val absolutePath: String get() = jailRoot.absolutePath

	override fun toString(): String = "JailVfs($jailRoot)"
}.root
