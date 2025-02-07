
group = "com.amazonaws.sfc"
version = "1.0.0"

val sfcRelease = rootProject.extra.get("sfc_release")!!
val sfcCoreVersion = sfcRelease
val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"

plugins {
    java
    id("sfc.kotlin-library-conventions")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core:sfc-core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("net.java.dev.jna:jna:5.16.0")
    implementation("com.nativelibs4java:jnaerator-runtime:0.12")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = group as String
            artifactId = "modbus"
            version = version
        }
    }

}

tasks.build {
    finalizedBy(tasks.publishToMavenLocal)
}


//
//tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//    kotlinOptions {
//        jvmTarget = "18"
//        freeCompilerArgs += listOf(
////            "-Xuse-ir",
//            "-Xskip-prerelease-check",
//            "-Xno-param-assertions",
//            "-Xno-call-assertions"
//        )
//    }
//}
//tasks.test {
//    useJUnitPlatform()
//}
//kotlin {
//
//    jvmToolchain(18)
//
//}
//java{
//    sourceCompatibility = JavaVersion.VERSION_18
//    targetCompatibility = JavaVersion.VERSION_18
//}