object KmlGenBuffer {
    @JvmStatic
    fun main(args: Array<String>) {
        printToFile("kmedialayer/common/src/main/kotlin/com/soywiz/kmedialayer/KmlBuffer.kt") { generateCommon() }
        printToFile("kmedialayer/android/src/main/kotlin/com/soywiz/kmedialayer/KmlBufferAndroid.kt") { generateJvm() }
        printToFile("kmedialayer/jvm/src/main/kotlin/com/soywiz/kmedialayer/KmlBufferJvm.kt") { generateJvm() }
        printToFile("kmedialayer/js/src/main/kotlin/com/soywiz/kmedialayer/KmlBufferJs.kt") { generateJs() }
        printToFile("kmedialayer/native/src/main/kotlin/com/soywiz/kmedialayer/KmlBufferNative.kt") { generateNative() }
    }

    open class KmlType(val name: String, val size: Int) {
        open val bbMethodName: String = name
    }
    object KmlByte : KmlType("Byte", 1) {
        override val bbMethodName: String = ""
    }
    object KmlShort : KmlType("Short", 2)
    object KmlInt : KmlType("Int", 4)
    object KmlFloat : KmlType("Float", 4)
    val types = listOf(KmlByte, KmlShort, KmlInt, KmlFloat)

    fun Printer.generateCommon() {
        println("@file:Suppress(\"unused\", \"RedundantUnitReturnType\")")
        println("")
        println("package com.soywiz.kmedialayer")
        println("")
        println("fun kmlInternalRoundUp(n: Int, m: Int) = if (n >= 0) ((n + m - 1) / m) * m else (n / m) * m")
        println("")
        println("expect class KmlNativeBuffer {")
        println("    val size: Int")
        println("    constructor(size: Int)")
        println("")
        for (type in types) {
            println("    fun get${type.name}(index: Int): ${type.name}")
            println("    fun set${type.name}(index: Int, value: ${type.name}): Unit")
        }
        println("    fun dispose()")
        println("}")
        println("")
        /*
        for (type in types) {
            val name = type.name
            val bufferName = "Kml${name}Buffer"
            val arrayName = "${name}Array"

            println("class $bufferName(override val baseBuffer: KmlBufferBase) : KmlBuffer, Iterable<$name> {")
            println("    companion object {")
            println("        const val ELEMENT_SIZE = ${type.size}")
            println("        operator fun invoke(array: $arrayName): $bufferName = array.to${name}Buffer()")
            println("    }")
            println("    val size: Int = baseBuffer.size / ELEMENT_SIZE")
            println("    constructor(size: Int) : this(KmlBufferBase(size * ELEMENT_SIZE))")
            println("    inline operator fun get(index: Int): $name = baseBuffer.get$name(index)")
            println("    inline operator fun set(index: Int, value: $name): Unit = run { baseBuffer.set$name(index, value) }")
            println("    override fun iterator(): Iterator<$name> = object : Iterator<$name> {")
            println("        var pos = 0")
            println("        override fun hasNext(): Boolean = pos < size")
            println("        override fun next(): $name = get(pos++)")
            println("    }")
            println("    override fun toString(): String = \"$bufferName(\${this.toList()})\"")
            println("}")
            println("fun KmlBuffer.as${name}Buffer(): $bufferName = $bufferName(this.baseBuffer)")
            println("fun $arrayName.to${name}Buffer(): $bufferName = $bufferName(this.size).also { for (n in 0 until this.size) it[n] = this[n] }")
            println("fun $bufferName.to${name}Array(): $arrayName = $arrayName(this.size).also { for (n in 0 until this.size) it[n] = this[n] }")
            println("")
        }
        */
    }

