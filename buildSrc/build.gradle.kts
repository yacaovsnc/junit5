plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("de.schauderhaft.degraph:degraph-check:0.1.4")
}

gradlePlugin {
    (plugins) {
        "degraph" {
            id = "org.junit.degraph"
            implementationClass = "org.junit.build.degraph.DegraphPlugin"
        }
    }
}
