package com.soywiz.korio.file.std

import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.net.*
import com.soywiz.korio.file.*

object UniversalVfs {
	@PublishedApi
	internal var schemaBuilders = hashMapOf<String, (URI) -> VfsFile>()

	fun registerSchema(schema: String, builder: (URI) -> VfsFile) {
		schemaBuilders[schema] = builder
	}

	inline fun temporal(callback: UniversalVfs.() -> Unit) {
		val original = schemaBuilders.toMap()
		try {
			callback(this)
		} finally {
			schemaBuilders = HashMap(original)
		}
	}

	init {
		registerSchema("http") { UrlVfs(it) }
		registerSchema("https") { UrlVfs(it) }
		registerSchema("file") { rootLocalVfs[it.path] }
	}

	operator fun invoke(uri: String, base: VfsFile? = null): VfsFile {
		return when {
			URI.isAbsolute(uri) -> {
				val uriUri = URI(uri)
				val builder = schemaBuilders[uriUri.scheme]
				if (builder != null) {
					builder(uriUri)
				} else {
					invalidOp("Unsupported scheme '${uriUri.scheme}'")
				}
			}
			(base != null) -> base[uri]
			else -> localCurrentDirVfs[uri]
		}
	}
}

// @TODO: Make general
val String.uniVfs get() = UniversalVfs(this)

fun String.uniVfs(base: VfsFile? = null): VfsFile =
	UniversalVfs(this, base)
