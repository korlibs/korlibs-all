import java.util.*
import java.io.*
import java.lang.StringBuilder

operator fun File.get(name: String) = File(this, name)

org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitignore")

fun InputStream.readAvailableChunk(readRest: Boolean): ByteArray {
	val out = ByteArrayOutputStream()
	while (if (readRest) true else available() > 0) {
		val c = this.read()
		if (c < 0) break
		out.write(c)
	}
	return out.toByteArray()
}

val java.lang.Process.isAliveJre7: Boolean get() = try { exitValue(); false  } catch (e: IllegalThreadStateException) { true }


fun shellExec(
	vararg cmds: String,
	workingDir: File = File("."),
	envs: Map<String, String> = mapOf(),
	passthru: Boolean = true,
	captureOutput: Boolean = true,
	onOut: (data: ByteArray) -> Unit = {},
	onErr: (data: ByteArray) -> Unit = {}
): ShellExecResult {

	val p = ProcessBuilder(*cmds)
		.directory(workingDir)
		.also { it.environment().also { it.putAll(envs) } }
		.start()
	var closing = false
	var output = StringBuilder()
	var error = StringBuilder()
	while (true) {
		val o = p.inputStream.readAvailableChunk(readRest = closing)
		val e = p.errorStream.readAvailableChunk(readRest = closing)
		if (passthru) {
			System.out.print(o.toString(Charsets.UTF_8))
			System.err.print(e.toString(Charsets.UTF_8))
		}
		if (captureOutput) {
			output.append(o.toString(Charsets.UTF_8))
			error.append(e.toString(Charsets.UTF_8))
		}
		if (o.isNotEmpty()) onOut(o)
		if (e.isNotEmpty()) onErr(e)
		if (closing) break
		if (o.isEmpty() && e.isEmpty() && !p.isAliveJre7) {
			closing = true
			continue
		}
		Thread.sleep(1L)
	}
	p.waitFor()
	//handler.onCompleted(p.exitValue())
	val exitCode = p.exitValue()
	return ShellExecResult(exitCode, output.toString(), error.toString())
}

data class ShellExecResult(
	val exitCode: Int,
	val output: String,
	val error: String
) {
	val outputAndError: String get() = "$output$error"
	val outputIfNotError get() = if (exitCode == 0) output else error("Error executing command: $outputAndError")
}



fun copyTemplate(template: File, project: File) {
	println("$template -> $project")
	project["settings.gradle.kts"].delete()
	project["buildSrc"].deleteRecursively()
	copy {
		from(template["build.gradle"])
		from(template["settings.gradle"])
		// Gradlew
		from(template["gradlew"])

		from(template["gradlew.bat"])
		from(template["gradlew_win"])
		from(template["gradlew_wine"])
		from(template["gradlew_linux"])
		// Publishing
		from(template["publish"])
		from(template["publish_local"])
		// Travis
		from(template[".travis.yml"])
		from(template["travis_win.bat"])
		from(template["travis_win_bintray.bat"])

		// Into
		into(project)
	}
	sync {
		from(template["gradle"])
		into(project["gradle"])
	}
}

val kortemplateDir = rootDir["kortemplate"]
val PROJECT_NAMES = listOf(
	"kbignum",
	"kbox2d",
	"kds",
	"klock",
	"klogger",
	"kmem",
	"korau",
	"korge",
	"korim",
	"korinject",
	"korio",
	"korma",
	"korte",
	"korui",
	"krypto"
)
val PROJECT_DIRS = PROJECT_NAMES.map { rootDir[it] }.filter { it.exists() }

fun File.properties() = Properties().also { it.load(this.readText().reader()) }

val versions by lazy {
	val versions = PROJECT_DIRS.associate {
		val properties = it["gradle.properties"].properties()
		it.name to (properties["version"] ?: properties["projectVersion"] ?: "unknown")
	}.toMutableMap()
	val koruiVersion = versions["korui"] ?: "unknown"
	versions["kgl"] = koruiVersion
	versions["korag"] = koruiVersion
	versions["korev"] = koruiVersion
	versions["korgw"] = koruiVersion
	versions["korag-opengl"] = koruiVersion
	versions
}

val copyTemplate = tasks.create("copyTemplate") {
	group = "sync"
	//inputs.dir(kortemplateDir)
	//outputs.dirs(PROJECT_DIRS)
	doLast {
		for (projectDir in PROJECT_DIRS) {
			copyTemplate(kortemplateDir, projectDir)
		}
	}
}

val gitPushUpdateTemplate = tasks.create("gitPushUpdateTemplate") {
	group = "zgit"
	//inputs.dir(kortemplateDir)
	//outputs.dirs(PROJECT_DIRS)
	doLast {
		for (projectDir in PROJECT_DIRS) {
			shellExec("git", "add", "-A", workingDir = projectDir)
			shellExec("git", "commit", "-m", "Updated template", workingDir = projectDir)
			shellExec("git", "push", workingDir = projectDir)
			shellExec("git", "add", projectDir.name, workingDir = rootDir)
		}
	}
}

fun String.replaceVersions(): String = replace(Regex("(.*?)Version\\s*=\\s*.*", RegexOption.MULTILINE)) {
	val name = it.groupValues[1]
	if (name in versions) {
		val version = versions[name]
		"${name}Version=$version"
	} else {
		it.value
	}
	//println(":: ${it.groupValues[1]}")
	//it.value
}

fun File.replaceVersions() {
	println("Replacing versions for $this ...")
	this.writeText(this.readText().replaceVersions())
}

val updateVersions = tasks.create("updateVersions") {
	group = "sync"
	doLast {
		for (projectDir in PROJECT_DIRS) {
			projectDir["gradle.properties"].replaceVersions()
		}
		rootDir["korge/plugins/gradle.properties"].replaceVersions()
	}
}

tasks.create("versions") {
	group = "sync"
	doLast {
		for (version in versions) {
			println(version)
		}
	}
}

val synchronize = tasks.create("synchronize") {
	group = "sync"
	dependsOn(updateVersions, copyTemplate)
}
