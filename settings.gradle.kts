pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "noctorium-android-tv"

/**
 * Noctorium's core, from Noctorium-Base, which the TV screens are built on: found as Noctorium-Mobile finds it.
 *
 * 1. NOCTORIUM_BASE, when set.
 * 2. ../Noctorium-Base, a checkout beside this one, for working on both at once.
 * 3. base/, the submodule this repository carries, for a clean clone and CI.
 */
val baseCheckout: File = listOfNotNull(
    System.getenv("NOCTORIUM_BASE")?.takeIf { it.isNotBlank() }?.let(::file),
    file("../Noctorium-Base"),
    file("base"),
).firstOrNull { File(it, "core/build.gradle.kts").isFile }
    ?: error(
        "Noctorium-Base was not found. Either check it out beside this repository as ../Noctorium-Base, " +
            "run `git submodule update --init` to fetch the base/ submodule, or set NOCTORIUM_BASE to a checkout.",
    )

include(":core")
project(":core").projectDir = File(baseCheckout, "core")

/** The library itself: what Noctorium-Mobile includes as :tv. */
include(":tv")
