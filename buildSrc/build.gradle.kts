plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

apply(from = "../repositories.gradle.kts")

dependencies {
    implementation("com.android.tools.build:gradle:9.5.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.21")
}
