package com.sftp;

import com.crypto.PGPProperties;
import com.crypto.PgpService;
import com.exception.SftpException;
import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SftpServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void sftpException_preservesMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");

        SftpException messageOnly = new SftpException("message");
        SftpException withCause = new SftpException("message", cause);

        assertEquals("message", messageOnly.getMessage());
        assertEquals("message", withCause.getMessage());
        assertEquals(cause, withCause.getCause());
    }

    @Test
    void downloadFile_movesDownloadedFile_whenPgpIsDisabled() throws Exception {
        Session<SftpClient.DirEntry> session = mockSession();
        doAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write("file contents".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(session).read(eq(tempDir + "/data.gpg"), any(OutputStream.class));

        SftpService service = service(session, new PGPProperties(), mock(PgpService.class));

        service.downloadFile(tempDir.toString(), tempDir.toString(), "data.gpg");

        assertEquals("file contents", Files.readString(tempDir.resolve("data.gpg")));
        verify(session).read(eq(tempDir + "/data.gpg"), any(OutputStream.class));
    }

    @Test
    void downloadFile_decryptsDownloadedFile_whenPgpIsEnabled() throws Exception {
        Session<SftpClient.DirEntry> session = mockSession();
        doAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write("encrypted".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(session).read(eq(tempDir + "/data.gpg"), any(OutputStream.class));

        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(true);
        PgpService pgpService = mock(PgpService.class);
        doAnswer(invocation -> {
            InputStream encrypted = invocation.getArgument(0);
            OutputStream decrypted = invocation.getArgument(1);
            assertEquals("encrypted", new String(encrypted.readAllBytes(), StandardCharsets.UTF_8));
            decrypted.write("decrypted".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(pgpService).decrypt(any(InputStream.class), any(OutputStream.class));

        SftpService service = service(session, pgpProperties, pgpService);

        service.downloadFile(tempDir.toString(), tempDir.toString(), "data.gpg");

        assertEquals("decrypted", Files.readString(tempDir.resolve("data.txt")));
        verify(pgpService).decrypt(any(InputStream.class), any(OutputStream.class));
    }

    @Test
    void downloadFile_propagatesReadFailure() throws Exception {
        Session<SftpClient.DirEntry> session = mockSession();
        doThrow(new IOException("read failed"))
                .when(session).read(eq(tempDir + "/data.gpg"), any(OutputStream.class));

        SftpService service = service(session, new PGPProperties(), mock(PgpService.class));

        IOException exception = assertThrows(IOException.class,
                () -> service.downloadFile(tempDir.toString(), tempDir.toString(), "data.gpg"));
        assertEquals("read failed", exception.getMessage());
    }

    @Test
    void listRemoteFiles_filtersNullBlankAndUnsupportedNames() throws Exception {
        Session<SftpClient.DirEntry> session = mockSession();
        SftpClient.DirEntry csv = entry("one.csv");
        SftpClient.DirEntry text = entry("two.txt");
        SftpClient.DirEntry unsupported = entry("three.md");
        SftpClient.DirEntry blank = entry(" ");
        SftpClient.DirEntry withoutName = entry(null);
        when(session.list(tempDir.toString()))
                .thenReturn(new SftpClient.DirEntry[]{csv, text, unsupported, blank, withoutName});

        SftpProperties properties = properties();
        properties.setFileExtension(".csv,.txt");
        SftpService service = new SftpService(properties, new PGPProperties(),
                new SftpTemplate(sessionFactory(session)), mock(PgpService.class));

        assertEquals(List.of("one.csv", "two.txt"), service.listRemoteFiles());
    }

    @Test
    void write_writesPlainText_whenPgpIsDisabled() throws Exception {
        Session<SftpClient.DirEntry> session = mockSession();
        SftpService service = service(session, new PGPProperties(), mock(PgpService.class));

        service.write("hello", "file.csv");

        verify(session).write(any(InputStream.class), eq("file.csv"));
    }

    @Test
    void write_encryptsAndWrites_whenPgpIsEnabled() throws Exception {
        Session<SftpClient.DirEntry> session = mockSession();
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(true);
        PgpService pgpService = mock(PgpService.class);
        when(pgpService.encrypt("hello")).thenReturn(
                new ByteArrayInputStream("encrypted".getBytes(StandardCharsets.UTF_8)));
        SftpService service = service(session, pgpProperties, pgpService);

        service.write("hello", "file.csv");

        verify(pgpService).encrypt("hello");
        verify(session).write(any(InputStream.class), eq("file.csv"));
    }

    @Test
    void write_wrapsWriteFailureInRuntimeException() throws Exception {
        Session<SftpClient.DirEntry> session = mockSession();
        doThrow(new IOException("write failed")).when(session)
                .write(any(InputStream.class), anyString());
        SftpService service = service(session, new PGPProperties(), mock(PgpService.class));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.write("hello", "file.csv"));

        SftpException cause = assertInstanceOf(SftpException.class, exception.getCause());
        assertEquals("Error occurred while writing file: write failed", cause.getMessage());
        assertInstanceOf(IOException.class, cause.getCause());
    }

    @Test
    void write_wrapsEncryptionFailureInRuntimeException() throws Exception {
        Session<SftpClient.DirEntry> session = mockSession();
        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(true);
        PgpService pgpService = mock(PgpService.class);
        when(pgpService.encrypt("hello")).thenThrow(new IOException("encryption failed"));
        SftpService service = service(session, pgpProperties, pgpService);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.write("hello", "file.csv"));

        SftpException cause = assertInstanceOf(SftpException.class, exception.getCause());
        assertEquals("Error occurred while writing file: encryption failed", cause.getMessage());
        assertInstanceOf(IOException.class, cause.getCause());
    }

    @SuppressWarnings("unchecked")
    private Session<SftpClient.DirEntry> mockSession() {
        return mock(Session.class);
    }

    @SuppressWarnings("unchecked")
    private SessionFactory<SftpClient.DirEntry> sessionFactory(Session<SftpClient.DirEntry> session)
            throws IOException {
        SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
        when(sessionFactory.getSession()).thenReturn(session);
        return sessionFactory;
    }

    private SftpService service(Session<SftpClient.DirEntry> session, PGPProperties pgpProperties,
                                PgpService pgpService) throws IOException {
        return new SftpService(properties(), pgpProperties,
                new SftpTemplate(sessionFactory(session)), pgpService);
    }

    private SftpProperties properties() {
        SftpProperties properties = new SftpProperties();
        properties.setRemoteDirectoryInput(tempDir.toString());
        properties.setFileExtension(".gpg");
        properties.setTempFileExtension(".txt");
        return properties;
    }

    private SftpClient.DirEntry entry(String filename) {
        SftpClient.DirEntry entry = mock(SftpClient.DirEntry.class);
        when(entry.getFilename()).thenReturn(filename);
        return entry;
    }
}
