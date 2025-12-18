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
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;

public class CatalogNodeDirectory implements TargetNodeDirectory {

    private final Monitor monitor;
    private final ConnectorRegistryService registryService;

    public CatalogNodeDirectory(Monitor monitor, ConnectorRegistryService registryService) {
        this.monitor = monitor;
        this.registryService = registryService;
    }

    @Override
    public List<TargetNode> getAll() {
        this.monitor.debug("Pulling connectors from registry");
        return this.registryService.getAllConnectors();
    }

    // Not implemented, the external connector registry gives a list of all available participants
    @Override
    public void insert(TargetNode targetNode) {}

    // Not implemented, the external connector registry gives a list of all available participants
    @Override
    public TargetNode remove(String id) {
        return null;
    }
}
