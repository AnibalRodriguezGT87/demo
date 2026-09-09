package com.sftp;

import com.exception.SftpException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SftpFileWriterTest {

    @Test
    void write_and_close_uploadFileToSftp() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        when(sessionFactory.getSession()).thenReturn(session);

        SftpProperties properties = new SftpProperties();
        SftpFileWriter writer = new SftpFileWriter(sessionFactory, properties);
        setField(writer, "remoteDirectory", "/upload");
        setField(writer, "fileName", "output.txt");

        writer.open(new ExecutionContext());
        writer.write(new Chunk<>(List.of("first", "second")));
        writer.close();

        org.mockito.ArgumentCaptor<ByteArrayInputStream> streamCaptor = forClass(ByteArrayInputStream.class);
        verify(session).write(streamCaptor.capture(), eq("/upload/output.txt"));
        assertEquals("first\nsecond\n", new String(streamCaptor.getValue().readAllBytes()));
        verify(session).close();
    }

    @Test
    void write_throwsSftpException_whenWriterWasNotOpened() {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpFileWriter writer = new SftpFileWriter(sessionFactory, new SftpProperties());

        SftpException exception = assertThrows(SftpException.class, () ->
                writer.write(new Chunk<>(List.of("line"))));

        assertTrue(exception.getMessage().contains("Error writing to SFTP file"));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
