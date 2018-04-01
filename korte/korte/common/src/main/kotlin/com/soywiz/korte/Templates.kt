package com.soywiz.korte

import com.soywiz.korio.text.AsyncTextWriterContainer
import com.soywiz.korio.util.AsyncCache
import com.soywiz.korio.vfs.VfsFile

class Templates(
	val root: VfsFile,
	val includes: VfsFile = root,
	val layouts: VfsFile = root,
	val config: TemplateConfig = TemplateConfig()
) {
	val cache = AsyncCache()

	suspend fun getInclude(name: String): Template = cache("include/$name") {
		val content = includes[name].readString()
		Template(name, this@Templates, content, config).init()
	}

	suspend fun getLayout(name: String): Template = cache("layout/$name") {
		val content = includes[name].readString()
		Template(name, this@Templates, content, config).init()
	}

	//suspend operator fun get(name: String): Template = cache(name) { // @TODO: Unsupported operator. Re-enable when this limitation is lifted.
	suspend fun get(name: String): Template = cache(name) {
		val content = root[name].readString()
		Template(name, this@Templates, content, config).init()
	}

	suspend fun render(name: String, vararg args: Pair<String, Any?>): String {
		return get(name).invoke(*args)
	}

	suspend fun render(name: String, args: Map<String, Any?>): String {
		return get(name).invoke(HashMap(args))
	}

	suspend fun prender(name: String, vararg args: Pair<String, Any?>): AsyncTextWriterContainer {
		return get(name).prender(*args)
	}

	suspend fun prender(name: String, args: Map<String, Any?>): AsyncTextWriterContainer {
		return get(name).prender(args)
	}
}