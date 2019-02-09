import java.util.*

operator fun File.get(name: String) = File(this, name)

org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/.gitignore")

fun copyTemplate(template: File, project: File) {
	println("$template -> $project")
	project["settings.gradle.kts"].delete()
	//project["buildSrc"].deleteRecursively()
	sync {
		from(template["buildSrc"])
		into(project["buildSrc"])
		//include("**/*")
	}
	copy {
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

		// Into
		into(project)
	}
	sync {
		from(template["gradle"])
		into(project["gradle"])
	}
}

val kortemplateDir = rootDir["kortemplate"]
val PROJECT_DIRS = rootDir.listFiles().filter { it["gradle.properties"].exists() && it != kortemplateDir }

fun File.properties() = Properties().also { it.load(this.readText().reader()) }

val versions by lazy {
	PROJECT_DIRS.associate {
		val properties = it["gradle.properties"].properties()
		it.name to (properties["version"] ?: properties["projectVersion"] ?: "unknown")
	}
}


tasks.create("copyTemplate") {
	inputs.dir(kortemplateDir)
	outputs.dirs(PROJECT_DIRS)
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

tasks.create("updateVersions") {
	doLast {
		for (projectDir in PROJECT_DIRS) {
			projectDir["gradle.properties"].replaceVersions()
		}
		rootDir["korge/plugins/gradle.properties"].replaceVersions()
	}
}

tasks.create("versions") {
	doLast {
		for (version in versions) {
			println(version)
		}
	}
}
