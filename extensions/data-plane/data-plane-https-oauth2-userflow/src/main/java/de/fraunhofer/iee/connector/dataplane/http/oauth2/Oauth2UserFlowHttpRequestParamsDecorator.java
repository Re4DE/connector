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

package de.fraunhofer.iee.connector.dataplane.http.oauth2;

import de.fraunhofer.iee.iam.oauth2.spi.Oauth2UserFlowDataAddressValidator;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

public class Oauth2UserFlowHttpRequestParamsDecorator implements HttpParamsDecorator {

    private final Oauth2UserFlowCredentialFactory requestFactory;
    private final Oauth2Client client;
    private final Oauth2UserFlowDataAddressValidator validator = new Oauth2UserFlowDataAddressValidator();

    public Oauth2UserFlowHttpRequestParamsDecorator(Oauth2UserFlowCredentialFactory requestFactory, Oauth2Client client) {
        this.requestFactory = requestFactory;
        this.client = client;
    }

    @Override
    public HttpRequestParams.Builder decorate(DataFlowStartMessage dataFlowStartMessage, HttpDataAddress httpDataAddress, HttpRequestParams.Builder builder) {
        if (this.validator.test(httpDataAddress)) {
            return requestFactory.create(httpDataAddress)
                    .compose(client::requestToken)
                    .map(tokenRepresentation -> builder.header("Authorization", "Bearer " + tokenRepresentation.getToken()))
                    .orElseThrow(failure -> new EdcException("Cannot authenticate through OAuth2 UserFlow " + failure.getFailureDetail()));
        } else {
            return builder;
        }
    }
}
