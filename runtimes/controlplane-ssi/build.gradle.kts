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
    runtimeOnly(libs.edc.bom.controlplane.dcp)
    runtimeOnly(libs.edc.bom.controlplane.sql)
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.policy.monitor)
    runtimeOnly(libs.bundles.fc.ssi)

    runtimeOnly(project(":extensions:control-plane:self-registration:participant-self-registration-ssi"))
    runtimeOnly(project(":extensions:control-plane:policy-functions"))
    runtimeOnly(project(":extensions:control-plane:scope-functions"))
    runtimeOnly(project(":extensions:control-plane:target-node-resolver"))
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
