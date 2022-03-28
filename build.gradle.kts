import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}
group = "ru.senin.kotlin.wiki"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("com.apurebase:arkenv:3.3.3")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation(kotlin("reflect"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation(kotlin("test-junit5"))
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("ru.senin.kotlin.wiki.MainKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
