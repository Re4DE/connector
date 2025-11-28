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

package de.fraunhofer.iee.connector.controlplane.fc.ssi;

import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static java.util.Optional.ofNullable;

@Provides(TargetNodeDirectory.class)
@Extension(value = "Connector Registry node directory for ssi identity provider")
public class CatalogNodeDirectoryExtension implements ServiceExtension {

    @Setting(key = "edc.catalog.registry.enabled", description = "Switch to activate the node target node resolution through a external registry, default is true", defaultValue = "true")
    private boolean enabled;

    @Setting(key = "edc.catalog.registry.url", description = "The endpoint of a connector registry to fetch the target node directory")
    private String connectorRegistryUrl;

    @Setting(key = "edc.catalog.registry.token.url", description = "The endpoint of the connector registry to perform the self registration")
    private String tokenUrl;

    @Setting(key = "edc.catalog.registry.token.client.id", description = "The endpoint of the connector registry to perform the self registration")
    private String tokenClientId;

    @Setting(key = "edc.catalog.registry.token.client.secret.alias", description = "The endpoint of the connector registry to perform the self registration")
    private String tokenClientSecretAlias;

    @Inject
    private EdcHttpClient httpclient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Oauth2Client oauth2Client;

    @Inject
    Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (this.enabled) {
            this.typeManager.registerTypes(TargetNode.class);

            var tokenClientSecret = ofNullable(this.vault.resolveSecret(this.tokenClientSecretAlias))
                    .orElseThrow(() -> new EdcException("Could not retrieve client secret for catalog, could not find: %s".formatted(this.tokenClientSecretAlias)));

            var targetNodeDirectory = new CatalogNodeDirectory(this.httpclient, this.oauth2Client, context.getMonitor(), this.connectorRegistryUrl, this.tokenUrl, this.tokenClientId, tokenClientSecret, this.typeManager.getMapper());
            context.registerService(TargetNodeDirectory.class, targetNodeDirectory);
        } else {
            context.getMonitor().warning("Target node resolution through external registry is deactivated! Catalog of other participants will not be crawled");
        }
    }
}
