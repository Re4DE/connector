/*
 *  Copyright (c) 2025 Fraunhofer Institute for Energy Economics and Energy System Technology (IEE)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer IEE - initial API and implementation
 *
 */

plugins {
    id("application")
    alias(libs.plugins.shadow)
    alias(libs.plugins.docker)
}

dependencies {
    runtimeOnly(libs.edc.bom.dataplane)
    runtimeOnly(libs.edc.bom.dataplane.sql)
    runtimeOnly(libs.edc.vault.hashicorp)

    runtimeOnly(project(":extensions:common:http:mtls"))
    runtimeOnly(project(":extensions:data-plane:data-plane-https-oauth2-userflow"))
    runtimeOnly(project(":extensions:data-plane:data-plane-public-api-v2"))

    shadow(libs.bouncyCastle.bcpkixJdk18on)
    shadow(libs.bouncyCastle.bcprovJdk18on)
    shadow(libs.bouncyCastle.bctlsJdk18on)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")

    // Exclude bouncy castle, as it should be loaded as seperated library to not lose the signed jar
    dependencies {
        exclude(dependency("org.bouncycastle:bcpkix-jdk18on:1.84"))
        exclude(dependency("org.bouncycastle:bcprov-jdk18on:1.84"))
        exclude(dependency("org.bouncycastle:bctls-jdk18on:1.84"))
        exclude(dependency("org.bouncycastle:bcutil-jdk18on:1.84"))
    }
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}
