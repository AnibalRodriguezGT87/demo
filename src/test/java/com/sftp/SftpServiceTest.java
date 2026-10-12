package com.sftp;

import com.exception.SftpException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SftpServiceTest {

    @Test
    void readFile_throwsSftpException_whenSessionReadFails() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);
        doThrow(new RuntimeException("read boom")).when(session).read(any(String.class), any(OutputStream.class));

        var props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");
        props.setTempFileExtension(".tmp");

        var service = new SftpService(sessionFactory, props);
        service.openSftpSession();

        SftpException ex = assertThrows(SftpException.class, () -> service.readFile("data.csv"));
        assertTrue(ex.getMessage().contains("Error occurred reading decrypted file"));
    }

    @Test
    void readFile_returnsContent_whenSessionReadsSuccessfully() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        var props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");
        props.setTempFileExtension(".tmp");

        var service = new SftpService(sessionFactory, props);
        service.openSftpSession();

        try (InputStream in = service.readFile("data.csv")) {
            assertNotNull(in);
        }

        verify(session).read(eq("/upload/data.csv"), any(OutputStream.class));
    }

    @Test
    void listFiles_filtersOutNonMatchingEntries() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        var a = mock(org.apache.sshd.sftp.client.SftpClient.DirEntry.class);
        var b = mock(org.apache.sshd.sftp.client.SftpClient.DirEntry.class);
        var c = mock(org.apache.sshd.sftp.client.SftpClient.DirEntry.class);
        when(a.getFilename()).thenReturn("one.csv");
        when(b.getFilename()).thenReturn("two.txt");
        when(c.getFilename()).thenReturn("three.csv");
        when(session.list("/upload")).thenReturn(new org.apache.sshd.sftp.client.SftpClient.DirEntry[]{a, b, c});

        var props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");
        props.setFileExtension(".csv");

        var service = new SftpService(sessionFactory, props);
        service.openSftpSession();
        Iterator<String> it = service.listFiles();

        assertEquals("one.csv", it.next());
        assertEquals("three.csv", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void writeString_writesToConfiguredRemotePath() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        var props = new SftpProperties();
        props.setRemoteDirectoryOutput("/output");

        var service = new SftpService(sessionFactory, props);
        service.openSftpSession();

        service.write("hello", "file.csv");

        verify(session).write(any(InputStream.class), eq("/output/file.csv"));
    }

    @Test
    void writeInputStream_callsSessionWrite() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        var props = new SftpProperties();
        props.setRemoteDirectoryOutput("/output");

        var service = new SftpService(sessionFactory, props);
        service.openSftpSession();
        service.write(new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8)), "/output/file.csv");

        verify(session).write(any(InputStream.class), eq("/output/file.csv"));
    }

    @Test
    void openSftpSession_throwsSftpException_whenFactoryFails() {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        when(sessionFactory.getSession()).thenThrow(new RuntimeException("session boom"));

        var service = new SftpService(sessionFactory, new SftpProperties());

        SftpException ex = assertThrows(SftpException.class, service::openSftpSession);
        assertTrue(ex.getMessage().contains("Error occurred while opening SFTP connection"));
    }

    @Test
    void closeSession_closesOpenSession() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        var service = new SftpService(sessionFactory, new SftpProperties());
        service.openSftpSession();

        service.closeSession();

        verify(session).close();
    }

    @Test
    void closeSession_isNoOp_whenSessionWasNeverOpened() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var service = new SftpService(sessionFactory, new SftpProperties());

        service.closeSession();

        verifyNoInteractions(sessionFactory);
    }

    @Test
    void closeSession_throwsSftpException_whenSessionCloseFails() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);
        doThrow(new RuntimeException("close boom")).when(session).close();

        var service = new SftpService(sessionFactory, new SftpProperties());
        service.openSftpSession();

        SftpException ex = assertThrows(SftpException.class, service::closeSession);
        assertTrue(ex.getMessage().contains("Error occurred while closing the sftp session"));
    }

    @Test
    void listFiles_throwsSftpException_whenListingFails() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);
        when(session.list("/upload")).thenThrow(new RuntimeException("list boom"));

        var props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");
        props.setFileExtension(".csv");

        var service = new SftpService(sessionFactory, props);
        service.openSftpSession();

        SftpException ex = assertThrows(SftpException.class, service::listFiles);
        assertTrue(ex.getMessage().contains("Error occurred while listing files in remote directory"));
    }

    @Test
    void writeInputStream_throwsSftpException_whenWriteFails() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);
        doThrow(new RuntimeException("write boom")).when(session).write(any(InputStream.class), any(String.class));

        var props = new SftpProperties();
        props.setRemoteDirectoryOutput("/output");

        var service = new SftpService(sessionFactory, props);
        service.openSftpSession();

        SftpException ex = assertThrows(SftpException.class,
                () -> service.write(new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8)), "/output/file.csv"));
        assertTrue(ex.getMessage().contains("Error writing SFTP writer"));
    }

    @Test
    void listFiles_returnsEmptyIterator_whenNoEntryMatchesExtension() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        var entry = mock(org.apache.sshd.sftp.client.SftpClient.DirEntry.class);
        when(entry.getFilename()).thenReturn("notes.txt");
        when(session.list("/upload")).thenReturn(new org.apache.sshd.sftp.client.SftpClient.DirEntry[]{entry});

        var props = new SftpProperties();
        props.setRemoteDirectoryInput("/upload");
        props.setFileExtension(".csv");

        var service = new SftpService(sessionFactory, props);
        service.openSftpSession();

        assertFalse(service.listFiles().hasNext());
    }

    @Test
    void sftpException_constructorsPreserveMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");

        SftpException withMessage = new SftpException("message");
        SftpException withCause = new SftpException("message", cause);

        assertEquals("message", withMessage.getMessage());
        assertEquals("message", withCause.getMessage());
        assertSame(cause, withCause.getCause());
    }

    @Test
    void writeString_throwsWhenWriteFails() throws Exception {
        var sessionFactory = mock(org.springframework.integration.sftp.session.DefaultSftpSessionFactory.class);
        var session = mock(org.springframework.integration.sftp.session.SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);
        doThrow(new RuntimeException("fail")).when(session).write(any(InputStream.class), any(String.class));

        var props = new SftpProperties();
        props.setRemoteDirectoryOutput("/output");

        var service = new SftpService(sessionFactory, props);
        service.openSftpSession();

        SftpException ex = assertThrows(SftpException.class, () -> service.write("hello", "file.csv"));
        assertTrue(ex.getMessage().contains("Error closing SFTP writer"));
    }
}
