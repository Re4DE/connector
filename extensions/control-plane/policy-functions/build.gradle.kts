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
}

dependencies {
    api(libs.edc.spi.http)
    api(libs.edc.spi.token)
    api(libs.edc.spi.oauth2)
    api(libs.edc.spi.json.ld)
    api(libs.edc.spi.policy.monitor)

    implementation(libs.edc.spi.core)
    implementation(libs.edc.controlplane.core)
    implementation(libs.edc.spi.identity.trust)
}