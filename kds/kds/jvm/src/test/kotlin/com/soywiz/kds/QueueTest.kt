package com.soywiz.kds

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QueueTest {
    @Test
    fun name() {
        val queue = Queue<Boolean>()
        for (n in 0 until 1025) queue.enqueue(true)
        for (n in 0 until 1025) assertEquals(true, queue.dequeue())
    }

    @Test
    fun test1() {
        val queue = Queue<Boolean>()
        assertFailsWith<IndexOutOfBoundsException> {
            queue.dequeue()
        }
        queue.enqueue(true)
        queue.enqueue(true)
        queue.dequeue()
        queue.dequeue()
        assertFailsWith<IndexOutOfBoundsException> {
            queue.dequeue()
        }
    }
}