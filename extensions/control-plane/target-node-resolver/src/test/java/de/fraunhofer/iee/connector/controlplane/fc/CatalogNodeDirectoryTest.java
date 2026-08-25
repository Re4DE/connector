package de.fraunhofer.iee.connector.controlplane.fc;

import de.fraunhofer.iee.connector.controlplane.registry.ConnectorRegistryService;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class CatalogNodeDirectoryTest {

    private final Monitor monitor = mock();
    private final ConnectorRegistryService connectorRegistryService = mock();
    private CatalogNodeDirectory catalogNodeDirectory;

    @BeforeEach
    void setUp() {
        catalogNodeDirectory = new CatalogNodeDirectory(monitor, connectorRegistryService);
    }


    @Test
    void shouldDelegateToRegistryService() {
        var expectedNodes = new ArrayList<TargetNode>();
        when(connectorRegistryService.getAllConnectors()).thenReturn(expectedNodes);

        var result = catalogNodeDirectory.getAll();
        assertSame(expectedNodes, result);
        verify(connectorRegistryService).getAllConnectors();
    }
}