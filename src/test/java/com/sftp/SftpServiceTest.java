package com.sftp;

import com.exception.SftpException;
import com.pgp.PGPProperties;
import com.pgp.PgpService;
import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SftpServiceTest {

    @Test
    void openSftpSession_opensSession() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        SftpService service = new SftpService(sessionFactory, new PGPProperties(), new SftpProperties(), mock(PgpService.class));

        service.openSftpSession();

        verify(sessionFactory).getSession();
    }

    @Test
    void openSftpSession_throwsSftpException_whenSessionCannotBeCreated() {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        when(sessionFactory.getSession()).thenThrow(new RuntimeException("boom"));

        SftpService service = new SftpService(sessionFactory, new PGPProperties(), new SftpProperties(), mock(PgpService.class));

        SftpException exception = assertThrows(SftpException.class, service::openSftpSession);
        assertTrue(exception.getMessage().contains("Error occurred while opening SFTP connection"));
    }

    @Test
    void readFile_readsLinesFromRemoteFile() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        doAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write("first\nsecond\n".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(session).read(eq("upload/data.csv"), any(OutputStream.class));

        SftpService service = new SftpService(sessionFactory, new PGPProperties(), new SftpProperties(), mock(PgpService.class));
        service.openSftpSession();
        service.readFile("upload/data.csv");

        assertEquals("first", service.getRowLine());
        assertEquals("second", service.getRowLine());
        assertNull(service.getRowLine());
        assertNotNull(service.getBufferedReader());

        service.closeReader();
        service.closeSession();
    }

    @Test
    void readDecryptedFile_readsAndDecryptsRemoteFile() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);
        PgpService pgpService = mock(PgpService.class);
        when(pgpService.decrypt(any(), any(), any())).thenReturn(new ByteArrayInputStream("plain\nline\n".getBytes(StandardCharsets.UTF_8)));

        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setPrivateKey("data.csv");

        SftpProperties properties = new SftpProperties();
        properties.setFileExtension(".pgp");
        properties.setTempFileExtension(".tmp");

        doAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write("encrypted".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(session).read(eq("upload/data.pgp"), any(OutputStream.class));

        SftpService service = new SftpService(sessionFactory, pgpProperties, properties, pgpService);
        service.openSftpSession();
        service.readEncryptedFile("upload/data.pgp");

        assertEquals("plain", service.getRowLine());
        assertEquals("line", service.getRowLine());

        service.closeReader();
        service.closeSession();
    }

    @Test
    void readFirstFile_readsFirstMatchingFileInDirectory() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        SftpClient.DirEntry firstEntry = mock(SftpClient.DirEntry.class);
        SftpClient.DirEntry secondEntry = mock(SftpClient.DirEntry.class);
        when(firstEntry.getFilename()).thenReturn("first.csv");
        when(secondEntry.getFilename()).thenReturn("second.csv");
        when(session.list("/upload")).thenReturn(new SftpClient.DirEntry[]{firstEntry, secondEntry});

        doAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write("first\nsecond\n".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(session).read(eq("/upload/first.csv"), any(OutputStream.class));

        SftpProperties properties = new SftpProperties();
        properties.setRemoteDirectoryInput("/upload");
        SftpService service = new SftpService(sessionFactory, new PGPProperties(), properties, mock(PgpService.class));
        service.openSftpSession();
        service.readFirstFile();

        assertEquals("first", service.getRowLine());
        service.closeReader();
        service.closeSession();
    }

    @Test
    void readFirstDecryptedFile_readsFirstMatchingEncryptedFile() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        PgpService pgpService = mock(PgpService.class);
        when(pgpService.decrypt(any(), any(), any())).thenReturn(new ByteArrayInputStream("secret\n".getBytes(StandardCharsets.UTF_8)));

        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setPrivateKey("data.csv");

        SftpProperties properties = new SftpProperties();
        properties.setFileExtension(".pgp");
        properties.setRemoteDirectoryInput("/upload");

        SftpClient.DirEntry ignoredEntry = mock(SftpClient.DirEntry.class);
        SftpClient.DirEntry encryptedEntry = mock(SftpClient.DirEntry.class);
        when(ignoredEntry.getFilename()).thenReturn("ignored.txt");
        when(encryptedEntry.getFilename()).thenReturn("data.pgp");
        when(session.list("/upload")).thenReturn(new SftpClient.DirEntry[]{ignoredEntry, encryptedEntry});

        doAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write("cipher".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(session).read(eq("/upload/data.pgp"), any(OutputStream.class));

        SftpService service = new SftpService(sessionFactory, pgpProperties, properties, pgpService);
        service.openSftpSession();
        service.readFirstEncryptedFile();

        assertEquals("secret", service.getRowLine());
        service.closeReader();
        service.closeSession();
    }

    @Test
    void setOutputStreamAndWriteSftpFile_writesToRemoteFile() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        SftpService service = new SftpService(sessionFactory, new PGPProperties(), new SftpProperties(), mock(PgpService.class));
        service.openSftpSession();
        service.setOutputStream();
        service.setInputStream("first");
        service.setInputStream("second");
        service.writeSftpFile("/upload", "output.csv");

        var captor = forClass(ByteArrayInputStream.class);
        verify(session).write(captor.capture(), eq("/upload/output.csv"));
        assertEquals("first\nsecond\n", new String(captor.getValue().readAllBytes(), StandardCharsets.UTF_8));

        service.closeSession();
    }

    @Test
    void setInputStream_throwsSftpException_whenOutputStreamIsNotInitialized() {
        SftpService service = new SftpService(mock(DefaultSftpSessionFactory.class), new PGPProperties(), new SftpProperties(), mock(PgpService.class));

        SftpException exception = assertThrows(SftpException.class, () -> service.setInputStream("value"));
        assertTrue(exception.getMessage().contains("Error writing to SFTP file"));
    }

    @Test
    void getBufferedReader_returnsNull_whenReaderIsNotInitialized() {
        SftpService service = new SftpService(mock(DefaultSftpSessionFactory.class), new PGPProperties(), new SftpProperties(), mock(PgpService.class));

        assertNull(service.getBufferedReader());
    }

    @Test
    void closeReader_throwsSftpException_whenReaderCloseFails() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        SftpService service = new SftpService(sessionFactory, new PGPProperties(), new SftpProperties(), mock(PgpService.class));
        service.openSftpSession();
        service.readFile("upload/data.csv");

        BufferedReader mockedReader = mock(BufferedReader.class);
        doThrow(new RuntimeException("close boom")).when(mockedReader).close();
        ReflectionTestUtils.setField(service, "reader", mockedReader);

        SftpException exception = assertThrows(SftpException.class, service::closeReader);
        assertTrue(exception.getMessage().contains("Error occurred while closing the reader"));

        service.closeSession();
    }

    @Test
    void closeSession_throwsSftpException_whenSessionCloseFails() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);
        doThrow(new RuntimeException("close boom")).when(session).close();

        SftpService service = new SftpService(sessionFactory, new PGPProperties(), new SftpProperties(), mock(PgpService.class));
        service.openSftpSession();

        SftpException exception = assertThrows(SftpException.class, service::closeSession);
        assertTrue(exception.getMessage().contains("Error occurred while closing the sftp session"));
    }

    @Test
    void readFirstFile_throwsSftpException_whenDirectoryListingFails() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);
        when(session.list("/upload")).thenThrow(new RuntimeException("listing boom"));

        SftpProperties properties = new SftpProperties();
        properties.setRemoteDirectoryInput("/upload");
        SftpService service = new SftpService(sessionFactory, new PGPProperties(), properties, mock(PgpService.class));
        service.openSftpSession();

        SftpException exception = assertThrows(SftpException.class, service::readFirstFile);
        assertTrue(exception.getMessage().contains("Error occurred while listing files in remote directory"));

        service.closeSession();
    }

    @Test
    void readFirstDecryptedFile_throwsSftpException_whenDirectoryListingFails() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);
        when(session.list("/upload")).thenThrow(new RuntimeException("listing boom"));

        SftpProperties properties = new SftpProperties();
        properties.setRemoteDirectoryInput("/upload");
        SftpService service = new SftpService(sessionFactory, new PGPProperties(), properties, mock(PgpService.class));
        service.openSftpSession();

        SftpException exception = assertThrows(SftpException.class, service::readFirstEncryptedFile);
        assertTrue(exception.getMessage().contains("Error occurred while listing files in remote directory"));

        service.closeSession();
    }

    @Test
    void readFirstFile_throwsSftpException_whenDirectoryIsEmpty() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);
        when(session.list("/upload")).thenReturn(new SftpClient.DirEntry[0]);

        SftpProperties properties = new SftpProperties();
        properties.setRemoteDirectoryInput("/upload");
        SftpService service = new SftpService(sessionFactory, new PGPProperties(), properties, mock(PgpService.class));
        service.openSftpSession();

        SftpException exception = assertThrows(SftpException.class, service::readFirstFile);
        assertTrue(exception.getMessage().contains("Error occurred while listing files in remote directory"));

        service.closeSession();
    }

    @Test
    void readFirstDecryptedFile_throwsSftpException_whenNoEncryptedFilesExist() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        SftpClient.DirEntry plainFile = mock(SftpClient.DirEntry.class);
        when(plainFile.getFilename()).thenReturn("notes.txt");
        when(session.list("/upload")).thenReturn(new SftpClient.DirEntry[]{plainFile});

        PGPProperties pgpProperties = new PGPProperties();
        pgpProperties.setPrivateKey("data.csv");
        SftpProperties properties = new SftpProperties();
        properties.setFileExtension(".pgp");
        properties.setRemoteDirectoryInput("/upload");

        SftpService service = new SftpService(sessionFactory, pgpProperties, properties, mock(PgpService.class));
        service.openSftpSession();

        SftpException exception = assertThrows(SftpException.class, service::readFirstEncryptedFile);
        assertTrue(exception.getMessage().contains("Error occurred while listing files in remote directory"));

        service.closeSession();
    }
}
