package com.soywiz.korte

import com.soywiz.korio.async.await
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.coroutine.CoroutineContext
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.templ.TemplateEngine

class KorteVertxTemplateEngine(val coroutineContext: CoroutineContext, val templates: Templates) : TemplateEngine {
	override fun render(context: RoutingContext, templateDirectory: String, templateFileName: String, handler: Handler<AsyncResult<Buffer>>) {
		spawnAndForget(coroutineContext) {
			handler.handle(vxResult {
				val str = templates.get(templateFileName).invoke()
				Buffer.buffer(str)
			})
		}
	}

	override fun render(context: RoutingContext, templateFileName: String, handler: Handler<AsyncResult<Buffer>>) {
		spawnAndForget(coroutineContext) {
			handler.handle(vxResult {
				val str = templates.get(templateFileName).invoke()
				Buffer.buffer(str)
			})
		}
	}
}

suspend fun <T : Any?> vxResult(callback: suspend () -> T): AsyncResult<T> {
	var succeeded = false
	var failed = false
	var result: T? = null
	var cause: Throwable? = null
	try {
		result = callback.await()
		succeeded = true
	} catch (e: Throwable) {
		cause = e
		failed = true
	}
	return object : AsyncResult<T> {
		override fun succeeded(): Boolean = succeeded
		override fun failed(): Boolean = failed
		override fun cause(): Throwable? = cause
		override fun result(): T? = result
	}
}