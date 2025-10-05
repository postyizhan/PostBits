plugins {
    kotlin("jvm") version "1.8.22"
}

group = "com.github.postyizhan"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.oraxen.com/releases")

    // ProtocolLib 仓库
    maven("https://repo.dmulloy2.net/repository/public/")

    // CraftEngine 仓库（使用中国镜像）
    maven("https://repo-momi.gtemc.cn/releases/")
    maven("https://repo.gtemc.net/releases/") // 备用镜像

    // Bstats 仓库
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.13-R0.1-SNAPSHOT")

    // ProtocolLib
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")

    // CraftEngine
    compileOnly("net.momirealms:craft-engine-core:0.0.22")
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.22")

    // ItemsAdder
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.1")

    // Oraxen
    compileOnly("io.th0rgal:oraxen:1.189.0")

    // Bstats
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    main {
        java {
            srcDir("aneToastAPI")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
    
    exclude("META-INF/DEPENDENCIES")
    exclude("META-INF/LICENSE")
    exclude("META-INF/LICENSE.txt")
    exclude("META-INF/license.txt")
    exclude("META-INF/NOTICE")
    exclude("META-INF/NOTICE.txt")
    exclude("META-INF/notice.txt")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
