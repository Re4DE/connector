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
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    runtimeOnly(libs.edc.bom.controlplane.base)
    runtimeOnly(libs.edc.bom.controlplane.sql)
    runtimeOnly(libs.edc.oauth2.client)
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.policy.monitor)
    runtimeOnly(libs.bundles.fc.oauth2)

    runtimeOnly(project(":extensions:common:iam:x509"))
    runtimeOnly(project(":extensions:control-plane:self-registration"))
    runtimeOnly(project(":extensions:control-plane:policy-functions"))
    runtimeOnly(project(":extensions:control-plane:target-node-resolver"))

    shadow(libs.bouncyCastle.bcpkixJdk18on)
    shadow(libs.bouncyCastle.bcprovJdk18on)
    shadow(libs.bouncyCastle.bctlsJdk18on)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.shadowJar {
    exclude("**/pom.properties", "**/pom.xml")

    // Exclude target node directory sql, as this overrides our own implementation
    dependencies {
        exclude(dependency("org.eclipse.edc:target-node-directory-sql"))
    }

    // Exclude bouncy castle, as it should be loaded as separated library to not lose the signed jar
    dependencies {
        exclude(dependency("org.bouncycastle:bcpkix-jdk18on:1.84"))
        exclude(dependency("org.bouncycastle:bcprov-jdk18on:1.84"))
        exclude(dependency("org.bouncycastle:bctls-jdk18on:1.84"))
        exclude(dependency("org.bouncycastle:bcutil-jdk18on:1.84"))
    }

    mergeServiceFiles()
    archiveFileName.set("controlplane-x509.jar")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.register<Copy>("copyShadowLibs") {
    from(configurations.shadow)
    into(layout.buildDirectory.dir("libs"))
}

tasks.named("shadowJar") {
    dependsOn(tasks.named("copyShadowLibs"))
}

tasks.named("startScripts") {
    dependsOn(tasks.named("copyShadowLibs"))
}

tasks.named("distTar") {
    dependsOn(tasks.named("copyShadowLibs"))
}

tasks.named("distZip") {
    dependsOn(tasks.named("copyShadowLibs"))
}

edcBuild {
    publish.set(false)
}
