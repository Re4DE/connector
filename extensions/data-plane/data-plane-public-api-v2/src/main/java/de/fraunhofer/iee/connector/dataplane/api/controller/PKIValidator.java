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

package de.fraunhofer.iee.connector.dataplane.api.controller;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

public class PKIValidator {
    private static final String PKI_VALIDATE_ADDRESS_FLAG = "pki:validate";
    private static final String PKI_HEADER_NAME = "pki:headerName";

    private static final String TRUSTED_ROOT_PKI = "SM-Beta-PKI";

    public static boolean isMaKo(DataAddress address) {
        var pkiFlag = address.getProperty(PKI_VALIDATE_ADDRESS_FLAG);
        if (pkiFlag == null) {
            return false;
        }

        return Boolean.parseBoolean(pkiFlag.toString());
    }

    public static Result<Boolean> checkHeader(Map<String, String> headers, DataAddress address) {
        var certHeaderName = (String) address.getProperty(PKI_HEADER_NAME);
        if (certHeaderName == null) {
            return Result.failure("Missing PKI header %s".formatted(PKI_HEADER_NAME));
        }

        var cert = headers.get(certHeaderName);
        if (cert == null) {
            return Result.failure("Could not find cert in header %s".formatted(certHeaderName));
        }

        // Extract raw cert data
        var certString = cert
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("\\R", "")
                .replace("-----END CERTIFICATE-----", "");
        var decoded64Cert = Base64.getDecoder().decode(certString);

        // Use bouncy castle provider to load brainpool certificate and analyze it
        try {
            var certFact = CertificateFactory.getInstance("X509", BouncyCastleProvider.PROVIDER_NAME);
            var chain = certFact.generateCertificates(new ByteArrayInputStream(decoded64Cert));
            if (chain.size() != 1) {
                return Result.failure("Invalid cert in header %s".formatted(certHeaderName));
            }

            var x509 = (X509Certificate) chain.toArray(new Certificate[0])[0];
            // Check the validity
            if (!checkValidity(x509)) {
                return Result.failure("Provided certificate is out of date.");
            }
            // Check that the certificate is from the Smart Meter PKI
            var issuerName = extractIssuerRootName(x509);
            if (issuerName.failed()) {
                return Result.failure("Could not extract issuer name from cert. Invalid Certificate.");
            }
            if (!issuerName.getContent().equals(TRUSTED_ROOT_PKI)) {
                return Result.failure("Untrusted issuer name, need to be: %s".formatted(TRUSTED_ROOT_PKI));
            }

            // TODO: At this point more checks can be done, as public key is valid of issuer and so on...

        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("Could not load certificate factory; internal server error");
        }

        return Result.success(true);
    }

    private static boolean checkValidity(X509Certificate cert) {
        try {
            cert.checkValidity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Result<String> extractIssuerRootName(X509Certificate cert) {
        var issuerDN = cert.getIssuerX500Principal().getName();
        if (issuerDN == null) {
            return Result.failure("Issuer DN in certificate is null.");
        }
        return Result.from(Arrays.stream(issuerDN.split(","))
                .filter(cn -> cn.startsWith("O="))
                .map(cn -> cn.split("=")[1])
                .findFirst());
    }
}
