package com.soywiz.kmem

import org.junit.Test
import kotlin.test.assertEquals

class BitsExtTest {
    @Test
    fun testCountTrailingZeros() {
        assertEquals(32, (0b00000000000000000000000000000000).countTrailingZeros())
        assertEquals(0, (0b01111111111111111111111111111111).countTrailingZeros())
        assertEquals(1, (0b11111111111111111111111111111110).toInt().countTrailingZeros())
        for (n in 0 until 32) assertEquals(n, (1 shl n).countTrailingZeros())
        for (n in 0 until 32) assertEquals(n, (0x173F52B1 shl n).countTrailingZeros())
        for (n in 0 until 32) assertEquals(n, ((-1) shl n).countTrailingZeros())
    }

    @Test
    fun testCountTrailingOnes() {
        assertEquals(32, (0b11111111111111111111111111111111).toInt().countTrailingOnes())
        assertEquals(31, (0b01111111111111111111111111111111).toInt().countTrailingOnes())
        assertEquals(0, (0b11111111111111111111111111111110).toInt().countTrailingOnes())
        for (n in 0 until 32) assertEquals(n, (1 shl n).inv().countTrailingOnes())
        for (n in 0 until 32) assertEquals(n, (0x173F52B1 shl n).inv().countTrailingOnes())
        for (n in 0 until 32) assertEquals(n, ((-1) shl n).inv().countTrailingOnes())
    }

    @Test
    fun testCountLeadingZeros() {
        assertEquals(32, (0b00000000000000000000000000000000).countLeadingZeros())
        assertEquals(1, (0b01111111111111111111111111111111).countLeadingZeros())
        assertEquals(0, (0b11111111111111111111111111111110).toInt().countLeadingZeros())
        for (n in 0 until 32) assertEquals(n, (1 shl n).reverseBits().countLeadingZeros())
        for (n in 0 until 32) assertEquals(n, (0x173F52B1 shl n).reverseBits().countLeadingZeros())
        for (n in 0 until 32) assertEquals(n, ((-1) shl n).reverseBits().countLeadingZeros())
    }
}