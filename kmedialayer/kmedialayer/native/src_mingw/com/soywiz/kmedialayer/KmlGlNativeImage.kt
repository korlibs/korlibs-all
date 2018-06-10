package com.soywiz.kmedialayer

import kotlinx.cinterop.*
import platform.gdiplus.*
import platform.windows.*

// https://referencesource.microsoft.com/#System.Drawing/commonui/System/Drawing/Advanced/Gdiplus.cs,87a3562f8aa6f54e,references

var initializedGdiPlus = false
fun initGdiPlusOnce() {
	if (initializedGdiPlus) return
	initializedGdiPlus = true
	memScoped {
		val ptoken = allocArray<ULONG_PTRVar>(1)
		val si = alloc<GdiplusStartupInput>().apply {
			GdiplusVersion = 1
			DebugEventCallback = null
			SuppressExternalCodecs = FALSE
			SuppressBackgroundThread = FALSE
		}
		GdiplusStartup(ptoken, si.ptr, null)
	}
}

/*
@Suppress("unused")
class MyMemoryStream(rawPtr: kotlinx.cinterop.NativePtr) : CStructVar(rawPtr) {
    var pad0: Long = 0L
    var pad1: Long = 0L
    var position: Long = 0
    var length: Long = 0
    var data: CPointer<ByteVar>? = null

    //companion object : kotlinx.cinterop.CStructVar.Type(64, 16)
    //companion object
    companion object : kotlinx.cinterop.CStructVar.Type(64, 1)
}

@Suppress("unused")
fun MemoryIStreamAddRef(s: CPointer<MyMemoryStream>?): ULONG {
    val ms = s!!.pointed
    //println("MemoryIStreamAddRef() : ${ms.pad0}, ${ms.pad1}, ${ms.data}, ${ms.position}, ${ms.length}")
    return 0
}

@Suppress("unused")
fun MemoryIStreamRelease(s: CPointer<MyMemoryStream>?): ULONG {
    val ms = s!!.pointed
    //println("MemoryIStreamRelease() : ${ms.pad0}, ${ms.pad1}, ${ms.data}, ${ms.position}, ${ms.length}")
    return 0
}

//HRESULT Stat([out] STATSTG *pstatstg, [in]  DWORD   grfStatFlag );
@Suppress("unused")
fun MemoryIStreamStat(s: CPointer<MyMemoryStream>?, pstatstg: CPointer<STATSTG>?, grfStatFlag: DWORD): ULONG {
    val ms = s!!.pointed
    //println("MemoryIStreamStat() : ${ms.pad0}, ${ms.pad1}, ${ms.data}, ${ms.position}, ${ms.length}")
    pstatstg!!.pointed.cbSize.QuadPart = ms.length
    return 0
}

//fun MemoryIStreamSetSize(s: CPointer<IStream>?, newSize: CValue<ULARGE_INTEGER>): HRESULT  {
@Suppress("unused")
fun MemoryIStreamSetSize(s: CPointer<MyMemoryStream>?, newSize: Long): HRESULT {
    val ms = s!!.pointed
    //println("MemoryIStreamSetSize() : ${ms.pad0}, ${ms.pad1}, ${ms.data}, ${ms.position}, ${ms.length} ---> $newSize")
    return 0
}

//HRESULT Seek([in]  LARGE_INTEGER  dlibMove,[in]  DWORD          dwOrigin,[out] ULARGE_INTEGER *plibNewPosition);
//fun MemoryIStreamSeek(s: CPointer<IStream>?, dlibMove: CValue<LARGE_INTEGER>, dwOrigin: DWORD, plibNewPosition: CPointer<ULARGE_INTEGER>?): HRESULT  {
@Suppress("unused")
fun MemoryIStreamSeek(s: CPointer<MyMemoryStream>?, dlibMove: Long, dwOrigin: DWORD, plibNewPosition: CPointer<ULARGE_INTEGER>?): HRESULT {
    val ms = s!!.pointed
    //println("MemoryIStreamSeek: : ${ms.pad0}, ${ms.pad1}, ${ms.data}, ${ms.position}, ${ms.length} ---> $dlibMove, $dwOrigin, $plibNewPosition")
    ms.position = when (dwOrigin) {
        platform.windows.SEEK_SET -> dlibMove
        platform.windows.SEEK_CUR -> ms.position + dlibMove
        platform.windows.SEEK_SET -> ms.length + dlibMove
        else -> TODO("Invalid $dwOrigin seek")
    }
    plibNewPosition?.pointed?.QuadPart = ms.position
    return 0
}

// https://msdn.microsoft.com/en-us/library/ms890697.aspx
//HRESULT Read(void* pv, ULONG cb, ULONG* pcbRead);
@Suppress("unused")
fun MemoryIStreamRead(s: CPointer<MyMemoryStream>?, pv: CPointer<ByteVar>?, cb: ULONG, pcbRead: CPointer<ULONGVar>?): HRESULT {
    val ms = s!!.pointed
    //println("MemoryIStreamRead: : ${ms.pad0}, ${ms.pad1}, ${ms.data}, ${ms.position}, ${ms.length} ---> $pv, $cb, $pcbRead")

    val remaining = ms.length - ms.position
    val toRead = kotlin.math.min(remaining.toLong(), cb.toLong()).toInt()
    for (n in 0 until toRead) {
        //println("READ: $n .. ${toRead}")
        pv!![n] = ms.data!![n]
    }
    pcbRead?.pointed?.value = toRead

    return 0
}

fun gdipKmlLoadImageFromByteArray(data: ByteArray): KmlNativeNativeImageData {
    return memScoped {
        data.usePinned { datap ->
            val streamData = alloc<MyMemoryStream>()
            val pstream = streamData.uncheckedCast<IStream>()
            val pstreamBA = streamData.uncheckedCast<MyMemoryStream>()
            val pimage = allocArray<COpaquePointerVar>(1)
            val width = alloc<FloatVar>()
            val height = alloc<FloatVar>()
            val streamVtbl = alloc<IStreamVtbl>().apply {
                this.AddRef = staticCFunction(::MemoryIStreamAddRef).uncheckedCast()
                this.Release = staticCFunction(::MemoryIStreamRelease).uncheckedCast()
                this.SetSize = staticCFunction(::MemoryIStreamSetSize).uncheckedCast()
                this.Read = staticCFunction(::MemoryIStreamRead).uncheckedCast()
                this.Seek = staticCFunction(::MemoryIStreamSeek).uncheckedCast()
                this.Stat = staticCFunction(::MemoryIStreamStat).uncheckedCast()
            }
            pstream.lpVtbl = streamVtbl.ptr

            pstreamBA.position = 0
            pstreamBA.length = data.size.toLong()
            pstreamBA.data = datap.addressOf(0)


            initGdiPlusOnce()
            val res = GdipCreateBitmapFromStream(pstream.ptr, pimage)
            if (res != 0) {
                throw RuntimeException("Can't load image from Bytes(${data.size})")
            }

            //println(width.value.toInt())
            //println(height.value.toInt())

            GdipGetImageDimension(pimage[0], width.ptr, height.ptr)
            val iwidth = width.value.toInt()
            val iheight = height.value.toInt()

            val rect = alloc<GpRect>().apply {
                X = 0
                Y = 0
                Width = width.value.toInt()
                Height = height.value.toInt()
            }
            val bmpData = alloc<BitmapData>()
            val res2 = GdipBitmapLockBits(pimage[0], rect.ptr, ImageLockModeRead, PixelFormat32bppARGB, bmpData.ptr)
            if (res2 != 0) {
                throw RuntimeException("Can't load image from Bytes(${data.size})")
            }
            //println("res2: $res2")
            //println(bmpData.Width)
            //println(bmpData.Height)
            //println(bmpData.Stride)
            //println(bmpData.Scan0)
            val out = KmlIntBuffer(bmpData.Width * bmpData.Height)
            var n = 0
            for (y in 0 until bmpData.Height) {
                val p = (bmpData.Scan0.toLong() + (bmpData.Stride * y)).toCPointer<IntVar>()
                for (x in 0 until bmpData.Width) {
                    out[n] = p!![x]
                    n++
                }
            }

            GdipBitmapUnlockBits(pimage[0], bmpData.ptr)
            GdipDisposeImage(pimage[0])

            //println(out.toList())

            println("Loaded image Bytes(${data.size}) ($iwidth, $iheight)")

            KmlNativeNativeImageData(iwidth, iheight, out)
        }
    }
}
*/


