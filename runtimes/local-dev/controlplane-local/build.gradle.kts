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
}

dependencies {
    runtimeOnly(libs.edc.bom.controlplane.sql)
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.policy.monitor)

    runtimeOnly(project(":extensions:control-plane:policy-functions"))

    // Oauth2 Specific
    //runtimeOnly(libs.edc.bom.controlplane.base)
    //runtimeOnly(libs.bundles.fc.oauth2)
    //runtimeOnly(libs.edc.spi.jwt)

    //runtimeOnly(project(":extensions:control-plane:self-registration:participant-self-registration-oauth2"))
    //runtimeOnly(project(":extensions:common:iam:oauth2:oauth2-service"))

    // X509 Specific
    // runtimeOnly(project(":extensions:common:iam:x509"))

    // SSI Specific
    runtimeOnly(libs.edc.bom.controlplane.dcp)

    // The federated catalog can not be tested locally with real connectors, as the real connectors can not resolve local DIDs
    //runtimeOnly(libs.bundles.fc.ssi)

    runtimeOnly(project(":extensions:control-plane:self-registration:participant-self-registration-ssi"))
    runtimeOnly(project(":extensions:control-plane:scope-functions"))
    runtimeOnly(project(":extensions:control-plane:target-node-resolver"))
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")

    // Exclude target node directory sql, as this overrides our own implementation
    dependencies {
        exclude(dependency("org.eclipse.edc:target-node-directory-sql"))
    }

    // Exclude bouncy castle, as it should be loaded as seperated library to not lose the signed jar
    dependencies {
        // exclude(dependency("org.bouncycastle:bcpkix-jdk18on:1.81"))
        // exclude(dependency("org.bouncycastle:bcprov-jdk18on:1.81"))
        exclude(dependency("org.bouncycastle:bctls-jdk18on:1.81"))
        exclude(dependency("org.bouncycastle:bcutil-jdk18on:1.81"))
    }

    mergeServiceFiles()
    archiveFileName.set("controlplane.jar")
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

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}