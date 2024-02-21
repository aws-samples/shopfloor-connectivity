import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "com.amazonaws.sfc"

version = "1.0.0"

val sfcCoreVersion = "1.0.0"
val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"
val pahoVersion = "1.2.5"



plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("sfc.kotlin-library-conventions")
}

tasks.named("build"){
    finalizedBy("shadowJar")
}


tasks.withType<ShadowJar> {
    archiveFileName.set("${project.name}-${project.version}.jar")
}

dependencies {

    implementation(project(":core:sfc-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:$pahoVersion")

}

