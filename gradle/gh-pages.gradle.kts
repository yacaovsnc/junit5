apply(plugin = "org.ajoberstar.git-publish")

val docsDir = file("$buildDir/ghpages-docs")
val replaceCurrentDocs = project.hasProperty("replaceCurrentDocs")
val docsVersion = rootProject.extra["docsVersion"]

val prepareDocsForUploadToGhPages by tasks.registering(Copy::class) {
	dependsOn("aggregateJavadocs", ":documentation:asciidoctor")
	outputs.dir(docsDir)

	from("${project(":documentation").buildDir}/checksum") {
		include("published-checksum.txt")
	}
	from("${project(":documentation").buildDir}/asciidoc") {
		include("user-guide/**")
		include("release-notes/**")
	}
	from("$buildDir/docs") {
		include("javadoc/**")
		filesMatching("**/*.html") {
			val favicon = "<link rel=\"icon\" type=\"image/png\" href=\"http://junit.org/junit5/assets/img/junit5-logo.png\">"
			filter { line ->
				if (line.startsWith("<head>")) line.replace("<head>", "<head>$favicon") else line
			}
		}
	}
	into("${docsDir}/${docsVersion}")
	filesMatching("javadoc/**") {
		path = path.replace("javadoc/", "api/")
	}
	includeEmptyDirs = false
}

val createCurrentDocsFolder by tasks.registering(Copy::class) {
	dependsOn(prepareDocsForUploadToGhPages)
	outputs.dir("${docsDir}/current")
	onlyIf { replaceCurrentDocs }

	from("${docsDir}/${docsVersion}")
	into("${docsDir}/current")
}

configure<org.ajoberstar.gradle.git.publish.GitPublishExtension> {
	repoUri.set("https://github.com/junit-team/junit5.git")
	branch.set("gh-pages")

	contents {
		from(docsDir)
		into("docs")
	}

	preserve {
		include("**/*")
		exclude("docs/${docsVersion}/**")
		if (replaceCurrentDocs) {
			exclude("docs/current/**")
		}
	}
}

tasks.named<org.ajoberstar.gradle.git.publish.tasks.GitPublishCommit>("gitPublishCommit") {
	dependsOn(prepareDocsForUploadToGhPages, createCurrentDocsFolder)
}
