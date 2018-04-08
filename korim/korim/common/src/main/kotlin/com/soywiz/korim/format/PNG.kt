package com.soywiz.korim.format

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlin.math.*

object PNG : ImageFormat("png") {
	const val MAGIC1 = 0x89504E47.toInt()
	const val MAGIC2 = 0x0D0A1A0A.toInt()

	data class InterlacedPass(
		val startingRow: Int, val startingCol: Int,
		val rowIncrement: Int, val colIncrement: Int,
		val blockHeight: Int, val blockWidth: Int
	) {
		val colIncrementShift = log2(colIncrement.toDouble()).toInt()
	}

	val InterlacedPasses = listOf(
		InterlacedPass(0, 0, 8, 8, 8, 8),
		InterlacedPass(0, 4, 8, 8, 8, 4),
		InterlacedPass(4, 0, 8, 4, 4, 4),
		InterlacedPass(0, 2, 4, 4, 4, 2),
		InterlacedPass(2, 0, 4, 2, 2, 2),
		InterlacedPass(0, 1, 2, 2, 2, 1),
		InterlacedPass(1, 0, 2, 1, 1, 1)
	)

	val NormalPasses = listOf(
		InterlacedPass(0, 0, 1, 1, 1, 1)
	)

	enum class Colorspace(val id: Int) {
		GRAYSCALE(0),
		RGB(2),
		INDEXED(3),
		GRAYSCALE_ALPHA(4),
		RGBA(6);

		companion object {
			val BY_ID = values().associateBy { it.id }
		}
	}

	class Header(
		val width: Int,
		val height: Int,
		val bitsPerChannel: Int,
		val colorspace: Colorspace, // 0=grayscale, 2=RGB, 3=Indexed, 4=grayscale+alpha, 6=RGBA
		val compressionmethod: Int, // 0
		val filtermethod: Int,
		val interlacemethod: Int
	) {
		val bytes = when (colorspace) {
			Colorspace.GRAYSCALE -> 1
			Colorspace.INDEXED -> 1
			Colorspace.GRAYSCALE_ALPHA -> 2
			Colorspace.RGB -> 3
			Colorspace.RGBA -> 4
			else -> 1
		}
		val stride = width * bytes
	}

	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = try {
		val header = readCommon(s, readHeader = true) as Header
		ImageInfo().apply {
			this.width = header.width
			this.height = header.height
			this.bitsPerPixel = header.bitsPerChannel
		}
	} catch (t: Throwable) {
		null
	}

	//private val method = Deflate
	private val method = ZLib

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		val bitmap = image.mainBitmap
		val width = bitmap.width
		val height = bitmap.height
		s.write32_be(MAGIC1)
		s.write32_be(MAGIC2)

		fun writeChunk(name: String, data: ByteArray) {
			val nameBytes = name.toByteArray().copyOf(4)

			var crc = CRC32.INITIAL
			crc = CRC32.update(crc, nameBytes)
			crc = CRC32.update(crc, data)

			s.write32_be(data.size)
			s.writeBytes(nameBytes)
			s.writeBytes(data)
			s.write32_be(crc) // crc32!
		}

		val level = props.quality.convertRangeClamped(0.0, 1.0, 0.0, 9.0).toInt()

		fun compress(data: ByteArray): ByteArray {
			if (level == 0) {
				//if (false) {
				var adler32 = Adler32.INITIAL
				//if (false) {
				//val data = ByteArray(0x15)
				val blocks = ceil(data.size.toDouble() / 0xFFFF.toDouble()).toInt()
				val lastBlockSize = data.size % 0xFFFF
				val out = ByteArray(2 + 4 + data.size + blocks * 5)
				var upos = 0
				var pos = 2
				out.write8(0, 0x78)
				out.write8(1, 0x01)
				for (n in 0 until blocks) {
					val last = n == blocks - 1
					val size = if (last) lastBlockSize else 0xFFFF
					out.write8(pos, if (last) 1 else 0)
					out.write16_le(pos + 1, size)
					out.write16_le(pos + 3, size.inv())
					arraycopy(data, upos, out, pos + 5, size)
					pos += 5 + size
					upos += size
				}

				//adler32.update(out, 0, pos)
				adler32 = Adler32.update(adler32, data, 0, data.size)
				out.write32_be(pos + 0, adler32)
				return out
			} else {
				return data.syncCompress(method, CompressionContext(level = level))
			}
		}

