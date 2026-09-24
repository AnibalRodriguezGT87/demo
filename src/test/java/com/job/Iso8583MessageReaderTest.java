package com.job;

import com.exception.BatchReadException;
import com.exception.SftpException;
import com.sftp.SftpService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Iso8583MessageReaderTest {

    @Test
    void read_readsLinesFromRemoteFile() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        when(sftpService.getRowLine()).thenReturn("first line", "second line", null);

        Iso8583MessageReader reader = new Iso8583MessageReader(sftpService);
        reader.open(new ExecutionContext());

        assertEquals("first line", reader.read());
        assertEquals("second line", reader.read());
        assertNull(reader.read());

        reader.close();

        verify(sftpService).openSftpSession();
        verify(sftpService).readFirstEncryptedFile();
        verify(sftpService, times(3)).getRowLine();
        verify(sftpService).closeReader();
        verify(sftpService).closeSession();
    }

    @Test
    void open_throwsItemStreamException_whenSessionCannotBeCreated() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        doThrow(new SftpException("Error occurred while opening SFTP connection:boom"))
                .when(sftpService).openSftpSession();

        Iso8583MessageReader reader = new Iso8583MessageReader(sftpService);

        ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(new ExecutionContext()));
        assertInstanceOf(BatchReadException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Error occurred while opening SFTP connection"));
    }

    @Test
    void read_throwsItemStreamException_whenReadFails() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        when(sftpService.getRowLine()).thenThrow(new SftpException("read boom"));

        Iso8583MessageReader reader = new Iso8583MessageReader(sftpService);
        reader.open(new ExecutionContext());

        BatchReadException exception = assertThrows(BatchReadException.class, reader::read);
        assertTrue(exception.getMessage().contains("Error occurred while reading from SFTP file"));
    }

    @Test
    void close_throwsItemStreamException_whenCloseFails() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        doThrow(new SftpException("close boom")).when(sftpService).closeReader();

        Iso8583MessageReader reader = new Iso8583MessageReader(sftpService);
        reader.open(new ExecutionContext());

        ItemStreamException exception = assertThrows(ItemStreamException.class, reader::close);
        assertInstanceOf(BatchReadException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Error occurred while closing SFTP file reader"));
    }
}
