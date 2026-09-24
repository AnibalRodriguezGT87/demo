package com.job;

import com.exception.BatchWriteException;
import com.exception.SftpException;
import com.sftp.SftpService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class Iso8583MessageWriterTest {

    @Test
    void write_and_close_uploadFileToSftp() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        Iso8583MessageWriter writer = new Iso8583MessageWriter(sftpService);

        writer.open(new ExecutionContext());
        writer.write(new Chunk<>(List.of("first", "second")));
        writer.close();

        verify(sftpService).openSftpSession();
        verify(sftpService).setOutputStream();
        verify(sftpService).setInputStream("first");
        verify(sftpService).setInputStream("second");
        verify(sftpService).write("output.csv.pgp");
        verify(sftpService).closeSession();
    }

    @Test
    void write_throwsBatchWriteException_whenWriterWasNotOpened() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        doThrow(new SftpException("not open")).when(sftpService).setInputStream("line");

        Iso8583MessageWriter writer = new Iso8583MessageWriter(sftpService);

        BatchWriteException exception = assertThrows(BatchWriteException.class, () ->
                writer.write(new Chunk<>(List.of("line"))));

        assertTrue(exception.getMessage().contains("Error writing to SFTP file"));
    }

    @Test
    void open_throwsRuntimeException_whenSessionCannotBeCreated() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        doThrow(new SftpException("open boom")).when(sftpService).openSftpSession();

        Iso8583MessageWriter writer = new Iso8583MessageWriter(sftpService);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> writer.open(new ExecutionContext()));
        assertTrue(exception.getMessage().contains("Error opening SFTP session"));
    }

    @Test
    void close_throwsRuntimeException_whenUploadFails() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        doThrow(new SftpException("upload boom")).when(sftpService).write("output.csv.pgp");

        Iso8583MessageWriter writer = new Iso8583MessageWriter(sftpService);
        writer.open(new ExecutionContext());

        RuntimeException exception = assertThrows(RuntimeException.class, writer::close);
        assertInstanceOf(SftpException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("upload boom"));
    }
}
