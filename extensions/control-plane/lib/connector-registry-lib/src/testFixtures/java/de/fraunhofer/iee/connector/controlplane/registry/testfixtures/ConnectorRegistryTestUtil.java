package de.fraunhofer.iee.connector.controlplane.registry.testfixtures;

import de.fraunhofer.iee.connector.controlplane.registry.ConnectorRegistryService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.mockito.Mockito;

public class ConnectorRegistryTestUtil {

    public static ConnectorRegistryService connectorRegistryService(EdcHttpClient http, TypeManager typeManager, String participantId, String url, String apiKey) {
        return new ConnectorRegistryService(http, Mockito.mock(Monitor.class), typeManager.getMapper(), participantId, url, apiKey);
    }

    private ConnectorRegistryTestUtil() {}
}
