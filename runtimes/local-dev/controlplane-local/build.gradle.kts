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

/* Uncomment the desired flavor to test it */
dependencies {
    /* DCP Controlplane (default) */
    runtimeOnly(libs.edc.bom.controlplane.dcp)
    runtimeOnly(libs.edc.bom.federatedcatalog.dcp)

    /* OAuth2 Controlplane */
    //runtimeOnly(libs.edc.bom.controlplane.base)
    //runtimeOnly(libs.edc.bom.federatedcatalog.base)
    //runtimeOnly(project(":extensions:common:iam:oauth2:oauth2-service"))

    /* X.509 Controlplane */
    //runtimeOnly(libs.edc.bom.controlplane.base)
    //runtimeOnly(libs.edc.bom.federatedcatalog.base)
    //runtimeOnly(libs.edc.oauth2.client)
    //runtimeOnly(project(":extensions:common:iam:x509"))

    /* Common dependency, needed for all flavors */
    runtimeOnly(libs.edc.bom.controlplane.feature.sql)
    runtimeOnly(libs.edc.bom.federatedcatalog.feature.sql)
    runtimeOnly(libs.edc.vault.hashicorp)
    runtimeOnly(project(":extensions:control-plane:self-registration"))
    runtimeOnly(project(":extensions:control-plane:policy-functions"))
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

    // Exclude bouncy castle, as it should be loaded as seperated library to not lose the signed jar
    // Only needed for X509
    /*dependencies {
        exclude(dependency("org.bouncycastle:bcpkix-jdk18on"))
        exclude(dependency("org.bouncycastle:bcprov-jdk18on"))
        exclude(dependency("org.bouncycastle:bctls-jdk18on"))
        exclude(dependency("org.bouncycastle:bcutil-jdk18on"))
    }*/

    mergeServiceFiles()
    archiveFileName.set("controlplane.jar")
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