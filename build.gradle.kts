import java.util.*

operator fun File.get(name: String) = File(this, name)

org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitignore")

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
