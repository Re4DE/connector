/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       Mercedes-Benz Tech Innovation GmbH - publish public api context into dedicated swagger hub page
 *
 */


plugins {
    `java-library`
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(libs.edc.spi.http)
    api(libs.edc.spi.web)
    api(libs.edc.spi.data.plane)
    implementation(libs.edc.util.lib)

    implementation(libs.edc.data.plane.util)
    implementation(libs.jakarta.rsApi)

    implementation(libs.bouncyCastle.bcpkixJdk18on)
    implementation(libs.bouncyCastle.bcprovJdk18on)

    testImplementation(libs.edc.http)
    testImplementation(libs.edc.junit)
    testImplementation(libs.jersey.multipart)
    testImplementation(libs.restAssured)
    testImplementation(testFixtures(libs.edc.jersey.core))
}
edcBuild {
    swagger {
        apiGroup.set("public-api")
    }
}