fun gdipKmlLoadImageFromByteArray(data: ByteArray): KmlNativeNativeImageData {
	return memScoped {
		val width = alloc<FloatVar>()
		val height = alloc<FloatVar>()
		val pimage = allocArray<COpaquePointerVar>(1)

		initGdiPlusOnce()
		data.usePinned { datap ->
			val pdata = datap.addressOf(0)
			val pstream = SHCreateMemStream(pdata, data.size)!!
			try {
				if (GdipCreateBitmapFromStream(pstream, pimage) != 0) {
					throw RuntimeException("Can't load image from byte array")
				}
			} finally {
				pstream.pointed.lpVtbl?.pointed?.Release?.invoke(pstream)
			}
		}

		GdipGetImageDimension(pimage[0], width.ptr, height.ptr)

		val rect = alloc<GpRect>().apply {
			X = 0
			Y = 0
			Width = width.value.toInt()
			Height = height.value.toInt()
		}
		val bmpData = alloc<BitmapData>()
		if (GdipBitmapLockBits(pimage[0], rect.ptr, ImageLockModeRead, PixelFormat32bppARGB, bmpData.ptr) != 0) {
			throw RuntimeException("Can't lock image")
		}

		val out = IntArray(bmpData.Width * bmpData.Height)
		var n = 0
		for (y in 0 until bmpData.Height) {
			val p = (bmpData.Scan0.toLong() + (bmpData.Stride * y)).toCPointer<IntVar>()
			for (x in 0 until bmpData.Width) {
				out[n] = p!![x]
				n++
			}
		}

		GdipBitmapUnlockBits(pimage[0], bmpData.ptr)
		GdipDisposeImage(pimage[0])

		//println(out.toList())
		KmlNativeNativeImageData(width.value.toInt(), height.value.toInt(), out)
	}
}

