package com.soywiz.korio.ktor

/*
fun VfsFile.openByteReadChannel(range: LongRange? = null) = writer(DefaultDispatcher) {
	val ch = channel
	val temp = ByteArray(16 * 1024)
	open(VfsOpenMode.READ).use {
		val ss = if (range != null) sliceWithBounds(range.start, range.endInclusive + 1) else this
		while (ss.hasAvailable()) {
			val read = ss.read(temp, 0, temp.size)
			if (read <= 0) break
			ch.writeFully(temp, 0, read)
		}
	}
}.channel

class VfsFileContent(
	val file: VfsFile,
	val stat: VfsStat,
	override val contentType: ContentType = ContentType.defaultForFile(Paths.get(file.absolutePath))
) : OutgoingContent.ReadChannelContent() {
	companion object {
		suspend operator fun invoke(
			file: VfsFile,
			contentType: ContentType = ContentType.defaultForFile(Paths.get(file.absolutePath))
		) = VfsFileContent(file, file.stat(), contentType)
	}

	override val contentLength: Long get() = stat.size

	init {
		versions += LastModifiedVersion(Date(stat.modifiedTime))
	}

	// TODO: consider using WriteChannelContent to avoid piping
	// Or even make it dual-content so engine implementation can choose
	override fun readFrom(): ByteReadChannel = file.openByteReadChannel()

	override fun readFrom(range: LongRange): ByteReadChannel = file.openByteReadChannel(range)
}


suspend fun ApplicationCall.respondFile(file: VfsFile) {
	val finalFile = file.getUnderlyingUnscapedFile()
	if (finalFile.vfs is LocalVfs) {
		// Try to respond with optimized File whenever it is possible
		respondFile(File(finalFile.path))
	} else {
		respond(VfsFileContent(file))
	}
}
*/
