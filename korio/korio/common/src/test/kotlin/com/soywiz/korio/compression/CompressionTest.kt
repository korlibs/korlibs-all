package com.soywiz.korio.compression

import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.crypto.*
import kotlin.test.*

class CompressionTest {
	@Test
	fun name() = syncTest {
		val data =
			Base64.decode("H4sIAAAAAAAAA+3SsREAEBSD4WcFm2ACTID9dxGFxgDcub/4mjQpEmdmDuYPKwsSJT3qz1KkXu7fWZMu4/IGr78AAAAAAD+a6ywcnAAQAAA=")
		val res = Compression.uncompressGzip(data)
		val res2 = res.readIntArray_le(0, 4096 / 4)
		assertEquals(
			"" +
					"1111111111111111111111111111111111111111111111111111111111111111111818181814950511111111111111111111111111818181816566671111111" +
					"1111111111111111118181811818283111111111111111111111111118181111111111111111111111111111111111111111111111111111111111111111111" +
					"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
					"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
					"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
					"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
					"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
					"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
					"111111111111111111111111111111",
			res2.toList().joinToString("")
		)
	}
}