		fun writeChunk(name: String, initialCapacity: Int = 4096, callback: SyncStream.() -> Unit) {
			return writeChunk(name, MemorySyncStreamToByteArray(initialCapacity) { callback() })
		}

		fun writeChunkCompressed(name: String, initialCapacity: Int = 4096, callback: SyncStream.() -> Unit) {
			return writeChunk(name, compress(MemorySyncStreamToByteArray(initialCapacity) { callback() }))
		}

		fun writeHeader(colorspace: Colorspace) {
			writeChunk("IHDR", initialCapacity = 13) {
				write32_be(width)
				write32_be(height)
				write8(8) // bits
				write8(colorspace.id) // colorspace
				write8(0) // compressionmethod
				write8(0) // filtermethod
				write8(0) // interlacemethod
			}
		}

		when (bitmap) {
			is Bitmap8 -> {
				writeHeader(Colorspace.INDEXED)
				writeChunk("PLTE", initialCapacity = bitmap.palette.size * 3) {
					for (c in bitmap.palette) {
						write8(RGBA.getR(c))
						write8(RGBA.getG(c))
						write8(RGBA.getB(c))
					}
				}
				writeChunk("tRNS", initialCapacity = bitmap.palette.size * 1) {
					for (c in bitmap.palette) {
						write8(RGBA.getA(c))
					}
				}

				val out = ByteArray(height + width * height)
				var pos = 0
				for (y in 0 until height) {
					out.write8(pos++, 0)
					val index = bitmap.index(0, y)
					arraycopy(bitmap.data, index, out, pos, width)
					pos += width
				}
				writeChunk("IDAT", compress(out))
			}
			is Bitmap32 -> {
				writeHeader(Colorspace.RGBA)

				val out = ByteArray(height + width * height * 4)
				var pos = 0
				for (y in 0 until height) {
					out.write8(pos++, 0) // no filter
					val index = bitmap.index(0, y)
					if (bitmap.premult) {
						for (x in 0 until width) {
							out.write32_le(pos, RGBA.depremultiplyFast(bitmap.data[index + x]))
							pos += 4
						}
					} else {
						for (x in 0 until width) {
							out.write32_le(pos, bitmap.data[index + x])
							pos += 4
						}
					}
				}

				writeChunk("IDAT", compress(out))
			}
		}

