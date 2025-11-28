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
            scmUrl.set("https://github.com/OWNER/REPO.git")
            scmConnection.set("scm:git:git@github.com:OWNER/REPO.git")
            developerName.set("yourcompany")
            developerEmail.set("admin@yourcompany.com")
            projectName.set("your cool project based on EDC")
            projectUrl.set("www.coolproject.com")
            description.set("your description")
            licenseUrl.set("https://opensource.org/licenses/MIT")
        }
    }
}