package de.fraunhofer.iee.connector.controlplane.registration.registration;

import de.fraunhofer.iee.connector.controlplane.registry.ConnectorRegistryService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegistryReregisterServiceTest {
    private static final String CONNECTOR_NAME = "test-connector";
    private static final String DSP_URL = "http://localhost:8080/dsp";

    private final Monitor monitor = mock();
    private final ConnectorRegistryService registryService = mock();
    private final ScheduledExecutorService executor = mock();
    private final RegistryReregisterService service = new RegistryReregisterService(monitor, registryService, CONNECTOR_NAME, DSP_URL, executor);

    @AfterEach
    void tearDown() {
        Thread.interrupted();
    }

    @Test
    void shouldScheduleAtFixedRate_whenStartIsCalled() {
        service.start(30);

        verify(executor).scheduleAtFixedRate(any(Runnable.class), eq(30L), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldRegisterConnector_whenScheduledTaskRuns() {
        service.start(30);

        var captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleAtFixedRate(captor.capture(), anyLong(), anyLong(), any());

        captor.getValue().run();

        verify(registryService).registerConnector(CONNECTOR_NAME, DSP_URL);
    }

    @Test
    void shouldShutdownGracefully_whenStopIsCalled() throws Exception {
        when(executor.awaitTermination(anyLong(), any())).thenReturn(true);

        service.stop();

        verify(executor).shutdown();
        verify(executor, never()).shutdownNow();
        verifyNoInteractions(monitor);
    }

    @Test
    void shouldForceShutdown_whenAwaitTerminationTimesOut() throws Exception {
        when(executor.awaitTermination(anyLong(), any())).thenReturn(false).thenReturn(true);

        service.stop();

        verify(executor).shutdown();
        verify(executor).shutdownNow();
        verify(monitor, never()).severe(anyString());
    }

    @Test
    void shouldLogSevere_whenForceShutdownTimesOut() throws Exception {
        when(executor.awaitTermination(anyLong(), any())).thenReturn(false).thenReturn(false);

        service.stop();

        verify(executor).shutdown();
        verify(executor).shutdownNow();
        verify(monitor).severe(contains("await termination timeout"));
    }

    @Test
    void shouldHandleInterrupted_whenAwaitTerminationIsInterrupted() throws Exception {
        var exception = new InterruptedException();
        when(executor.awaitTermination(anyLong(), any())).thenThrow(exception);

        service.stop();
        verify(monitor).severe(contains("await termination failed"), eq(exception));
        verify(executor).shutdownNow();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}