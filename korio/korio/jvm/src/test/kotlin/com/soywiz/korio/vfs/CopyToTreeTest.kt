package com.soywiz.korio.vfs

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korio.stream.*
import kotlin.test.*

class CopyToTreeTest {
	@Test
	fun name() = syncTest {
		val mem = MemoryVfs(
			lmapOf(
				"root.txt" to "hello".toByteArray().openAsync(),
				"hello/world.txt" to "hello".toByteArray().openAsync()
			)
		)
		val out = MemoryVfs()
		mem.copyToTree(out)
		assertEquals(
			"[/root.txt, /hello, /hello/world.txt]",
			out.listRecursive().map { it.fullname }.toList().toString()
		)
	}
}