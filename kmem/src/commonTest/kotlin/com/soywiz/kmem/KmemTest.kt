package com.soywiz.kmem

import kotlin.test.*

class KmemTest {
	@Test
	fun testBasicUsage() {
		val data = MemBufferAlloc(16)

		val i8 = data.asInt8Buffer()
		i8[0] = 0
		i8[1] = 1
		i8[2] = 2
		i8[3] = 3

		i8[4] = 4
		i8[5] = 5
		i8[6] = 6
		i8[7] = 7

		val i32 = data.asInt32Buffer()
		assertEquals(0x03020100, i32[0])
		assertEquals(0x07060504, i32[1])

		val i32_off1 = i32.subarray(1)
		assertEquals(0x07060504, i32_off1[0])
		i32_off1[1] = 0x0B0A0908

		assertEquals(0x0B0A0908, i32[2])
	}

	@Test
	fun testArrayCopyOverlapping() {
		val i32 = Int32BufferAlloc(10)
		i32[0] = 0x01020304
		i32[1] = 0x05060708
		arraycopy(i32, 0, i32, 1, 4)
		assertEquals(0x01020304, i32[0])
		assertEquals(0x01020304, i32[1])
		assertEquals(0x05060708, i32[2])
		assertEquals(0x00000000, i32[3])
		assertEquals(0x00000000, i32[4])

		val fast = KmlNativeBuffer(i32.mem)

		assertEquals(listOf(4, 3, 2, 1, 4, 3, 2, 1, 8, 7, 6, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), (0 until (10 * 4)).map { fast.i8[it].toInt() }.toList())

		val out = ByteArray(10)
		fast.getArrayInt8(1, out, 1, 5)

		assertEquals(listOf(0, 3, 2, 1, 4, 3, 0, 0, 0, 0), out.toList().map { it.toInt() })

		val outS = ShortArray(10)
		fast.getAlignedArrayInt16(1, outS, 1, 5)

		assertEquals(listOf(0, 258, 772, 258, 1800, 1286, 0, 0, 0, 0), outS.toList().map { it.toInt() })

		fast.setAlignedArrayInt16(1, shortArrayOf(1, 2, 3, 4, 5, 6), 1, 4)
		fast.getAlignedArrayInt16(1, outS, 1, 5)

		assertEquals(listOf(0, 2, 3, 4, 5, 1286, 0, 0, 0, 0), outS.toList().map { it.toInt() })
	}

	@Test
	fun testKmlNativeBuffer() {
		val mem = KmlNativeBuffer.alloc(10)
		for (n in 0 until 8) mem[n] = n
		assertEquals(0x03020100, mem.getAlignedInt32(0))
		assertEquals(0x07060504, mem.getAlignedInt32(1))

		assertEquals(0x03020100, mem.getUnalignedInt32(0))
		assertEquals(0x04030201, mem.getUnalignedInt32(1))
		assertEquals(0x05040302, mem.getUnalignedInt32(2))
	}

	@Test
	fun testCopy() {
		val array = arrayOf("a", "b", "c", null, null)
		arraycopy(array, 0, array, 1, 4)
		assertEquals(listOf("a", "a", "b", "c", null), array.toList())
		arraycopy(array, 2, array, 1, 3)
		assertEquals(listOf("a", "b", "c", null, null), array.toList())
	}

	@Test
	fun testFill() {
		val array = intArrayOf(1, 1, 1, 1, 1)
		array.fill(2)
		assertEquals(intArrayOf(2, 2, 2, 2, 2).toList(), array.toList())
		array.fill(3, 1, 4)
		assertEquals(intArrayOf(2, 3, 3, 3, 2).toList(), array.toList())
	}
}