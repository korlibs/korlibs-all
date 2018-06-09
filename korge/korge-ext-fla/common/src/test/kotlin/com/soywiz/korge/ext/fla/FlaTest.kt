package com.soywiz.korge.ext.fla

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class FlaTest {
	//val TestAssertVfs = localCurrentDirVfs["src/test/resources"]
	val TestAssertVfs = ResourcesVfs

	@Test
	@Ignore
	fun name() = suspendTest {
		val fla = Fla.read(TestAssertVfs["simple1.fla"])
		//val fla = Fla.read(ResourcesVfs["simple1"])
	}
}
