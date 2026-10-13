package com.sftp;

import com.crypto.PGPProperties;
import com.crypto.PgpService;
import com.exception.SftpException;
import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SftpServiceTest {

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
    void readFile_returnsContent_whenSessionReadsSuccessfully() throws Exception {
        SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
        @SuppressWarnings("unchecked")
        Session<SftpClient.DirEntry> session = mock(Session.class);
        when(sessionFactory.getSession()).thenReturn(session);
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(1);
            os.write("hello".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(session).read(eq("/upload/data.csv"), any(OutputStream.class));

        SftpProperties props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");
        props.setTempFileExtension(".tmp");

        SftpService service = new SftpService(props, new PGPProperties(), new SftpTemplate(sessionFactory), mock(PgpService.class));

        try (BufferedReader reader = service.readFile("data.csv")) {
            assertEquals("hello", reader.readLine());
        }
    }

    @Test
    void readFile_throwsRuntimeException_whenSessionReadFails() throws Exception {
        SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
        @SuppressWarnings("unchecked")
        Session<SftpClient.DirEntry> session = mock(Session.class);
        when(sessionFactory.getSession()).thenReturn(session);
        doThrow(new RuntimeException("read boom")).when(session).read(anyString(), any(OutputStream.class));

        SftpProperties props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");
        props.setTempFileExtension(".tmp");

        SftpService service = new SftpService(props, new PGPProperties(), new SftpTemplate(sessionFactory), mock(PgpService.class));

        assertThrows(RuntimeException.class, () -> service.readFile("data.csv"));
    }

    @Test
    void readFile_decryptsWhenPgpEnabled() throws Exception {
        SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
        @SuppressWarnings("unchecked")
        Session<SftpClient.DirEntry> session = mock(Session.class);
        when(sessionFactory.getSession()).thenReturn(session);
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(1);
            os.write("encrypted".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(session).read(eq("/upload/data.csv"), any(OutputStream.class));

        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(true);
        PgpService pgpService = mock(PgpService.class);
        when(pgpService.decrypt(any(InputStream.class))).thenReturn(new ByteArrayInputStream("decrypted".getBytes(StandardCharsets.UTF_8)));

        SftpProperties props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");
        props.setTempFileExtension(".tmp");

        SftpService service = new SftpService(props, pgpProperties, new SftpTemplate(sessionFactory), pgpService);

        try (BufferedReader reader = service.readFile("data.csv")) {
            assertEquals("decrypted", reader.readLine());
        }
    }

    @Test
    void listRemoteFiles_filtersOutNonMatchingEntries() throws Exception {
        SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
        @SuppressWarnings("unchecked")
        Session<SftpClient.DirEntry> session = mock(Session.class);
        when(sessionFactory.getSession()).thenReturn(session);

        SftpClient.DirEntry a = mock(SftpClient.DirEntry.class);
        SftpClient.DirEntry b = mock(SftpClient.DirEntry.class);
        SftpClient.DirEntry c = mock(SftpClient.DirEntry.class);
        when(a.getFilename()).thenReturn("one.csv");
        when(b.getFilename()).thenReturn("two.txt");
        when(c.getFilename()).thenReturn("three.csv");
        when(session.list("/upload")).thenReturn(new SftpClient.DirEntry[]{a, b, c});

        SftpProperties props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");
        props.setFileExtension(".csv");

        SftpService service = new SftpService(props, new PGPProperties(), new SftpTemplate(sessionFactory), mock(PgpService.class));

        assertEquals(List.of("one.csv", "three.csv"), service.listRemoteFiles());
    }

    @Test
    void listRemoteFiles_throwsRuntimeException_whenListingFails() throws Exception {
        SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
        @SuppressWarnings("unchecked")
        Session<SftpClient.DirEntry> session = mock(Session.class);
        when(sessionFactory.getSession()).thenReturn(session);
        when(session.list("/upload")).thenThrow(new RuntimeException("list boom"));

        SftpProperties props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");
        props.setFileExtension(".csv");

        SftpService service = new SftpService(props, new PGPProperties(), new SftpTemplate(sessionFactory), mock(PgpService.class));

        assertThrows(RuntimeException.class, () -> service.listRemoteFiles());
    }

    @Test
    void write_writesPlainTextData_whenPgpDisabled() throws Exception {
        SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
        @SuppressWarnings("unchecked")
        Session<SftpClient.DirEntry> session = mock(Session.class);
        when(sessionFactory.getSession()).thenReturn(session);

        SftpProperties props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");

        SftpService service = new SftpService(props, new PGPProperties(), new SftpTemplate(sessionFactory), mock(PgpService.class));

        service.write("hello", "file.csv");

        verify(session).write(any(InputStream.class), eq("file.csv"));
    }

    @Test
    void write_encryptsAndWrites_whenPgpEnabled() throws Exception {
        SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
        @SuppressWarnings("unchecked")
        Session<SftpClient.DirEntry> session = mock(Session.class);
        when(sessionFactory.getSession()).thenReturn(session);

        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setEnabled(true);
        PgpService pgpService = mock(PgpService.class);
        when(pgpService.encrypt("hello")).thenReturn(new ByteArrayInputStream("encrypted".getBytes(StandardCharsets.UTF_8)));

        SftpProperties props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");

        SftpService service = new SftpService(props, pgpProperties, new SftpTemplate(sessionFactory), pgpService);

        service.write("hello", "file.csv");

        verify(pgpService).encrypt("hello");
        verify(session).write(any(InputStream.class), eq("file.csv"));
    }

    @Test
    void write_throwsRuntimeException_whenWriteFails() throws Exception {
        SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
        @SuppressWarnings("unchecked")
        Session<SftpClient.DirEntry> session = mock(Session.class);
        when(sessionFactory.getSession()).thenReturn(session);
        doThrow(new RuntimeException("write boom")).when(session).write(any(InputStream.class), anyString());

        SftpProperties props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");

        SftpService service = new SftpService(props, new PGPProperties(), new SftpTemplate(sessionFactory), mock(PgpService.class));

        assertThrows(RuntimeException.class, () -> service.write("hello", "file.csv"));
    }
}
