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
    runtimeOnly(libs.edc.bom.controlplane.dcp)
    runtimeOnly(libs.edc.bom.controlplane.sql)
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(libs.edc.policy.monitor)
    runtimeOnly(libs.bundles.fc.dcp)

    runtimeOnly(project(":extensions:control-plane:self-registration"))
    runtimeOnly(project(":extensions:control-plane:policy-functions"))
    runtimeOnly(project(":extensions:control-plane:scope-functions"))
    runtimeOnly(project(":extensions:control-plane:target-node-resolver"))
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

    mergeServiceFiles()
    archiveFileName.set("controlplane-dcp.jar")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

edcBuild {
    publish.set(false)
}
