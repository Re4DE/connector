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

allprojects {
    apply(plugin = edcBuildId)

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        pom {
            scmUrl.set("https://github.com/Re4DE/connector.git")
            scmConnection.set("scm:git@github.com:Re4DE/connector.git")
            developerName.set("Fraunhofer IEE")
            developerEmail.set("sebastian.copei@iee.fraunhofer.de")
            projectName.set("Re4DE")
            projectUrl.set("https://github.com/Re4DE")
            description.set("Re4DE for everyone :D")
            licenseUrl.set("https://opensource.org/licenses/MIT")
        }
    }
}