operator fun File.get(name: String) = File(this, name)

fun copyTemplate(template: File, project: File) {
	println("$template -> $project")
	project["settings.gradle.kts"].delete()
	copy {
		from(template["buildSrc"])
		into(project["buildSrc"])
	}
	copy {
		from(template["settings.gradle"])
		into(project)
	}
}

val PROJECTS = listOf(
		"kds", "kmem", "klock", "korinject", "krypto", "kbox2d", "kbignum",
		"korma", "korio",
		"korim", "korau",
		"korui",
		"korge"
)

tasks.create("copyTemplate") {
	doLast {
		for (project in PROJECTS) {
			copyTemplate(rootDir["kortemplate"], rootDir[project])
		}
	}
}
