package com.soywiz.korio.vfs

import java.net.*

fun UrlVfs(url: URL): VfsFile = UrlVfs(url.toString())
