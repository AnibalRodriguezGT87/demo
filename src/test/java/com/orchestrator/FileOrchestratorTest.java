package com.orchestrator;

import com.crypto.PGPProperties;
import com.crypto.PgpService;
import com.exception.SftpException;
import com.sftp.SftpProperties;
import com.sftp.SftpService;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileOrchestratorTest {

    @Test
    void openSFTConnection_delegatesToSftpService() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, mock(PgpService.class), new PGPProperties(), new SftpProperties());

        orchestrator.openSFTConnection();

        verify(sftpService).openSftpSession();
    }

    @Test
    void openSFTConnection_wrapsSftpException() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        doThrow(new SftpException("fail open")).when(sftpService).openSftpSession();

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, mock(PgpService.class), new PGPProperties(), new SftpProperties());

        assertThatThrownBy(orchestrator::openSFTConnection)
                .isInstanceOf(FileOrchestratorException.class)
                .hasMessageContaining("Error occurred while opening SFTP connection");
    }

    @Test
    void setListFiles_setsIteratorFromSftpService() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        PgpService pgpService = mock(PgpService.class);
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(false);
        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setTempFileExtension(".txt");

        Path tempFile = Files.createTempFile("orchestrator-setlist-", ".txt");
        Files.writeString(tempFile, "hello from iterator", StandardCharsets.UTF_8);

        when(sftpService.listFiles()).thenReturn(List.of(tempFile.getFileName().toString()).iterator());
        when(sftpService.readFile(tempFile.getFileName().toString())).thenReturn(Files.newInputStream(tempFile));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, pgpService, pgpProperties, sftpProperties);
        orchestrator.setListFiles();

        try (BufferedReader reader = orchestrator.getNextFile()) {
            assertThat(reader).isNotNull();
            assertThat(reader.readLine()).isEqualTo("hello from iterator");
        }
    }

    @Test
    void setListFiles_wrapsSftpException() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        when(sftpService.listFiles()).thenThrow(new SftpException("fail list"));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, mock(PgpService.class), new PGPProperties(), new SftpProperties());

        assertThatThrownBy(orchestrator::setListFiles)
                .isInstanceOf(FileOrchestratorException.class)
                .hasMessageContaining("Error occurred while listing files");
    }

    @Test
    void getListFiles_returnsIteratorFromSftpService() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        when(sftpService.listFiles()).thenReturn(List.of("a.txt", "b.txt").iterator());

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, mock(PgpService.class), new PGPProperties(), new SftpProperties());

        Iterator<String> result = orchestrator.getListFiles();

        assertThat(result).toIterable().containsExactly("a.txt", "b.txt");
    }

    @Test
    void getFileReader_readsPlainText_whenPgpDisabled() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        PgpService pgpService = mock(PgpService.class);
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(false);

        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setTempFileExtension(".txt");

        Path tempFile = Files.createTempFile("orchestrator-reader-", ".txt");
        Files.writeString(tempFile, "reader content", StandardCharsets.UTF_8);

        when(sftpService.readFile("orders.csv")).thenReturn(Files.newInputStream(tempFile));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, pgpService, pgpProperties, sftpProperties);

        try (BufferedReader reader = orchestrator.getFileReader("orders.csv")) {
            assertThat(reader).isNotNull();
            assertThat(reader.readLine()).isEqualTo("reader content");
        }
    }

    @Test
    void getFileReader_decryptsWhenPgpEnabled() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        PgpService pgpService = mock(PgpService.class);
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(true);

        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setTempFileExtension(".tmp");

        when(sftpService.readFile("orders.csv")).thenReturn(new ByteArrayInputStream("encrypted".getBytes(StandardCharsets.UTF_8)));
        when(pgpService.decrypt(any(InputStream.class))).thenReturn(new ByteArrayInputStream("decrypted".getBytes(StandardCharsets.UTF_8)));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, pgpService, pgpProperties, sftpProperties);

        try (BufferedReader reader = orchestrator.getFileReader("orders.csv")) {
            assertThat(reader).isNotNull();
            assertThat(reader.readLine()).isEqualTo("decrypted");
        }
    }

    @Test
    void getFileReader_wrapsReadException() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        when(sftpService.readFile("orders.csv")).thenThrow(new SftpException("bad read"));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, mock(PgpService.class), new PGPProperties(), new SftpProperties());

        assertThatThrownBy(() -> orchestrator.getFileReader("orders.csv"))
                .isInstanceOf(FileOrchestratorException.class)
                .hasMessageContaining("Error occurred while reading file");
    }

    @Test
    void getNextFile_returnsNull_whenNoMoreFiles() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        PgpService pgpService = mock(PgpService.class);
        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, pgpService, new PGPProperties(), new SftpProperties());

        assertThat(orchestrator.getNextFile()).isNull();
    }

    @Test
    void getNextFile_readsPlainTextFile_whenPgpDisabled() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        PgpService pgpService = mock(PgpService.class);
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(false);

        Path tempFile = Files.createTempFile("orchestrator-plain-", ".txt");
        Files.writeString(tempFile, "plain content", StandardCharsets.UTF_8);

        when(sftpService.listFiles()).thenReturn(List.of(tempFile.getFileName().toString()).iterator());
        when(sftpService.readFile(tempFile.getFileName().toString())).thenReturn(Files.newInputStream(tempFile));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, pgpService, pgpProperties, new SftpProperties());
        orchestrator.setListFiles();

        try (BufferedReader reader = orchestrator.getNextFile()) {
            assertThat(reader).isNotNull();
            assertThat(reader.readLine()).isEqualTo("plain content");
        }
    }

    @Test
    void getNextFile_decryptsFile_whenPgpEnabled() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        PgpService pgpService = mock(PgpService.class);
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(true);

        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setTempFileExtension(".tmp");

        when(sftpService.listFiles()).thenReturn(List.of("report.csv").iterator());
        when(sftpService.readFile("report.csv")).thenReturn(new ByteArrayInputStream("encrypted".getBytes(StandardCharsets.UTF_8)));
        when(pgpService.decrypt(any(InputStream.class))).thenReturn(new ByteArrayInputStream("decrypted".getBytes(StandardCharsets.UTF_8)));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, pgpService, pgpProperties, sftpProperties);
        orchestrator.setListFiles();

        try (BufferedReader reader = orchestrator.getNextFile()) {
            assertThat(reader).isNotNull();
            assertThat(reader.readLine()).isEqualTo("decrypted");
        }
    }

    @Test
    void write_usesPlainWrite_whenPgpDisabled() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        PgpService pgpService = mock(PgpService.class);
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(false);

        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setRemoteDirectoryOutput("/remote/output");

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, pgpService, pgpProperties, sftpProperties);

        orchestrator.write("orders.csv", "payload");

        verify(sftpService).write("payload", "/remote/output/orders.csv");
        verifyNoInteractions(pgpService);
    }

    @Test
    void write_usesOverrideDirectory_whenProvided() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, mock(PgpService.class), new PGPProperties(), new SftpProperties());

        orchestrator.write("orders.csv", "payload", "/custom");

        verify(sftpService).write("payload", "/custom/orders.csv");
    }

    @Test
    void write_concatenatesBlankRemoteDirectoryAndFileName() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(false);

        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setRemoteDirectoryOutput(" ");

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, mock(PgpService.class), pgpProperties, sftpProperties);

        orchestrator.write("orders.csv", "payload");

        verify(sftpService).write("payload", " /orders.csv");
    }

    @Test
    void write_encryptsWhenPgpEnabled() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        PgpService pgpService = mock(PgpService.class);
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(true);

        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setRemoteDirectoryOutput("/remote/output");

        when(pgpService.encrypt("payload")).thenReturn(new ByteArrayInputStream("encrypted-payload".getBytes(StandardCharsets.UTF_8)));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, pgpService, pgpProperties, sftpProperties);

        orchestrator.write("orders.csv", "payload");

        verify(pgpService).encrypt("payload");
        verify(sftpService).write(any(InputStream.class), eq("/remote/output/orders.csv"));
    }

    @Test
    void write_wrapsEncryptionFailure() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        PgpService pgpService = mock(PgpService.class);
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(true);

        when(pgpService.encrypt("payload")).thenThrow(new RuntimeException("encrypt failed"));

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, pgpService, pgpProperties, new SftpProperties());

        assertThatThrownBy(() -> orchestrator.write("orders.csv", "payload"))
                .isInstanceOf(FileOrchestratorException.class)
                .hasMessageContaining("Error occurred while writing file");
    }

    @Test
    void closeSFTConnection_delegatesToSftpService() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, mock(PgpService.class), new PGPProperties(), new SftpProperties());

        orchestrator.closeSFTConnection();

        verify(sftpService).closeSession();
    }

    @Test
    void closeSFTConnection_wrapsSftpException() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        doThrow(new SftpException("fail close")).when(sftpService).closeSession();

        FileOrchestrator orchestrator = new FileOrchestrator(sftpService, mock(PgpService.class), new PGPProperties(), new SftpProperties());

        assertThatThrownBy(orchestrator::closeSFTConnection)
                .isInstanceOf(FileOrchestratorException.class)
                .hasMessageContaining("Error occurred while closing SFTP connection");
    }
}
