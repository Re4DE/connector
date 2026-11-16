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
    alias(libs.plugins.edc.build)
}

val edcBuildId = libs.plugins.edc.build.get().pluginId

val edcScmConnection: String by project
val edcScmUrl: String by project


allprojects {
    apply(plugin = edcBuildId)

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        pom {
            scmConnection.set(edcScmConnection)
            scmUrl.set(edcScmUrl)
        }
    }

    configure<CheckstyleExtension> {
        configFile = rootProject.file("resources/edc-checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))
    }
}