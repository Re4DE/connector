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

package de.fraunhofer.iee.iam.oauth2.spi.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Oauth2UserFlowCredentialsRequest extends Oauth2CredentialsRequest {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    @NotNull
    public String getUsername() {
        return (String) params.get(USERNAME);
    }

    @NotNull
    public String getPassword() {
        return (String) params.get(PASSWORD);
    }

    public static class Builder<B extends Oauth2UserFlowCredentialsRequest.Builder<B>> extends  Oauth2CredentialsRequest.Builder<Oauth2UserFlowCredentialsRequest, Oauth2UserFlowCredentialsRequest.Builder<B>> {

        protected Builder(Oauth2UserFlowCredentialsRequest request) {
            super(request);
        }

        @JsonCreator
        public static <B extends Oauth2UserFlowCredentialsRequest.Builder<B>> Oauth2UserFlowCredentialsRequest.Builder<B> newInstance() {
            return new Oauth2UserFlowCredentialsRequest.Builder<>(new Oauth2UserFlowCredentialsRequest());
        }

        public B username(String username) {
            param(USERNAME, username);
            return self();
        }

        public B password(String password) {
            param(PASSWORD, password);
            return self();
        }

        @SuppressWarnings("unchecked")
        @Override
        public B self() {
            return (B) this;
        }

        @Override
        public Oauth2UserFlowCredentialsRequest build() {
            Objects.requireNonNull(request.params.get(USERNAME), USERNAME);
            Objects.requireNonNull(request.params.get(PASSWORD), PASSWORD);
            return super.build();
        }
    }
}
