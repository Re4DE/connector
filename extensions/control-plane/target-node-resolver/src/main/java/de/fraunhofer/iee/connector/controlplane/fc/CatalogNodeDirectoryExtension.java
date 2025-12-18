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

package de.fraunhofer.iee.connector.controlplane.fc;

import de.fraunhofer.iee.connector.controlplane.registry.ConnectorRegistryService;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

@Provides(TargetNodeDirectory.class)
@Extension(value = "Connector Registry Node Directory")
public class CatalogNodeDirectoryExtension implements ServiceExtension {

    @Setting(key = "edc.catalog.registry.enabled", description = "Switch to activate the node target node resolution through a external registry, default is true", defaultValue = "true")
    private boolean enabled;

    @Setting(key = "edc.catalog.registry.url", description = "The endpoint of a connector registry to fetch the target node directory")
    private String connectorRegistryUrl;

    @Setting(key = "edc.catalog.registry.api.key", description = "The api key needed to authenticated")
    private String apiKey;

    @Inject
    private EdcHttpClient httpclient;

    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (this.enabled) {
            this.typeManager.registerTypes(TargetNode.class);

            var registryClient = new ConnectorRegistryService(this.httpclient, context.getMonitor(), this.typeManager.getMapper(), this.connectorRegistryUrl, this.apiKey);

            var targetNodeDirectory = new CatalogNodeDirectory(context.getMonitor(), registryClient);
            context.registerService(TargetNodeDirectory.class, targetNodeDirectory);
        } else {
            context.getMonitor().warning("Target node resolution through external registry is deactivated! Catalog of other participants will not be crawled");
        }
    }
}
