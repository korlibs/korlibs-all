package com.soywiz.korte.web.example

import com.soywiz.korio.Korio
import com.soywiz.korio.ext.web.router.Route
import com.soywiz.korio.ext.web.router.RoutePriority
import com.soywiz.korio.ext.web.router.registerRoutes
import com.soywiz.korio.ext.web.router.router
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.net.http.createHttpServer
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korte.Templates

fun main(args: Array<String>) = Korio {
	val templates = Templates(ResourcesVfs)
	val injector = AsyncInjector()
		.mapInstance(templates)
	val server = createHttpServer()
		.router(injector) {
			registerRoutes<RootRoute>()
		}
		.listen(8080)
	println("Listening at... ${server.actualPort}")
}

@Suppress("unused")
class RootRoute(
	val templates: Templates
) {
	val staticRoot = ResourcesVfs["static"].jail()

	@Route(Http.Methods.GET, "/")
	suspend fun root() = templates.prender("index.html")

	@Route(Http.Methods.ALL, "/*", priority = RoutePriority.LOWEST)
	suspend fun static(req: HttpServer.Request): VfsFile = staticRoot[req.path]

	@Route(Http.Methods.GET, "/demo")
	suspend fun demo(): String {
		return "DEMO!"
	}

}