		writeChunk("IEND", initialCapacity = 0) {
		}
	}

	private fun readCommon(s: SyncStream, readHeader: Boolean): Any? {
		if (s.readS32_be() != MAGIC1) throw IllegalArgumentException("Invalid PNG file")
		s.readS32_be() // magic continuation

		var pheader: Header? = null
		val pngdata = ByteArrayBuilder()
		val rgbPalette = UByteArray(3 * 0x100)
		val aPalette = UByteArray(ByteArray(0x100) { -1 })
		var paletteCount = 0

		fun SyncStream.readChunk() {
			val length = readS32_be()
			val type = readStringz(4)
			val data = readStream(length.toLong())
			val crc = readS32_be()

			when (type) {
				"IHDR" -> {
					pheader = data.run {
						Header(
							width = readS32_be(),
							height = readS32_be(),
							bitsPerChannel = readU8(),
							colorspace = Colorspace.BY_ID[readU8()] ?: Colorspace.RGBA,
							compressionmethod = readU8(),
							filtermethod = readU8(),
							interlacemethod = readU8()
						)
					}

					val header = pheader!!
				}
				"PLTE" -> {
					paletteCount = max(paletteCount, data.length.toInt() / 3)
					data.read(rgbPalette.data, 0, data.length.toInt())
				}
				"tRNS" -> {
					paletteCount = max(paletteCount, data.length.toInt())
					data.read(aPalette.data, 0, data.length.toInt())
				}
				"IDAT" -> {
					pngdata.append(data.readAll())
				}
				"IEND" -> {
				}
			}
			//println(type)
		}

		while (!s.eof) {
			s.readChunk()
			if (readHeader && pheader != null) return pheader
		}

		val header = pheader ?: throw IllegalArgumentException("PNG without header!")
		val width = header.width
		val height = header.height

		val datab = ByteArray((1 + width) * height * header.bytes)

		pngdata.toByteArray().syncUncompressTo(method, datab)

		val data = datab.openSync()
		val context = DecodingContext(header)
		val bpp = context.header.bytes
		val row32 = context.row32

		val bmp = when {
			header.bytes == 1 -> Bitmap8(
				width,
				height,
				palette = (0 until paletteCount).map {
					RGBA(
						rgbPalette[it * 3 + 0],
						rgbPalette[it * 3 + 1],
						rgbPalette[it * 3 + 2],
						aPalette[it]
					)
				}.toIntArray()
			)
			else -> Bitmap32(width, height)
		}
		val bmp8 = bmp as? Bitmap8?
		val bmp32 = bmp as? Bitmap32?
		val passes = when (header.interlacemethod) {
			1 -> InterlacedPasses
			else -> NormalPasses
		}

		for (pass in passes) {
			for (row in pass.startingRow until height step pass.rowIncrement) {
				val col = pass.startingCol
				val colIncrement = pass.colIncrement
				val pixelsInThisRow = width ushr pass.colIncrementShift
				val bytesInThisRow = (pixelsInThisRow * header.bytes)
				val filter = data.readU8()
				val currentRow = context.currentRow
				val lastRow = context.lastRow
				data.readExact(currentRow.data, 0, bytesInThisRow)
				when {
					bmp8 != null -> {
						applyFilter(filter, lastRow, currentRow, header.bytes)
						bmp8.setRowChunk(col, row, currentRow.data, width, colIncrement)
					}
					bmp32 != null -> {
						applyFilter(filter, lastRow, currentRow, bpp, bytesInThisRow)
						when (bpp) {
							3 -> RGB.decode(currentRow.data, 0, row32, 0, pixelsInThisRow)
							4 -> RGBA.decode(currentRow.data, 0, row32, 0, pixelsInThisRow)
							else -> TODO("Bytes: $bpp")
						}
						bmp32.setRowChunk(col, row, row32, width, colIncrement)
					}
				}
				context.swapRows()
			}
		}

		return bmp
	}

	class DecodingContext(val header: Header) {
		var lastRow = UByteArray(header.stride)
		var currentRow = UByteArray(header.stride)
		val row32 = IntArray(header.width)

		fun swapRows() {
			val temp = currentRow
			currentRow = lastRow
			lastRow = temp
		}
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData =
		ImageData(listOf(ImageFrame(readCommon(s, readHeader = false) as Bitmap)))

	fun paethPredictor(a: Int, b: Int, c: Int): Int {
		val p = a + b - c
		val pa = abs(p - a)
		val pb = abs(p - b)
		val pc = abs(p - c)
		return if ((pa <= pb) && (pa <= pc)) a else if (pb <= pc) b else c
	}

	fun applyFilter(filter: Int, p: UByteArray, c: UByteArray, bpp: Int, size: Int = c.size) {
		when (filter) {
			0 -> Unit
			1 -> for (n in bpp until size) c[n] += c[n - bpp]
			2 -> for (n in 0 until size) c[n] += p[n]
			3 -> {
				for (n in 0 until bpp) c[n] += p[n] / 2
				for (n in bpp until size) c[n] += (c[n - bpp] + p[n]) / 2
			}
			4 -> {
				for (n in 0 until bpp) c[n] += p[n]
				for (n in bpp until size) c[n] += paethPredictor(c[n - bpp], p[n], p[n - bpp])
			}
			else -> TODO("Filter: $filter")
		}
	}
}