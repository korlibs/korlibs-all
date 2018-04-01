package com.soywiz.kds

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CircularListTest {
    private val CircularList<String>.str get() = this.joinToString(",")

    private fun <T> create() = CircularList<T>()

    @Test
    fun simple() {
        val l = create<String>()
        l += listOf("a", "b", "c", "d", "e", "f")
        assertEquals("a,b,c,d,e,f", l.str)
        assertEquals("a", l.first)
        assertEquals("f", l.last)
        assertEquals(6, l.size)
        l.removeAt(1)
        assertEquals(5, l.size)
        l.removeAt(l.size - 2)
        assertEquals(4, l.size)
        assertEquals("a,c,d,f", l.str)
        l.remove("d")
        assertEquals(3, l.size)
        assertEquals("a,c,f", l.str)
        l.retainAll(listOf("a", "f"))
        assertEquals(2, l.size)
        assertEquals("a,f", l.str)
        l.removeAll(listOf("a"))
        assertEquals(1, l.size)
        assertEquals("f", l.str)
    }

    @Test
    fun grow() {
        val l = create<String>()
        for (n in 0 until 1000) l.add("$n")
        for (n in 0 until 495) {
            l.removeFirst()
            l.removeLast()
        }
        assertEquals(10, l.size)
        assertEquals("495,496,497,498,499,500,501,502,503,504", l.str)
    }

    @Test
    fun grow2() {
        val l = create<Boolean>()
        for (n in 0 until 1000) l.addFirst(true)
        for (n in 0 until 1000) l.removeFirst()
        for (n in 0 until 1000) l.addFirst(true)
    }

    @Test
    fun test2() {
        val l = create<String>()
        l.addLast("a")
        l.addLast("b")
        l.addLast("c")
        l.removeAt(1)
        assertEquals("a,c", l.str)
    }

    @Test
    fun exceptions() {
        val l = create<Boolean>()
        assertFailsWith<IndexOutOfBoundsException> {
            l.removeFirst()
        }
        assertFailsWith<IndexOutOfBoundsException> {
            l.removeLast()
        }
        assertFailsWith<IndexOutOfBoundsException> {
            l.removeAt(1)
        }
        l.addFirst(true)
        l.removeAt(0)
        assertFailsWith<IndexOutOfBoundsException> {
            l.removeAt(0)
        }
    }
}
