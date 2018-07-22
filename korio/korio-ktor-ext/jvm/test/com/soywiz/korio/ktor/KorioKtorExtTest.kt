package com.soywiz.korio.ktor

import com.soywiz.korio.file.std.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import org.junit.Test
import kotlin.test.*

class KorioKtorExtTest {
	@Test
	fun name() {
		val mem = MemoryVfsMix(
			"hello.txt" to "world"
		)

		withTestApplication({
			routing {
				get("/") {
					call.respondFile(mem["hello.txt"])
				}
			}
		}) {
			handleRequest(HttpMethod.Get, "/").apply {
				assertEquals("world", response.content)
			}
		}
	}
}