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

rootProject.name = "connector"

// this is needed to have access to snapshot builds of plugins
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}

// add dependencies
include(":runtimes:controlplane-oauth2")
include(":runtimes:controlplane-ssi")
include(":runtimes:controlplane-x509")
include(":runtimes:dataplane")
include(":runtimes:local-dev:controlplane-local")
include(":runtimes:local-dev:dataplane-local")
include(":runtimes:local-dev:docker-env")

include(":extensions:common:iam:x509")
include(":extensions:common:iam:oauth2:oauth2-core")
include(":extensions:common:iam:oauth2:oauth2-service")

include(":extensions:control-plane:self-registration:participant-self-registration-oauth2")
include(":extensions:control-plane:self-registration:participant-self-registration-ssi")
include(":extensions:control-plane:policy-functions")
include(":extensions:control-plane:phonebook:target-node-resolver-oauth2")
include(":extensions:control-plane:phonebook:target-node-resolver-ssi")
include(":extensions:control-plane:scope-functions")

include(":extensions:data-plane:data-plane-https-oauth2-userflow")
include(":extensions:data-plane:data-plane-public-api-v2")

include(":spi:common:iee-oauth2-spi")
