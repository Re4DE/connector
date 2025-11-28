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
    runtimeOnly(libs.edc.bom.controlplane.base)
    runtimeOnly(libs.edc.bom.controlplane.sql)
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.policy.monitor)
    runtimeOnly(libs.bundles.fc.oauth2)

    // Dependencies from oauth2, now local, as removed from upstream edc
    runtimeOnly(libs.edc.spi.jwt)
    runtimeOnly(project(":extensions:common:iam:oauth2:oauth2-service"))

    runtimeOnly(project(":extensions:control-plane:self-registration:participant-self-registration-oauth2"))
    runtimeOnly(project(":extensions:control-plane:policy-functions"))
    runtimeOnly(project(":extensions:control-plane:phonebook:target-node-resolver-oauth2"))
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")

    // Exclude target node directory sql, as this overrides our own implementation
    dependencies {
        exclude(dependency("org.eclipse.edc:target-node-directory-sql"))
    }

    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}
