plugins {
    id("java")
}

group = "eu.jameshamilton"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        version = 21
    }
}

dependencies {
    implementation("com.guardsquare:proguard-core:9.1.0")
}
