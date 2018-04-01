import java.io.*

interface Printer {
	fun println(str: String)
}

fun printToConsole(callback: Printer.() -> Unit) {
	callback(object : Printer {
		override fun println(str: String) = kotlin.io.println(str)
	})
}

fun printToFile(file: String, callback: Printer.() -> Unit) {
	val lines = arrayListOf<String>()
	callback(object : Printer {
		override fun println(str: String) = run { lines += str }
	})
	File(file).parentFile.mkdirs()
	File(file).writeText(lines.joinToString("\n"))
}
