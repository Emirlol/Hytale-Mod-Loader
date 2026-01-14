plugins {
	alias(libs.plugins.shadow)
	`java-library`
	base
}

repositories {

	// SpongePowered Maven repository for Mixins
	maven("https://repo.spongepowered.org/maven/") {
		name = "SpongePowered"
	}
	// FabricMC Maven repository for Fabric Loader
	maven("https://maven.fabricmc.net/") {
		name = "FabricMC"
	}

	// Maven Central for core dependencies
	mavenCentral() {
		content {
			// Force ASM and ME to come from the fabric maven.
			// This ensures that the version has been mirrored for use by the launcher/installer.
			excludeGroupByRegex("org.ow2.asm")
			excludeGroupByRegex("io.github.llamalad7")
		}
	}
}


version = property("provider_version") as String
group = "me.ancientri"

base {
	archivesName = "hytale-fabric-modloader"
}

dependencies {
	api(libs.guava)
	api(libs.gson)

	// Fabric dependencies
	api(libs.fabricLoader)

	// Mixin dependencies

	api(libs.mixin) {
		exclude(module = "launchwrapper")
		exclude(group = "com.google.guava")
		exclude(group = "com.google.code.gson")
	}
}

sourceSets {
	main {
		java {
			srcDir("src")
		}
	}
}

val gamePath = "$projectDir/run"

tasks {
	shadowJar {
		archiveClassifier.set("")
		manifest {
			attributes(
				mapOf(
					"Main-Class" to "net.fabricmc.loader.launch.knot.KnotServer",
					"Multi-Release" to true
				)
			)
		}
	}
	assemble {
		dependsOn("shadowJar")
	}
	register<Copy>("copyJar") {
		group = "build"
		dependsOn("jar", "shadowJar")
		from(shadowJar.get().archiveFile)
		into(gamePath)
	}
	register<JavaExec>("runServer") {
		description = "Runs the server in projectRoot/run."
		group = "run"
		dependsOn("copyJar")
		workingDir = file(gamePath)
		mainClass = "net.fabricmc.loader.launch.knot.KnotServer"
		classpath = files("$gamePath/HytaleServer.jar", "$gamePath/${shadowJar.get().archiveFileName.get()}")
		jvmArgs = listOf("-Dfabric.skipMcProvider=true")
		doLast {
			exec()
		}
		isIgnoreExitValue = true
	}
}
