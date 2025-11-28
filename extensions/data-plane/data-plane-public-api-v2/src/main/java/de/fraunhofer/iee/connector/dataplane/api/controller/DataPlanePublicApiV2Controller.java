/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - handle HEAD requests
 *
 */

package de.fraunhofer.iee.connector.dataplane.api.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.response.TransferErrorResponse;
import org.eclipse.edc.connector.dataplane.util.sink.AsyncStreamingDataSink;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.WILDCARD;
import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.status;

@Path("{any:.*}")
@Produces(WILDCARD)
public class DataPlanePublicApiV2Controller implements DataPlanePublicApiV2 {

    private final PipelineService pipelineService;
    private final DataFlowRequestSupplier requestSupplier;
    private final ExecutorService executorService;
    private final DataPlaneAuthorizationService authorizationService;

    public DataPlanePublicApiV2Controller(PipelineService pipelineService,
                                          ExecutorService executorService,
                                          DataPlaneAuthorizationService authorizationService) {
        this.pipelineService = pipelineService;
        this.authorizationService = authorizationService;
        this.requestSupplier = new DataFlowRequestSupplier();
        this.executorService = executorService;
    }

    private static Response error(Response.Status status, List<String> errors) {
        return status(status).type(APPLICATION_JSON).entity(new TransferErrorResponse(errors)).build();
    }

    @GET
    @Override
    public void get(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    @HEAD
    @Override
    public void head(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link POST} request to the data source and returns data.
     *
     * @param requestContext Request context.
     * @param response       Data fetched from the data source.
     */
    @POST
    @Override
    public void post(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link PUT} request to the data source and returns data.
     *
     * @param requestContext Request context.
     * @param response       Data fetched from the data source.
     */
    @PUT
    @Override
    public void put(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link DELETE} request to the data source and returns data.
     *
     * @param requestContext Request context.
     * @param response       Data fetched from the data source.
     */
    @DELETE
    @Override
    public void delete(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link PATCH} request to the data source and returns data.
     *
     * @param requestContext Request context.
     * @param response       Data fetched from the data source.
     */
    @PATCH
    @Override
    public void patch(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    private void handle(ContainerRequestContext requestContext, AsyncResponse response) {
        var contextApi = new ContainerRequestContextApiImpl(requestContext);

        var token = contextApi.headers().get(HttpHeaders.AUTHORIZATION);
        if (token == null) {
            response.resume(error(UNAUTHORIZED, List.of("Missing Authorization Header")));
            return;
        }

        var sourceDataAddress = authorizationService.authorize(token, buildRequestData(requestContext));
        if (sourceDataAddress.failed()) {
            response.resume(error(FORBIDDEN, sourceDataAddress.getFailureMessages()));
            return;
        }

        // Check for an asset that uses SM-PKI certificates
        var address = sourceDataAddress.getContent();
        if (PKIValidator.isMaKo(address)) {
            // Check the header
            var pkiCheck = PKIValidator.checkHeader(contextApi.headers(), address);
            if (pkiCheck.failed()) {
                response.resume(error(FORBIDDEN, pkiCheck.getFailureMessages()));
            }
        }

        var startMessage = requestSupplier.apply(contextApi, sourceDataAddress.getContent());

        processRequest(startMessage, response);
    }

    private Map<String, Object> buildRequestData(ContainerRequestContext requestContext) {
        var requestData = new HashMap<String, Object>();
        requestData.put("headers", requestContext.getHeaders());
        requestData.put("path", requestContext.getUriInfo());
        requestData.put("method", requestContext.getMethod());
        requestData.put("content-type", requestContext.getMediaType());
        return requestData;
    }

    private void processRequest(DataFlowStartMessage dataFlowStartMessage, AsyncResponse response) {

        AsyncStreamingDataSink.AsyncResponseContext asyncResponseContext = callback -> {
            StreamingOutput output = t -> callback.outputStreamConsumer().accept(t);
            var resp = Response.ok(output).type(callback.mediaType()).build();
            return response.resume(resp);
        };

        var sink = new AsyncStreamingDataSink(asyncResponseContext, executorService);

        pipelineService.transfer(dataFlowStartMessage, sink)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        if (result.failed()) {
                            response.resume(error(INTERNAL_SERVER_ERROR, result.getFailureMessages()));
                        }
                    } else {
                        var error = "Unhandled exception occurred during data transfer: " + throwable.getMessage();
                        response.resume(error(INTERNAL_SERVER_ERROR, List.of(error)));
                    }
                });
    }

}
