plugins {
    id("java")
    id("application")
}

group = "eu.jameshamilton"
version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        version = 21
    }
    application {
        mainClass = "eu.jameshamilton.Main"
    }
}

dependencies {
    implementation("com.guardsquare:proguard-core:9.1.0")
}
