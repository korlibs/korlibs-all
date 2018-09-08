package com.soywiz.example1

object JvmExample1 {
    @JvmStatic fun main(args: Array<String>) = example2.main(args)
}


//fun com.soywiz.example1.main(args: Array<String>): Unit = Korio {
//    val vfs = MemoryVfsMix("hello" to "WORLD")
//    println("HELLO ${vfs["hello"].readString()} FROM KORIO")
//}

