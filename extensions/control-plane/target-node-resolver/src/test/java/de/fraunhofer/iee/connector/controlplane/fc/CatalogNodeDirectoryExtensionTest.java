package de.fraunhofer.iee.connector.controlplane.fc;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(DependencyInjectionExtension.class)
class CatalogNodeDirectoryExtensionTest {

    private static final String API_KEY = "devpass";
    private static final String CONNECTOR_REGISTRY_URL = "http://localhost:8080/api/registry";
    private static final String PARTICIPANT_ID = "did:web:localhost:test";
    private final EdcHttpClient http = mock();
    private final TypeManager typeManager = mock();
    private final Monitor monitor = mock();
    private final SingleParticipantContextSupplier participantContextSupplier = mock();
    private CatalogNodeDirectoryExtension extension;
    private ServiceExtensionContext context;
    private ObjectFactory factory;
    private Map<String, String> configMap;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        this.context = context;
        this.factory = factory;
        when(context.getMonitor()).thenReturn(monitor);

        context.registerService(EdcHttpClient.class, http);
        context.registerService(TypeManager.class, typeManager);
        context.registerService(SingleParticipantContextSupplier.class, participantContextSupplier);

        var participantContext = ParticipantContext.Builder.newInstance()
                .participantContextId(PARTICIPANT_ID)
                .identity(PARTICIPANT_ID)
                .build();
        when(participantContextSupplier.get()).thenReturn(ServiceResult.success(participantContext));

        configMap = new HashMap<>(Map.of(
                "edc.catalog.registry.enabled", "true",
                "edc.catalog.registry.url", CONNECTOR_REGISTRY_URL,
                "edc.catalog.registry.api.key", API_KEY
        ));
    }

    @Test
    void shouldRegisterTargetNodeDirectory_whenEnabled() {
        var config = ConfigFactory.fromMap(configMap);
        when(context.getConfig()).thenReturn(config);

        extension = factory.constructInstance(CatalogNodeDirectoryExtension.class);
        extension.initialize(context);

        verify(typeManager).registerTypes(eq(TargetNode.class));
        verify(context).registerService(eq(TargetNodeDirectory.class), isA(TargetNodeDirectory.class));
    }

    @Test
    void shouldNotRegisterTargetNodeDirectory_whenDisabled() {
        configMap.put("edc.catalog.registry.enabled", "false");
        var config = ConfigFactory.fromMap(configMap);
        when(context.getConfig()).thenReturn(config);

        extension = factory.constructInstance(CatalogNodeDirectoryExtension.class);
        extension.initialize(context);

        verify(monitor).warning("Target node resolution through external registry is deactivated! " +
                "Catalog of other participants will not be crawled");

        verify(typeManager, never()).registerTypes(eq(TargetNode.class));
        verify(context, never()).registerService(eq(TargetNodeDirectory.class), isA(TargetNodeDirectory.class));
    }
}