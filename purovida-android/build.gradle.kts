// Plugin versions come from gradle/libs.versions.toml; each module applies
// what it needs. Keeping Android plugins out of this root script means pure
// JVM builds (`gradle :core:test --configure-on-demand`) never resolve AGP,
// which allows the engine tests to run in environments without Google's
// Maven repository or an Android SDK.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
