package com.soywiz.korim.format

import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*

open class BaseImageFormatTest {
	val root: VfsFile = if (OS.isJs) {
		// localCurrentDirVfs = korim/js/build/node_modules
		//LocalVfs("../../../../korim/common/src/test/resources")
		ResourcesVfs
	} else {
		ResourcesVfs
	}
}