fun Project.nextSyncCommon(current: String, next: String, toNext: Boolean) {
	sync {
		val currentDir = File(project.projectDir, "../$current/src")
		val nextDir = File(project.projectDir, "../korge-next/$next/src")
		val dirStr = if (toNext) "->" else "<-"

		println("current=$currentDir $dirStr next=$nextDir")

		if (toNext) {
			from(currentDir)
			into(nextDir)
		} else {
			from(nextDir)
			into(currentDir)
		}
	}
}

fun Project.nextSync(current: String, next: String) = nextSyncCommon(current, next, toNext = true)
fun Project.nextUnsync(current: String, next: String) = nextSyncCommon(current, next, toNext = false)

fun Project.syncMaster(pname: String) {
	val wdir = File(project.projectDir, "../$pname")
	exec {
		workingDir(wdir)
		commandLine("git", "checkout", "master")
	}
	exec {
		workingDir(wdir)
		commandLine("git", "pull")
	}
}

tasks {
	val subprojects = listOf(
		"kbignum", "kbox2d", "kds", "klock", "klogger", "kmem", "korau", "korge",
		"korge-dragonbones", "korge-spine", "korge-swf", "korgw", "korim", "korinject",
		"korio", "korma", "korma-shape", "korte", "korvi", "krypto", "luak"
	)
	val repoList = subprojects.map { it.substringBefore('-') }.toSet().toList()

	val syncPairs = subprojects.map { subproject ->
		val project = subproject.substringBefore('-')
		"$project/$subproject" to subproject
	} + listOf(
			"korge-plugins/korge-gradle-plugin" to "korge-gradle-plugin"
	)

	val gitSyncMaster by creating(Task::class) {
		doLast {
			for (repo in repoList + listOf("korge-plugins")) syncMaster(repo)
		}
	}
	val addGitAttributes by creating(Task::class) {
		doLast {
			for (repo in repoList) {
				File(project.projectDir, "../$repo/.gitattributes").writeText("* text=auto eol=lf\n")
			}
		}
	}

	val copyToNext by creating(Task::class) {
		doLast {
			for ((current, next) in syncPairs) {
				nextSync(current, next)
			}
		}
	}

	val copyFromNext by creating(Task::class) {
		doLast {
			for ((current, next) in syncPairs) {
				nextUnsync(current, next)
			}
		}
	}
}
