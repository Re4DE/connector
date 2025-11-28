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
    implementation(libs.edc.dcp.core)
    implementation(libs.edc.jws2020.lib)
    implementation(libs.edc.transform.lib)
    implementation(libs.edc.spi.identity.trust)
    implementation(libs.edc.spi.catalog)
    implementation(libs.edc.spi.identity.did)
}