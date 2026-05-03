plugins {
    `maven-publish`
    id("hytale-mod") version "0.+"
}

group = "com.varyon"
version = "1.0.0"
val javaVersion = 25

repositories {
    mavenCentral()
    maven("https://maven.hytale-modding.info/releases") {
        name = "HytaleModdingReleases"
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.jspecify)
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("org.slf4j:slf4j-api:1.7.30")
}

hytale {
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(javaVersion) }
    withSourcesJar()
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "plugin_group" to findProperty("plugin_group"),
        "plugin_maven_group" to project.group,
        "plugin_name" to (findProperty("plugin_name") ?: project.name),
        "plugin_version" to project.version,
        "server_version" to findProperty("server_version"),
        "plugin_description" to findProperty("plugin_description"),
        "plugin_website" to findProperty("plugin_website"),
        "plugin_main_entrypoint" to findProperty("plugin_main_entrypoint"),
        "plugin_author" to findProperty("plugin_author"),
    )
    filesMatching("manifest.json") { expand(replaceProperties) }
    inputs.properties(replaceProperties)

    from("src/main/resources/Pages") {
        into("Common/UI/Custom/Pages")
    }
}

val fatJar = tasks.register<Jar>("fatJar") {
    archiveClassifier.set("")
    archiveBaseName.set("Varyon-Playtime")
    archiveVersion.set(version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    val implementationJars = configurations.runtimeClasspath.get()
        .filter {
            it.name.contains("hikari") ||
                it.name.contains("mysql-connector-j") ||
                it.name.contains("gson") ||
                it.name.contains("slf4j") ||
                it.name.contains("sqlite-jdbc")
        }
    from({ implementationJars.map { zipTree(it) } })

    exclude("META-INF/versions/**")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
    exclude("module-info.class")
    exclude("**/package-info.class")
    exclude("LICENSE")
    exclude("NOTICE")

    manifest {
        attributes["Specification-Title"] = rootProject.name
        attributes["Specification-Version"] = version
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = version.toString()
    }
}

tasks.named("build") { dependsOn(fatJar) }

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    repositories {}
    publications { create<MavenPublication>("maven") { from(components["java"]) } }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

val syncAssets = tasks.register<Copy>("syncAssets") {
    group = "hytale"
    description = "Sync assets from Build back to Source after server stops."
    from(layout.buildDirectory.dir("resources/main"))
    into("src/main/resources")
    exclude("manifest.json")
    exclude("Common/**")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

afterEvaluate {
    val targetTask = tasks.findByName("runServer") ?: tasks.findByName("server")
    if (targetTask != null) targetTask.finalizedBy(syncAssets)
}
