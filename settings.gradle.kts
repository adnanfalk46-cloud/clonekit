pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "apk-cloner"
include(":app")
include(":core:apkpatch")
include(":core:signer")
include(":core:installer")
include(":core:clonestore")
include(":core:cloner")
