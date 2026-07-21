package de.fraunhofer.iee.connector.controlplane.registration.registration;

import de.fraunhofer.iee.connector.controlplane.registry.ConnectorRegistryService;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

public class RegistryReregisterService {
    private static final int SHUTDOWN_TIMEOUT = 10;

    private final Monitor monitor;
    private final ConnectorRegistryService registryService;
    private final String connectorName;
    private final String dspUrl;

    private final ScheduledExecutorService executor;

    public RegistryReregisterService(Monitor monitor, ConnectorRegistryService registryService, String connectorName, String dspUrl) {
        this.monitor = monitor;
        this.registryService = registryService;
        this.connectorName = connectorName;
        this.dspUrl = dspUrl;

        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void start(int delay) {
        this.executor.scheduleAtFixedRate(this::reregister, delay, delay, SECONDS);
    }

    /**
     * Stop the loop gracefully as suggested in the {@link ExecutorService} documentation
     */
    public void stop() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT, SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT, SECONDS)) {
                    monitor.severe("RegistryReregisterService await termination timeout");
                }
            }
        } catch (InterruptedException e) {
            monitor.severe("RegistryReregisterService await termination failed", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void reregister() {
        this.registryService.registerConnector(this.connectorName, this.dspUrl);
    }
}
