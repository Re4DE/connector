plugins {
    `java-library`
}

dependencies {
    api(libs.edc.spi.http)
    implementation(libs.edc.spi.core)
    implementation(libs.edc.jetty.core)
    implementation(libs.jetty.server)
    implementation(libs.edc.vault.hashicorp)
    implementation(libs.edc.jsonld)

    implementation(libs.bouncyCastle.bcpkixJdk18on)
    implementation(libs.bouncyCastle.bcprovJdk18on)
    implementation(libs.bouncyCastle.bctlsJdk18on)
}