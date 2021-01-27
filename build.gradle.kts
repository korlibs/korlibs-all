plugins {
	idea
}

val rootDirectory = rootDir

fun nextSyncCommon(current: String, next: String, toNext: Boolean) {
	sync {
		val currentDir = File(rootDirectory, "$current/src")
		val nextDir = File(project.projectDir, "korge-next/$next/src")
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

fun <T> retry(times: Int = 5, block: () -> T): T {
	var lastE: Throwable? = null
	for (n in 0 until times) {
		try {
			return block()
		} catch (e: Throwable) {
			lastE = e
		}
	}
	throw lastE ?: Exception("retry.times = 0")
}

fun nextSync(current: String, next: String) = nextSyncCommon(current, next, toNext = true)
fun nextUnsync(current: String, next: String) = nextSyncCommon(current, next, toNext = false)

fun Project.syncMaster(pname: String) {
	val wdir = File(rootDirectory, "$pname")
	println("## SYNC $pname...")
	exec {
		workingDir(wdir)
		commandLine("git", "checkout", "master")
	}
	retry {
		exec {
			workingDir(wdir)
			commandLine("git", "pull")
		}
	}
}

fun generatePairForRepo(subproject: String): Pair<String, String> {
	val project = subproject.substringBefore('-')
	return "$project/$subproject" to subproject
}

tasks {
	val subprojects = listOf(
		"kbignum", "kbox2d", "kds", "klock", "klogger", "kmem", "korau", "korge",
		"korge-dragonbones", "korge-spine", "korge-swf", "korgw", "korim", "korinject",
		"korio", "korma", "korma-shape", "korte", "korvi", "krypto", "luak"
	)
	val repoList = subprojects.map { it.substringBefore('-') }.toSet().toList()

	val syncPairs = subprojects.map { subproject ->
		generatePairForRepo(subproject)
	} + listOf(
			"korge-plugins/korge-gradle-plugin" to "korge-gradle-plugin"
	)

	val gitSyncMaster by creating(Task::class) {
		group = "sync"
		doLast {
			for (repo in repoList + listOf("korge-plugins")) syncMaster(repo)
		}
	}
	val addGitAttributes by creating(Task::class) {
		group = "sync"
		doLast {
			for (repo in repoList) {
				File(project.projectDir, "../$repo/.gitattributes").writeText("* text=auto eol=lf\n")
			}
		}
	}

	val copyToNext by creating(Task::class) {
		group = "sync"
		doLast {
			for ((current, next) in syncPairs) {
				nextSync(current, next)
			}
		}
	}


	for (repo in repoList + listOf("korgePlugin")) {
		create("copy${repo.capitalize()}ToNext") {
			group = "sync"
			doLast {
				if (repo == "korgePlugin") {
					nextSync("korge-plugins/korge-gradle-plugin", "korge-gradle-plugin")
				} else {
					val (current, next) = generatePairForRepo(repo)
					nextSync(current, next)
				}
			}
		}
	}

	val copyFromNext by creating(Task::class) {
		group = "sync"
		doLast {
			for ((current, next) in syncPairs) {
				nextUnsync(current, next)
			}
		}
	}
}



idea {
	module {
		excludeDirs = rootDir.listFiles().filter { it.isDirectory }.toSet()
	}
}
