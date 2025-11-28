/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(libs.edc.spi.http)
    api(libs.edc.spi.oauth2)
    implementation(libs.edc.spi.protocol)
    implementation(libs.edc.spi.keys)
    implementation(libs.edc.spi.jwt.signer)
    implementation(libs.edc.oauth2.client)
    implementation(libs.edc.token.lib)

    implementation(libs.nimbus.jwt)

    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(libs.edc.http.lib))

    testImplementation(libs.mockserver.netty)
    testImplementation(libs.mockserver.client)
}


