package com.soywiz.korio.async

import com.soywiz.korio.expectException
import org.junit.Ignore
import kotlin.IllegalStateException
import kotlin.String
import kotlin.test.*

class PromiseTest {
	@Test
	fun nullPromise() = suspendTest {
		val promise = Promise.resolved<String?>(null)
		assertEquals(null, promise.await())
	}

	@Test
	fun rejectedPromise() = suspendTest {
		val promise = Promise.rejected<String?>(IllegalStateException())
		expectException<IllegalStateException> { promise.await() }
	}

	@Test
	fun name() = suspendTest {
		var out = ""
		val p = spawn(coroutineContext) {
			try {
				sleep(100)
				10
			} catch (e: com.soywiz.korio.CancellationException) {
				out += "|"
				throw e
			}
		}

		p.cancel()

		try {
			out += "" + p.await()
		} catch (e: com.soywiz.korio.CancellationException) {
			out += "CancellationException"
		}

		assertEquals(
			"CancellationException",
			out
		)
	}

	@Test
	@Ignore
	fun cancel1() = suspendTest {
		var out = ""
		val prom = go {
			out += "a"
			sleep(1000)
			out += "b"
			go {
				sleep(1000)
				out += "c"
				sleep(1000)
				out += "d"
			}.await()
		}
		assertEquals("a", out)
		this@syncTest.step(1200)
		assertEquals("ab", out)
		this@syncTest.step(100)
		assertEquals("ab", out)
		prom.cancel()
		this@syncTest.step(1200)
		assertEquals("ab", out)
	}

	@Test
	@Ignore
	fun jvmUnsafeAwait() = suspendTest {
		assertEquals(10, async(coroutineContext) {
			sleep(20)
			10
		}.jvmSyncAwait())
	}
}