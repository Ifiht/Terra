import org.gradle.kotlin.dsl.register
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("xyz.jpenilla.run-paper") version Versions.Bukkit.runPaper
}

dependencies {
    shaded(project(":platforms:bukkit:common"))
    shaded(project(":platforms:bukkit:nms:v1_21_3", configuration = "reobf"))
    shaded("xyz.jpenilla", "reflection-remapper", Versions.Bukkit.reflectionRemapper)
}

tasks {
    shadowJar {
        relocate("io.papermc.lib", "com.dfsek.terra.lib.paperlib")
        relocate("com.google.common", "com.dfsek.terra.lib.google.common")
        relocate("org.apache.logging.slf4j", "com.dfsek.terra.lib.slf4j-over-log4j")
        exclude("org/slf4j/**")
        exclude("org/checkerframework/**")
        exclude("org/jetbrains/annotations/**")
        exclude("org/intellij/**")
        exclude("com/google/errorprone/**")
        exclude("com/google/j2objc/**")
        exclude("javax/**")
    }

    runServer {
        minecraftVersion(Versions.Bukkit.minecraft)
        dependsOn(shadowJar)
        pluginJars(shadowJar.get().archiveFile)
        downloadPlugins {
            modrinth("viaversion", "5.2.0")
            modrinth("viabackwards", "5.2.0")
        }
    }
}

// Test Paper run & immediately shut down, for github actions
tasks.register<RunServer>("runServerTest") {
    dependsOn(tasks.shadowJar)
    // Accept a Minecraft version via -PmcVersion=1.21.5, default to 1.21.4
    val mcVersion = project.findProperty("mcVersion") as String? ?: "1.21.4"
    minecraftVersion(mcVersion)
    downloadPlugins {
        github("Ifiht", "AutoStop", "v1.2.0", "AutoStop-1.2.0.jar")
    }
    pluginJars.from(tasks.shadowJar)
}
// Start a local test server for login & manual testing
tasks.register<RunServer>("runServerInteractive_1-21-4") {
    dependsOn(tasks.shadowJar)
    minecraftVersion("1.21.4")
    downloadPlugins {
        hangar("Multiverse-Core", "5.6.1")
    }
    pluginJars.from(tasks.shadowJar)
}


addonDir(project.file("./run/plugins/Terra/addons"), tasks.named("runServer").get())
