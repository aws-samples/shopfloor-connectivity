group = "com.amazonaws.sfc"

version = "1.0.0"

val sfcCoreVersion = "1.0.0"
val kotlinCoroutinesVersion = "1.6.2"
val kotlinVersion = "1.9.0"
val jvmTarget = "1.8"



plugins {

    java
    
    id("sfc.kotlin-library-conventions")
}

dependencies {

    implementation(project(":core:sfc-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

}

















