fun Project.nextSync(from: String, into: String) {
	sync {
		from(File(project.projectDir, "../$from/src"))
		into(File(project.projectDir, "../korge-next/$into/src"))
	}
}

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
	val repoList = listOf("kbignum", "kbox2d", "kds", "klock", "klogger", "kmem", "korau", "korge", "korgw", "korim", "korinject", "korio", "korma", "korte", "korvi", "krypto", "luak")

	val gitSyncMaster by creating(Task::class) {
		doLast {
			for (repo in repoList) syncMaster(repo)
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
			nextSync("kbignum/kbignum", "kbignum")
			nextSync("kbox2d/kbox2d", "kbox2d")
			nextSync("kds/kds", "kds")
		}
	}
}