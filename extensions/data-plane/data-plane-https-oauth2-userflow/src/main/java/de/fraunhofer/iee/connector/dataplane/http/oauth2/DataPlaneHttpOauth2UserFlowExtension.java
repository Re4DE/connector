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

import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

public class DataPlaneHttpOauth2UserFlowExtension implements ServiceExtension {
    public static final String NAME = "Data Plane HTTP Oauth2 UserFlow";

    @Inject
    private HttpRequestParamsProvider paramsProvider;

    @Inject
    private Vault vault;

    @Inject
    private Oauth2Client oauth2Client;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var requestFactory = new Oauth2UserFlowCredentialFactory(this.vault);
        var oauth2UserFlowParamsDecorator = new Oauth2UserFlowHttpRequestParamsDecorator(requestFactory, oauth2Client);

        this.paramsProvider.registerSourceDecorator(oauth2UserFlowParamsDecorator);
    }
}
