package com.sftp;

import com.exception.BatchReadException;
import com.exception.SftpException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SftpFileReaderTest {

    @Test
    void read_readsLinesFromRemoteFile() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        when(sftpService.getRowLine()).thenReturn("first line", "second line", null);

        SftpFileReader reader = new SftpFileReader(sftpService);
        reader.open(new ExecutionContext());

        assertEquals("first line", reader.read());
        assertEquals("second line", reader.read());
        assertNull(reader.read());

        reader.close();

        verify(sftpService).openSftpSession();
        verify(sftpService).readFile("upload/data.csv");
        verify(sftpService, times(3)).getRowLine();
        verify(sftpService).closeReader();
        verify(sftpService).closeSession();
    }

    @Test
    void open_throwsItemStreamException_whenSessionCannotBeCreated() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        doThrow(new SftpException("Error occurred while opening SFTP connection:boom"))
                .when(sftpService).openSftpSession();

        SftpFileReader reader = new SftpFileReader(sftpService);

        ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(new ExecutionContext()));
        assertTrue(exception.getCause() instanceof BatchReadException);
        assertTrue(exception.getCause().getMessage().contains("Error occurred while opening SFTP connection"));
    }

    @Test
    void read_throwsItemStreamException_whenReadFails() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        when(sftpService.getRowLine()).thenThrow(new SftpException("read boom"));

        SftpFileReader reader = new SftpFileReader(sftpService);
        reader.open(new ExecutionContext());

        BatchReadException exception = assertThrows(BatchReadException.class, reader::read);
        assertTrue(exception.getMessage().contains("Error occurred while reading from SFTP file"));
    }

    @Test
    void close_throwsItemStreamException_whenCloseFails() throws Exception {
        SftpService sftpService = mock(SftpService.class);
        doThrow(new SftpException("close boom")).when(sftpService).closeReader();

        SftpFileReader reader = new SftpFileReader(sftpService);
        reader.open(new ExecutionContext());

        ItemStreamException exception = assertThrows(ItemStreamException.class, reader::close);
        assertTrue(exception.getCause() instanceof BatchReadException);
        assertTrue(exception.getCause().getMessage().contains("Error occurred while closing SFTP file reader"));
    }
}
