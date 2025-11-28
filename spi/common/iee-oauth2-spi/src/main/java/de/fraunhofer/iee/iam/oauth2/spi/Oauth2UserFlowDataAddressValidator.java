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

package de.fraunhofer.iee.iam.oauth2.spi;

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.function.Predicate;

import static de.fraunhofer.iee.iam.oauth2.spi.Oauth2UserFlowDataAddressSchema.*;

/**
 * Validates {@link org.eclipse.edc.spi.types.domain.DataAddress}, returns true if the Address has the fields for the OAuth2 User credential flow
 */
public class Oauth2UserFlowDataAddressValidator implements Predicate<DataAddress> {

    private final Predicate<DataAddress> hasTokenUrl = dataAddress -> dataAddress.hasProperty(TOKEN_URL);
    private final Predicate<DataAddress> hasUsername = dataAddress -> dataAddress.hasProperty(USERNAME);
    private final Predicate<DataAddress> hasPasswordSecretName = dataAddress -> dataAddress.hasProperty(PASSWORD_SECRET_NAME);

    @Override
    public boolean test(DataAddress dataAddress) {
        return hasTokenUrl.and(hasUsername).and(hasPasswordSecretName).test(dataAddress);
    }
}
