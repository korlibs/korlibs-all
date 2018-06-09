package com.codeazur.as3swf

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class As3SwfTest {
	@Test
	fun name() = suspendTest {
		val swf2 = SWF().loadBytes(ResourcesVfs["empty.swf"].readAll())
		println(swf2.frameSize.rect)
		for (tag in swf2.tags) {
			println(tag)
		}

		val swf = SWF()
		swf.tags += TagFileAttributes()
	}

	@Test
	fun name2() = suspendTest {
		val swf2 = SWF().loadBytes(ResourcesVfs["simple.swf"].readAll())
		println(swf2.frameSize.rect)
		for (tag in swf2.tags) {
			println(tag)
		}

		val swf = SWF()
		swf.tags += TagFileAttributes()
	}


	@Test
	fun name3() = suspendTest {
		val swf2 = SWF().loadBytes(ResourcesVfs["test1.swf"].readAll())
		println(swf2.frameSize.rect)
		for (tag in swf2.tags) {
			println(tag)
		}

		val swf = SWF()
		swf.tags += TagFileAttributes()
	}

	//@Test
	//@Ignore
	//fun name4() = suspendTest {
	//	val swf2 = SWF().loadBytes(LocalVfs["c:/temp/ui.swf"].readAll())
	//	println(swf2.frameSize.rect)
	//	for (tag in swf2.tags) {
//
	//		println(tag)
	//	}
//
	//	val swf = SWF()
	//	swf.tags += TagFileAttributes()
	//}

	//@Test
	//fun name4() = suspendTest {
	//	//val swf2 = SWF().loadBytes(File("c:/temp/sample1.swf").readBytes())
	//	println(swf2.frameSize.rect)
	//	for (tag in swf2.tags) {
	//		println(tag)
	//	}

	//}
}
