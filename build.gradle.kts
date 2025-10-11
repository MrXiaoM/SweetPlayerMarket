plugins {
    java
    `maven-publish`
    id ("com.gradleup.shadow") version "8.3.0"
    id ("com.github.gmazzo.buildconfig") version "5.6.7"
}

group = "top.mrxiaom.sweet.playermarket"
version = "1.0.0"
val targetJavaVersion = 8
val pluginBaseVersion = "1.6.6"
val shadowGroup = "top.mrxiaom.sweet.playermarket.libs"

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.helpch.at/releases/")
    maven("https://jitpack.io")
    maven("https://repo.rosewooddev.io/repository/public/")
}

val libraries = arrayListOf<String>()
fun DependencyHandlerScope.library(dependencyNotation: String) {
    compileOnly(dependencyNotation)
    libraries.add(dependencyNotation)
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    // compileOnly("org.spigotmc:spigot:1.20") // NMS

    compileOnly("com.github.MascusJeoraly:LanguageUtils:1.9")
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.black_ixx:playerpoints:3.2.7")
    compileOnly(files("libs/MPoints-1.2.2.jar"))

    library("net.kyori:adventure-api:4.22.0")
    library("net.kyori:adventure-platform-bukkit:4.4.0")
    library("net.kyori:adventure-text-minimessage:4.22.0")
    library("net.kyori:adventure-text-serializer-plain:4.22.0")
    library("com.zaxxer:HikariCP:4.0.3")
    library("org.jetbrains:annotations:24.0.0")

    implementation("de.tr7zw:item-nbt-api:2.15.3-SNAPSHOT")
    implementation("com.github.technicallycoded:FoliaLib:0.4.4") { isTransitive = false }
    implementation("top.mrxiaom.pluginbase:library:$pluginBaseVersion")
    implementation("top.mrxiaom.pluginbase:paper:$pluginBaseVersion")
    implementation("top.mrxiaom:LibrariesResolver:$pluginBaseVersion")
}
buildConfig {
    className("BuildConstants")
    packageName("top.mrxiaom.sweet.playermarket")

    val librariesVararg = libraries.joinToString(", ") { "\"$it\"" }

    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("java.time.Instant", "BUILD_TIME", "java.time.Instant.ofEpochSecond(${System.currentTimeMillis() / 1000L}L)")
    buildConfigField("String[]", "LIBRARIES", "new String[] { $librariesVararg }")
}
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withJavadocJar()
    withSourcesJar()
}
tasks {
    shadowJar {
        mapOf(
            "top.mrxiaom.pluginbase" to "base",
            "de.tr7zw.changeme.nbtapi" to "nbtapi",
            "com.tcoded.folialib" to "folialib",
        ).forEach { (original, target) ->
            relocate(original, "$shadowGroup.$target")
        }
        listOf(
            "top/mrxiaom/pluginbase/temporary/*",
        ).forEach(this::exclude)
    }
    val copyTask = create<Copy>("copyBuildArtifact") {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs)
        rename { "${project.name}-$version.jar" }
        into(rootProject.file("out"))
    }
    build {
        dependsOn(copyTask)
    }
    javadoc {
        (options as StandardJavadocDocletOptions).apply {
            links("https://hub.spigotmc.org/javadocs/spigot/")

            locale("zh_CN")
            encoding("UTF-8")
            docEncoding("UTF-8")
            addBooleanOption("keywords", true)
            addBooleanOption("Xdoclint:none", true)
        }
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().resources.srcDirs) {
            expand(mapOf("version" to version))
            include("plugin.yml")
        }
    }
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components.getByName("java"))
            groupId = project.group.toString()
            artifactId = rootProject.name
            version = project.version.toString()
        }
    }
}
