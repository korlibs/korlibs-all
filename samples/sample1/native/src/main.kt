import com.soywiz.korio.*
import com.soywiz.korio.file.std.*

//fun main(args: Array<String>) = example.main(args)

fun main(args: Array<String>): Unit = Korio {
    val vfs = MemoryVfsMix("hello" to "WORLD")
    println("HELLO ${vfs["hello"].readString()} FROM KORIO")
}

