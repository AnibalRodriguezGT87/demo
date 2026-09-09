package com.sftp;

import com.exception.SftpException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;

import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SftpLineReaderTest {

    @Test
    void read_readsLinesFromRemoteFile() throws Exception {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpSession session = mock(SftpSession.class);
        SftpProperties properties = new SftpProperties();
        properties.setRemoteInputFile("upload/data.csv");

        when(sessionFactory.getSession()).thenReturn(session);

        doAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write("first line\nsecond line\n".getBytes());
            return null;
        }).when(session).read(eq("upload/data.csv"), any(OutputStream.class));

        SftpLineReader reader = new SftpLineReader(sessionFactory, properties);
        reader.open(new ExecutionContext());

        assertEquals("first line", reader.read());
        assertEquals("second line", reader.read());
        assertNull(reader.read());

        reader.close();
    }

    @Test
    void open_throwsSftpException_whenSessionCannotBeCreated() {
        DefaultSftpSessionFactory sessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpProperties properties = new SftpProperties();
        properties.setRemoteInputFile("upload/data.csv");
        when(sessionFactory.getSession()).thenThrow(new RuntimeException("boom"));

        SftpLineReader reader = new SftpLineReader(sessionFactory, properties);

        ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(new ExecutionContext()));
        assertTrue(exception.getCause() instanceof SftpException);
        assertTrue(exception.getCause().getMessage().contains("Error occurred while opening SFTP connection"));
    }
}
