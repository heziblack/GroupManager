plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.12.0"
}

group = "org.hezistudio"
version = "0.0.5"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies{
    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("org.slf4j:slf4j-reload4j:2.0.3")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    implementation("org.jetbrains.exposed","exposed-core","0.38.2")
    implementation("org.jetbrains.exposed","exposed-dao","0.38.2")
    implementation("org.jetbrains.exposed","exposed-jdbc","0.38.2")
    implementation ("com.beust:klaxon:5.5")
    implementation ("com.google.code.gson:gson:2.9.1")
}