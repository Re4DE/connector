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

    implementation(libs.edc.vault.hashicorp)
    implementation(libs.edc.jsonld)
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.keys)
    implementation(libs.edc.token.lib)

    implementation(libs.bouncyCastle.bcpkixJdk18on)
    implementation(libs.bouncyCastle.bcprovJdk18on)
    implementation(libs.bouncyCastle.bctlsJdk18on)
}