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
 *       sovity GmbH - added issuedAt leeway
 *
 */

package de.fraunhofer.iee.connector.common.iam.oauth2;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static java.util.Map.entry;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(DependencyInjectionExtension.class)
class Oauth2ServiceExtensionTest {

    private final SingleParticipantContextSupplier participantContextSupplier = mock();
    private final Vault vault = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(SingleParticipantContextSupplier.class, participantContextSupplier);
        context.registerService(Vault.class, vault);
    }

    @Test
    void verifyExtensionWithCertificateAlias(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var config = spy(ConfigFactory.fromMap(Map.ofEntries(
                entry("edc.oauth.client.id", "id"),
                entry("edc.oauth.token.url", "url"),
                entry("edc.oauth.certificate.alias", "alias"),
                entry("edc.oauth.private.key.alias", "p_alias"))));
        mockOAuthSpecifica("alias");

        when(context.getConfig()).thenReturn(config);
        objectFactory.constructInstance(Oauth2ServiceExtension.class).initialize(context);

        verify(config, times(1)).getString("edc.oauth.certificate.alias");
        verify(config, never()).getString("edc.oauth.public.key.alias");
    }

    @Test
    void leewayWarningLoggedWhenLeewayUnconfigured(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var config = ConfigFactory.fromMap(Map.ofEntries(
                entry("edc.oauth.client.id", "id"),
                entry("edc.oauth.token.url", "url"),
                entry("edc.oauth.certificate.alias", "alias"),
                entry("edc.oauth.private.key.alias", "p_alias")));
        mockOAuthSpecifica("alias");

        var monitor = mock(Monitor.class);
        when(context.getMonitor()).thenReturn(monitor);
        when(context.getConfig()).thenReturn(config);
        objectFactory.constructInstance(Oauth2ServiceExtension.class).initialize(context);

        var message = "No value was configured for 'edc.oauth.validation.issued.at.leeway'.";
        verify(monitor, times(1)).info(contains(message));
    }

    @Test
    void leewayNoWarningWhenLeewayConfigured(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var config = ConfigFactory.fromMap(Map.ofEntries(
                entry("edc.oauth.client.id", "id"),
                entry("edc.oauth.token.url", "url"),
                entry("edc.oauth.certificate.alias", "alias"),
                entry("edc.oauth.private.key.alias", "p_alias"),
                entry("edc.oauth.validation.issued.at.leeway", "5")));
        mockOAuthSpecifica("alias");

        var monitor = mock(Monitor.class);
        when(context.getMonitor()).thenReturn(monitor);
        when(context.getConfig()).thenReturn(config);
        objectFactory.constructInstance(Oauth2ServiceExtension.class).initialize(context);

        var message = "No value was configured for 'edc.oauth.validation.issued.at.leeway'.";
        verify(monitor, never()).info(contains(message));
    }

    private void mockOAuthSpecifica(String alias) {
        when(vault.resolveSecret(alias)).thenReturn("-----BEGIN CERTIFICATE-----" +
                "MIIBPjCB6aADAgECAhRNrRtgnamwALeFs5XxvVpCAvCYljANBgkqhkiG9w0BAQsF\n" +
                "ADAMMQowCAYDVQQDDAF4MCAXDTI2MTExMzEzNDUxM1oYDzMwMjYxMTE0MTM0NTEz\n" +
                "WjAMMQowCAYDVQQDDAF4MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAMiJcudS40ZN\n" +
                "zfMLHkttC+kUh6rNtPdADjgaqtcg/LSE79BAyQaX/e3ipyLh9x7Mfuy8fAnIBeRY\n" +
                "dVO91hQSMPECAwEAAaMhMB8wHQYDVR0OBBYEFCszmhrv394axzbC+1C7mhNtz5o2\n" +
                "MA0GCSqGSIb3DQEBCwUAA0EAUP/4Npe1d39WVTeYRb6LDhbW42/PPDUczbIp4BF0\n" +
                "VWKtWlvdynb2G5MV9Bu47H7RNqwJNIcf4dw3RMRu8BFWIQ==" +
                "-----END CERTIFICATE-----");
        var participantContext = ParticipantContext.Builder.newInstance()
                .identity("id")
                .participantContextId("id")
                .build();
        when(participantContextSupplier.get()).thenReturn(ServiceResult.success(participantContext));
    }
}
