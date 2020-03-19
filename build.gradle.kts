import java.util.*
import java.io.*
import java.lang.StringBuilder

val easyGradlePluginVersion: String by project

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

fun File.execInDir(
		vararg cmds: String,
		envs: Map<String, String> = mapOf(),
		passthru: Boolean = true,
		captureOutput: Boolean = true,
		onOut: (data: ByteArray) -> Unit = {},
		onErr: (data: ByteArray) -> Unit = {}
): ShellExecResult {
	return shellExec(*cmds, workingDir = this, envs = envs, passthru = passthru, captureOutput = captureOutput, onOut = onOut, onErr = onErr)
}

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
		// Editorconfig
		from(template[".editorconfig"])
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

data class ProjectInfo(val projectDir: File) {
	val readmeFile = projectDir["README.md"]
	val propertiesFile = projectDir["gradle.properties"]
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
	"korgw",
	"krypto"
)
val PROJECT_DIRS = PROJECT_NAMES.map { rootDir[it] }.filter { it.exists() }
val PROJECT_INFOS = PROJECT_DIRS.map { ProjectInfo(it) }

val README_FILES = PROJECT_DIRS.map { File(it, "README.md") }


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

tasks {
	val updateSponsor by creating {
		group = "readme"
		//inputs.dir(kortemplateDir)
		//outputs.dirs(PROJECT_DIRS)

		inputs.files(README_FILES)

		doLast {
			for (projectInfo in PROJECT_INFOS) {
				val projectDir = projectInfo.projectDir
				val readmeFile = projectInfo.readmeFile
				val projectProperties = projectInfo.propertiesFile.properties()
				//copyTemplate(kortemplateDir, projectDir)

				val projectName = readmeFile.parentFile.name

				println("$readmeFile : $projectName")

				val readmeText = readmeFile.takeIf { it.exists() }?.readText() ?: ""

				val supportContent = """
				<!-- SUPPORT -->
				<h2 align="center">Support $projectName</h2>
				<p align="center">
				If you like $projectName, or want your company logo here, please consider <a href="https://github.com/sponsors/soywiz">becoming a sponsor ★</a>,<br />
				in addition to ensure the continuity of the project, you will get exclusive content.
				</p>
				<!-- /SUPPORT -->
			""".trimIndent()

				//println(readmeText.match(Regex("<!-- SUPPORT -->.*?<!-- /SUPPORT -->")))

				val newReadme = if (readmeText.contains("<!-- SUPPORT -->")) {
					readmeText.replace(Regex("<!-- SUPPORT -->.*<!-- /SUPPORT -->", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))) {
						supportContent
					}
				} else {
					var foundPlace = false
					var emptySpaces = 0

					readmeText.trim().lines().withIndex().map { (index, it) ->
						if (it.trim() == "") {
							emptySpaces++
						}
						if (!foundPlace && emptySpaces >= 2) {
							foundPlace = true
							"$it\n" + supportContent + "\n"
						} else {
							it
						}
					}.joinToString("\n") + "\n"
				}

				readmeFile.writeText(newReadme)
			}
		}
	}

	val updateBadges by creating {
		group = "readme"
		//inputs.dir(kortemplateDir)
		//outputs.dirs(PROJECT_DIRS)

		inputs.files(README_FILES)

		doLast {
			for (projectInfo in PROJECT_INFOS) {
				val projectDir = projectInfo.projectDir
				val readmeFile = projectInfo.readmeFile
				val projectProperties = projectInfo.propertiesFile.properties()

				val bintrayOrg = projectProperties["project.bintray.org"]
				val bintrayRepo = projectProperties["project.bintray.repository"]
				val bintrayPackage = projectProperties["project.bintray.package"]
				val bintrayPath = "" + bintrayOrg + "/" + bintrayRepo + "/" + bintrayPackage

				val githubOrg = "korlibs"
				val githubRepo = bintrayPackage

				val bintrayUrl = "https://bintray.com/$bintrayPath"

				//copyTemplate(kortemplateDir, projectDir)

				val projectName = readmeFile.parentFile.name

				println("$readmeFile : $projectName")

				val readmeText = readmeFile.takeIf { it.exists() }?.readText() ?: ""
				val supportContent = """
				<!-- BADGES -->
				<p align="center">
					<a href="https://github.com/$githubOrg/$githubRepo/actions"><img alt="Build Status" src="https://github.com/$githubOrg/$githubRepo/workflows/CI/badge.svg" /></a>
					<a href="$bintrayUrl"><img alt="Maven Version" src="https://img.shields.io/bintray/v/$bintrayPath.svg?style=flat&label=maven" /></a>
					<a href="https://slack.soywiz.com/"><img alt="Slack" src="https://img.shields.io/badge/chat-on%20slack-green?style=flat&logo=slack" /></a>
				</p>
				<!-- /BADGES -->
			""".trimIndent()

				//println(readmeText.match(Regex("<!-- SUPPORT -->.*?<!-- /SUPPORT -->")))

				val newReadme = if (readmeText.contains("<!-- BADGES -->")) {
					readmeText.replace(Regex("<!-- BADGES -->.*<!-- /BADGES -->", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))) {
						supportContent
					}
				} else {
					var foundPlace = false
					var emptySpaces = 0

					readmeText.trim().lines().withIndex().map { (index, it) ->
						if (it.trim() == "") {
							emptySpaces++
						}
						if (!foundPlace && emptySpaces >= 2) {
							foundPlace = true
							"$it\n" + supportContent + "\n"
						} else {
							it
						}
					}.joinToString("\n") + "\n"
				}

				readmeFile.writeText(newReadme)
			}
		}
	}

	val updateReadme by creating {
		dependsOn(updateSponsor)
		dependsOn(updateBadges)
	}

	val copyTemplate by creating {
		group = "sync"
		//inputs.dir(kortemplateDir)
		//outputs.dirs(PROJECT_DIRS)
		doLast {
			for (projectDir in PROJECT_DIRS) {
				copyTemplate(kortemplateDir, projectDir)
			}
		}
	}

	val gitPushUpdateTemplate by creating {
		group = "zgit"
		//inputs.dir(kortemplateDir)
		//outputs.dirs(PROJECT_DIRS)
		doLast {
			for (projectDir in PROJECT_DIRS) {
				projectDir.execInDir("git", "add", "-A")
				projectDir.execInDir("git", "commit", "-m", "Updated template")
				projectDir.execInDir("git", "push")
				projectDir.execInDir("git", "add", projectDir.name)
			}
		}
	}

	val gitPull by creating {
		group = "zgit"
		//inputs.dir(kortemplateDir)
		//outputs.dirs(PROJECT_DIRS)
		doLast {
			for (projectDir in PROJECT_DIRS) {
				projectDir.execInDir("git", "pull")
			}
		}
	}

	fun File.writeLines(lines: List<String>) {
		this.writeText(lines.joinToString("\n") + "\n")
	}

	fun File.addLineOnce(line: String) {
		if (this.exists()) {
			val lines = this.readLines().toMutableList()
			if (!lines.contains(line)) {
				lines.add(line)
				this.writeLines(lines)
			}
		}
	}

	val addKotlinNativeIgnoreDisabledTargets by creating {
		doLast {
			for (projectDir in PROJECT_DIRS) {
				projectDir["gradle.properties"].addLineOnce("kotlin.native.ignoreDisabledTargets=true")
			}
		}
	}

	val updateEasyKotlinMppGradlePlugin by creating {
		doLast {
			for (projectDir in PROJECT_DIRS) {
				val buildGradleFile = projectDir["build.gradle"]
				val lines = buildGradleFile.readLines()
				val transformedLines = lines.map { line ->
					if (line.contains("easy-kotlin-mpp-gradle-plugin")) {
						println("LINE: $line")
						"        classpath \"com.soywiz.korlibs:easy-kotlin-mpp-gradle-plugin:$easyGradlePluginVersion\" // Kotlin 1.3.61: https://github.com/korlibs/easy-kotlin-mpp-gradle-plugin"
					} else {
						line
					}
				}
				buildGradleFile.writeLines(transformedLines)
				//println(transformedLines)
			}
		}
	}

	val migrateToGithubActions by creating {
		doLast {
			for (projectDir in PROJECT_DIRS) {
				//for (projectDir in listOf(rootDir["klock"])) {
				copy {
					from(rootDir["kortemplate/.github/workflows/CI.yml"])
					into("$projectDir/.github/workflows")
				}
				copy {
					from("$projectDir/.travis.yml")
					from("$projectDir/travis_win.bat")
					from("$projectDir/travis_win_bintray.bat")
					into("$projectDir/old")
				}
				File("$projectDir/.travis.yml").delete()
				File("$projectDir/travis_win.bat").delete()
				File("$projectDir/travis_win_bintray.bat").delete()
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

	val updateVersions by creating {
		group = "sync"
		doLast {
			for (projectDir in PROJECT_DIRS) {
				projectDir["gradle.properties"].replaceVersions()
			}
			rootDir["korge/plugins/gradle.properties"].replaceVersions()
		}
	}

	val versions by creating {
		group = "sync"
		doLast {
			for (version in versions) {
				println(version)
			}
		}
	}

	val synchronize by creating {
		group = "sync"
		dependsOn(updateVersions, copyTemplate)
	}

	val updateGradlewWine by creating {
		doLast {
			for (projectDir in PROJECT_DIRS) {
				projectDir["gradlew_wine"].writeText("#!/bin/bash\n unset ANDROID_HOME\n WINEDEBUG=-all wine64 cmd /c gradlew.bat $*")
			}
		}
	}

	val checkoutMasterPull by creating {
		doLast {
			for (projectDir in PROJECT_DIRS) {
				println("projectDir: $projectDir")
				projectDir.execInDir("git", "checkout", "master")
				projectDir.execInDir("git", "pull")
			}
		}
	}

	val updateCI by creating {
		doLast {
			for (projectDir in PROJECT_DIRS) {
				println("projectDir: $projectDir")
				sync {

					from(kortemplateDir[".github/workflows"])
					into(projectDir[".github/workflows"])
				}
				projectDir.execInDir("git", "add", "-A")
				projectDir.execInDir("git", "commit", "-m", "Updated CI")
				projectDir.execInDir("git", "push")
				projectDir.execInDir("git", "add", projectDir.name)
			}
		}
	}
}