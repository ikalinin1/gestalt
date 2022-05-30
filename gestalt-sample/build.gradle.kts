plugins {
    id("gestalt.java-common-conventions")
    id("gestalt.java-test-conventions")
    id("gestalt.kotlin-common-conventions")
    id("gestalt.kotlin-test-conventions")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    val gestaltVersion = "0.12.0"
    testImplementation("com.github.gestalt-config:gestalt-core:$gestaltVersion")
    testImplementation("com.github.gestalt-config:gestalt-hocon:$gestaltVersion")
    testImplementation("com.github.gestalt-config:gestalt-kotlin:$gestaltVersion")
    testImplementation("com.github.gestalt-config:gestalt-json:$gestaltVersion")
    testImplementation("com.github.gestalt-config:gestalt-yaml:$gestaltVersion")
    testImplementation("com.github.gestalt-config:gestalt-s3:$gestaltVersion")

    testImplementation("com.github.gestalt-config:gestalt-kodein-di:$gestaltVersion")
    implementation(org.github.gestalt.config.Application.Kotlin.kodeinDI)

    testImplementation("com.github.gestalt-config:gestalt-koin-di:$gestaltVersion")
    implementation(org.github.gestalt.config.Application.Kotlin.koinDI)

    testImplementation(org.github.gestalt.config.Test.awsMock)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "org.github.gestalt.config.integration")
    }
}
