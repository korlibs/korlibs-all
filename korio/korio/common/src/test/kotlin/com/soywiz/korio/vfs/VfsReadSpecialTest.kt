package com.soywiz.korio.vfs

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import kotlin.test.*

class VfsReadSpecialTest {
	data class MySpecialClass(val vfs: Vfs, val path: String)
	data class MySpecialClass2(val vfs: Vfs, val path: String)

	class MySpecialClass_VfsSpecialReader : VfsSpecialReader<MySpecialClass>(MySpecialClass::class) {
		override suspend fun readSpecial(vfs: Vfs, path: String): MySpecialClass = MySpecialClass(vfs, path)
	}

	init {
		registerVfsSpecialReader(MySpecialClass_VfsSpecialReader())
	}

	@Test
	fun testReadSpecial() = suspendTest {
		val mem = MemoryVfs(lmapOf())
		assertEquals(
			MySpecialClass(mem.vfs, "/test.txt"),
			mem["test.txt"].readSpecial<MySpecialClass>()
		)
	}

	@kotlin.test.Test
	fun testReadSpecial2() = suspendTest {
		val mem = MemoryVfs(lmapOf())
		val root = MergedVfs(listOf(mem))
		assertEquals(
			MySpecialClass(mem.vfs, "/test.txt"),
			root["test.txt"].readSpecial<MySpecialClass>()
		)
	}

	@kotlin.test.Test
	fun testReadSpecialNonHandled() = suspendTest {
		expectException<Throwable> {
			val mem = MemoryVfs(lmapOf())
			mem["test.txt"].readSpecial<MySpecialClass2>()
		}
	}
}