package com.orchestrator;

import com.sftp.SftpProperties;
import com.sftp.SftpService;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileOrchestratorTest {

    @Test
    void getListFiles_returnsValuesFromSftpService() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        when(sftpService.listRemoteFiles()).thenReturn(List.of("a.txt", "b.txt"));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, new SftpProperties());

        assertThat(orchestrator.getListFiles()).containsExactly("a.txt", "b.txt");
    }


    @Test
    void getFileReader_delegatesToSftpService() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        when(sftpService.readFile("orders.csv")).thenReturn(new BufferedReader(new StringReader("hello")));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, new SftpProperties());

        try (BufferedReader reader = orchestrator.getFileReader("orders.csv")) {
            assertThat(reader.readLine()).isEqualTo("hello");
        }
    }

    @Test
    void getFileReader_wrapsExceptions() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        when(sftpService.readFile("orders.csv")).thenThrow(new RuntimeException("bad read"));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, new SftpProperties());

        assertThatThrownBy(() -> orchestrator.getFileReader("orders.csv"))
                .isInstanceOf(FileOrchestratorException.class)
                .hasMessageContaining("Error occurred while reading file");
    }

    @Test
    void write_usesConfiguredRemoteDirectory() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setRemoteDirectoryOutput("/remote/output");

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, sftpProperties);

        orchestrator.write("orders.csv", "payload");

        verify(sftpService).write("payload", "/remote/output/orders.csv");
    }

    @Test
    void write_usesOverrideDirectory_whenProvided() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setRemoteDirectoryOutput("/remote/output");

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, sftpProperties);

        orchestrator.write("orders.csv", "payload", "/custom");

        verify(sftpService).write("payload", "/custom/orders.csv");
    }

    @Test
    void write_wrapsExceptionsFromSftpService() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setRemoteDirectoryOutput("/remote/output");
        doThrow(new RuntimeException("boom")).when(sftpService).write("payload", "/remote/output/orders.csv");

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, sftpProperties);

        assertThatThrownBy(() -> orchestrator.write("orders.csv", "payload"))
                .isInstanceOf(FileOrchestratorException.class)
                .hasMessageContaining("Error occurred while writing file");
    }

    @Test
    void write_withExplicitDirectory_wrapsExceptionsFromSftpService() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setRemoteDirectoryOutput("/remote/output");
        doThrow(new RuntimeException("boom")).when(sftpService).write("payload", "/custom/orders.csv");

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, sftpProperties);

        assertThatThrownBy(() -> orchestrator.write("orders.csv", "payload", "/custom"))
                .isInstanceOf(FileOrchestratorException.class)
                .hasMessageContaining("Error occurred while writing file");
    }
}
