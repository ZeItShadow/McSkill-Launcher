plugins {
    id("java")
}

group = "launcher"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.eclipsesource.minimal-json:minimal-json:0.9.4")
    implementation("org.fusesource.jansi:jansi:1.11")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Manifest-Version"] = "1.0"
        attributes["Main-Class"] = "launcher.Launcher"
    }
    from({
        configurations.runtimeClasspath.get()
                .filter { it.name.contains("minimal-json") }
                .map { zipTree(it) }
    })

}

tasks.test {
    useJUnitPlatform()
}