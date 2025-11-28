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

package de.fraunhofer.iee.connector.controlplane.policyfunctions.functions;

import de.fraunhofer.iee.connector.controlplane.policyfunctions.oauth2.AccessTokenRetriever;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Map;

import static java.lang.String.format;

public class PermissionAdministratorFunction<C extends PolicyContext> implements AtomicConstraintRuleFunction<Permission, C> {

    private static final String STATUS_KEY = "status";
    private static final String TERMINATED_BY_ID_KEY = "terminatedById";
    private static final String TERMINATED_ON_KEY = "terminatedOn";

    private final Monitor monitor;
    private final EdcHttpClient httpClient;
    private final TypeManager typeManager;
    private final AccessTokenRetriever tokenRetriever;
    private final String pmUrl;

    public PermissionAdministratorFunction(Monitor monitor, EdcHttpClient httpClient, TypeManager typeManager, AccessTokenRetriever tokenRetriever, String pmUrl) {
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.typeManager = typeManager;
        this.tokenRetriever = tokenRetriever;
        this.pmUrl = pmUrl;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, C context) {
        // Check operator, need to be EQ
        if (operator != Operator.EQ) {
            context.reportProblem("Operator expected to be eq, but got " + operator.getOdrlRepresentation());
            return false;
        }

        // Check the provided value, need to be a permission request id
        // TODO: Only for demonstration purpose!
        //String permissionRequestId = this.extractPermissionIdFromContext(context);
        String permissionRequestId = rightValue.toString();
        if (permissionRequestId == null) {
            return false;
        }

        try {
            var token = this.tokenRetriever.obtainToken()
                    .orElseThrow(failure -> new EdcException(format("Could not reach permission administrator api: %s", failure.getFailureDetail())));

            var request = new Request.Builder()
                    .url(this.pmUrl + "/permissionrequest/" + permissionRequestId)
                    .header("Authorization", "Bearer " + token.getToken())
                    .method("GET", null)
                    .build();
            var response = this.httpClient.execute(request);
            if (!this.responseIsValid(response)) {
                return false;
            }

            var body = this.typeManager.readValue(response.body().bytes(), Map.class);

            // Check the permission request status
            var status = body.get(STATUS_KEY).toString();
            if (!status.equals("approved")) {
                context.reportProblem("Permission request is not approved, but it " + status);
                return false;
            }

            // Check the associated Permission Records, TODO: Static list for debug, need to be extracted from request as it is implemented
            var recordIds = new String[] { "f010a8bd-ad09-4c92-97db-06e9da39cfc2" };
            for (var recordId : recordIds) {
                try {
                    var req = new Request.Builder()
                            .url(this.pmUrl + "/permissionrecord/" + recordId)
                            .header("Authorization", "Bearer " + token.getToken())
                            .method("GET", null)
                            .build();
                    var res = this.httpClient.execute(req);
                    if (!this.responseIsValid(res)) {
                        continue;
                    }
                    var b = this.typeManager.readValue(res.body().bytes(), Map.class);
                    // Check that the permission record is not terminated
                    if (b.get(TERMINATED_BY_ID_KEY) != null ||
                            b.get(TERMINATED_ON_KEY) != null) {
                        context.reportProblem("Permission record was terminated.");
                        return false;
                    }

                } catch (Exception e) {
                    throw new EdcException(e);
                }
            }

            // All checks are done, permission is granted
            return true;

        } catch (Exception e) {
            throw new EdcException(e);
        }
    }

    private String extractPermissionIdFromContext(C context) {
        // TODO: Extract from claim
        return null;
    }

    private boolean responseIsValid(Response response) {
        if (!response.isSuccessful()) {
            this.monitor.debug("Fetch permission administrator failed, response not 200, is: " + response.code() + " " + response.message());
            response.close();
            return false;
        }

        if (response.body() == null) {
            this.monitor.debug("Response body is null");
            response.close();
            return false;
        }

        return true;
    }
}
