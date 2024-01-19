group = "com.amazonaws.sfc"

version = "1.0.0"

val sfcCoreVersion = "1.0.0"
val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"
val opcuaMiloVersion = "0.5.1"



plugins {

    java

    id("sfc.kotlin-library-conventions")
}

dependencies {

    implementation(project(":core:sfc-core"))
    implementation(project(":adapters:opcua"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation(project(":core:sfc-core"))
    implementation(project(":core:sfc-ipc"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.eclipse.milo:sdk-client:$opcuaMiloVersion")



}

