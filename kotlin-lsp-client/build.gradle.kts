plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    
    // LSP4J for Language Server Protocol support
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.21.1")
    
    // Gson for JSON parsing and formatting
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    enabled = false
}

// Reconfigure the build task to not depend on the check task
tasks.named("build") {
    setDependsOn(listOf("assemble"))
}
kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.example.MainKt")
}