fun gdipKmlLoadImage(imageName: String): KmlNativeNativeImageData {
	return memScoped {
		val pimage = allocArray<COpaquePointerVar>(1)
		val width = alloc<FloatVar>()
		val height = alloc<FloatVar>()

		println("Loading image $imageName...")

		initGdiPlusOnce()
		val res = GdipCreateBitmapFromFile(imageName.wcstr, pimage)
		if (res != 0) {
			throw RuntimeException("Can't find image $imageName")
		}

		GdipGetImageDimension(pimage[0], width.ptr, height.ptr)
		val iwidth = width.value.toInt()
		val iheight = height.value.toInt()

		val rect = alloc<GpRect>().apply {
			X = 0
			Y = 0
			Width = iwidth
			Height = iheight
		}
		val bmpData = alloc<BitmapData>()
		val res2 = GdipBitmapLockBits(pimage[0], rect.ptr, ImageLockModeRead, PixelFormat32bppARGB, bmpData.ptr)
		//println("res2: $res2")
		//println(bmpData.Width)
		//println(bmpData.Height)
		//println(bmpData.Stride)
		//println(bmpData.Scan0)
		val out = IntArray(bmpData.Width * bmpData.Height)
		var n = 0
		for (y in 0 until bmpData.Height) {
			val p = (bmpData.Scan0.toLong() + (bmpData.Stride * y)).toCPointer<IntVar>()
			for (x in 0 until bmpData.Width) {
				out[n] = p!![x]
				n++
			}
		}

		GdipBitmapUnlockBits(pimage[0], bmpData.ptr)
		GdipDisposeImage(pimage[0])

		//println(out.toList())

		println("Loaded image $imageName ($iwidth, $iheight)")

		KmlNativeNativeImageData(iwidth, iheight, out)
	}
}