    fun Printer.generateJvm() {
        println("package com.soywiz.kmedialayer")
        println("")
        println("import java.nio.*")
        println("")
        println("actual class KmlNativeBuffer constructor(val baseByteBuffer: ByteBuffer) {")
        println("    val byteBuffer = baseByteBuffer")
        println("    val shortBuffer = baseByteBuffer.asShortBuffer()")
        println("    val intBuffer = baseByteBuffer.asIntBuffer()")
        println("    val floatBuffer = baseByteBuffer.asFloatBuffer()")
        println("    actual val size: Int = baseByteBuffer.limit()")
        println("    actual constructor(size: Int) : this(ByteBuffer.allocateDirect(kmlInternalRoundUp(size, 8)).order(ByteOrder.nativeOrder()))")
        println("")
        for (type in types) {
            println("    actual fun get${type.name}(index: Int): ${type.name} = baseByteBuffer.get${type.bbMethodName}(index * ${type.size})")
            println("    actual fun set${type.name}(index: Int, value: ${type.name}): Unit = run { baseByteBuffer.put${type.bbMethodName}(index * ${type.size}, value) }")
        }
        println("    actual fun dispose() = Unit")
        println("}")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.nioBuffer: ByteBuffer get() = (this as KmlNativeBuffer).byteBuffer")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.nioByteBuffer: ByteBuffer get() = (this as KmlNativeBuffer).byteBuffer")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.nioShortBuffer: ShortBuffer get() = (this as KmlNativeBuffer).shortBuffer")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.nioIntBuffer: IntBuffer get() = (this as KmlNativeBuffer).intBuffer")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.nioFloatBuffer: FloatBuffer get() = (this as KmlNativeBuffer).floatBuffer")
        println("")
    }

    fun Printer.generateJs() {
        println("package com.soywiz.kmedialayer")
        println("")
        println("import org.khronos.webgl.*")
        println("")
        println("actual class KmlNativeBuffer constructor(val arrayBuffer: ArrayBuffer) {")
        println("   val arrayByte = Int8Array(arrayBuffer)")
        println("   val arrayUByte = Uint8Array(arrayBuffer)")
        println("   val arrayShort = Int16Array(arrayBuffer)")
        println("   val arrayInt = Int32Array(arrayBuffer)")
        println("   val arrayFloat = Float32Array(arrayBuffer)")
        println("   actual val size: Int = arrayBuffer.byteLength")
        println("   actual constructor(size: Int) : this(ArrayBuffer(kmlInternalRoundUp(size, 8)))")
        println("")
        for (type in types) {
            println("    actual fun get${type.name}(index: Int): ${type.name} = array${type.name}[index]")
            println("    actual fun set${type.name}(index: Int, value: ${type.name}): Unit = run { array${type.name}[index] = value }")
        }
        println("    actual fun dispose() = Unit")
        println("}")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.arrayBuffer: ArrayBuffer get() = (this as KmlNativeBuffer).arrayBuffer")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.arrayByte: Int8Array get() = (this as KmlNativeBuffer).arrayByte")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.arrayUByte: Uint8Array get() = (this as KmlNativeBuffer).arrayUByte")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.arrayShort: Int16Array get() = (this as KmlNativeBuffer).arrayShort")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.arrayInt: Int32Array get() = (this as KmlNativeBuffer).arrayInt")
        println("@Suppress(\"USELESS_CAST\") val KmlNativeBuffer.arrayFloat: Float32Array get() = (this as KmlNativeBuffer).arrayFloat")
        println("")
    }

    fun Printer.generateNative() {
        println("package com.soywiz.kmedialayer")
        println("")
        println("import konan.*")
        println("import kotlinx.cinterop.*")
        println("")
        println("actual class KmlNativeBuffer constructor(val placement: NativeFreeablePlacement, val ptr: NativePtr, actual val size: Int) {")
        println("    actual constructor(size: Int) : this(nativeHeap, nativeHeap.allocArray<ByteVar>(kmlInternalRoundUp(size, 8)).uncheckedCast(), size)")
        println("")
        for (type in types) {
            val tname = type.name
            val tsize = type.size
            println("    actual inline fun get$tname(index: Int): $tname = (ptr + index.toLong() * $tsize).uncheckedCast<${tname}VarOf<$tname>>().value")
            println("    actual inline fun set$tname(index: Int, value: $tname): Unit { (ptr + index.toLong() * $tsize).uncheckedCast<${tname}VarOf<$tname>>().value = value }")
        }
        println("    actual fun dispose() = run { placement.free(ptr) }")
        println("}")
        println("fun KmlNativeBuffer.unsafeAddress(): CPointer<ByteVar> = this.ptr.uncheckedCast()")
        println("")
    }
}