repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven(url = "https://jitpack.io")
    maven {
        name = "GitHubPackagesMiuix"
        url = uri("https://maven.pkg.github.com/compose-miuix-ui/miuix")
        val gprUser = System.getenv("GITHUB_ACTOR")
        val gprKey = System.getenv("GITHUB_TOKEN")
        if (!gprUser.isNullOrBlank() && !gprKey.isNullOrBlank()) {
            credentials {
                username = gprUser
                password = gprKey
            }
        }
    }
}
