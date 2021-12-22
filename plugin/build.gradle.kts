plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("io.papermc.paperweight.userdev") version "1.3.3"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
    id("xyz.jpenilla.run-paper") version "1.0.4"
}

dependencies {
    implementation(project(":pl3xmap-api"))
    val cloudVersion = "1.5.0"
    implementation("cloud.commandframework", "cloud-paper", cloudVersion)
    implementation("cloud.commandframework", "cloud-minecraft-extras", cloudVersion)
    implementation("net.kyori", "adventure-text-minimessage", "4.1.0-SNAPSHOT")
    implementation("io.undertow", "undertow-core", "2.2.3.Final")
    implementation("org.bstats", "bstats-bukkit", "2.2.1")
    implementation("com.zaxxer", "HikariCP", "5.0.0")

    paperDevBundle("1.18.1-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveFileName.set("${rootProject.name}-${rootProject.version}-mojang-mapped.jar")
        archiveClassifier.set("mojang-mapped")

        minimize {
            exclude { it.moduleName == "pl3xmap-api" }
            exclude(dependency("io.undertow:.*:.*")) // does not like being minimized _or_ relocated (xnio errors)
        }
        listOf(
            "cloud.commandframework",
            "io.leangen.geantyref",
            "net.kyori.adventure.text.minimessage",
            "org.bstats"
        ).forEach { relocate(it, "${rootProject.group}.plugin.lib.$it") }
    }
    build {
        dependsOn(reobfJar)
    }
    runServer {
        minecraftVersion("1.18.1")
    }
}

runPaper {
    disablePluginJarDetection()
}

java {
    targetCompatibility = JavaVersion.toVersion(17)
    sourceCompatibility = JavaVersion.toVersion(17)
}

bukkit {
    main = "net.pl3x.map.plugin.Pl3xMapPlugin"
    name = rootProject.name
    apiVersion = "1.18"
    website = "https://github.com/NeumimTo/Pl3xMap"
    authors = listOf("BillyGalbreath", "jmp", "NeumimTo")
}
