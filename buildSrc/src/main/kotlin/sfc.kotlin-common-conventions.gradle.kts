plugins {
    id("org.jetbrains.kotlin.jvm")
    id("idea")
    id("java")
}

repositories {
    mavenCentral()
}

val kotlinVersion = "1.9.0"

val jvmTarget = "1.8"
val junitVersion = "5.6.0"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

kotlin{
    jvmToolchain(8)
}

tasks.compileJava {
    sourceCompatibility = jvmTarget
    targetCompatibility = jvmTarget
}

tasks.compileTestJava {
    sourceCompatibility = jvmTarget
    targetCompatibility = jvmTarget
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-source", jvmTarget, "-target",  jvmTarget))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = jvmTarget
        freeCompilerArgs = listOf("-opt-in=kotlin.time.ExperimentalTime", "-opt-in=kotlin.ExperimentalUnsignedTypes")
    }
}
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}