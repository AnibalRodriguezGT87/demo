package com.job;

import com.exception.BatchReadException;
import com.orchestrator.FileOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Iso8583MessageReaderTest {

    @Test
    void read_returnsNextBufferedReaderFromOrchestrator() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        BufferedReader firstReader = new BufferedReader(new StringReader("first line\nsecond line\n"));
        BufferedReader secondReader = new BufferedReader(new StringReader("third line\n"));
        when(orchestrator.getListFiles()).thenReturn(List.of("first.csv", "second.csv"));
        when(orchestrator.getFileReader("first.csv")).thenReturn(firstReader);
        when(orchestrator.getFileReader("second.csv")).thenReturn(secondReader);

        Iso8583MessageReader reader = new Iso8583MessageReader(orchestrator);
        reader.open(new ExecutionContext());

        assertSame(firstReader, reader.read());
        assertSame(secondReader, reader.read());
        assertNull(reader.read());
    }

    @Test
    void open_throwsItemStreamException_whenFileListingFails() {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        doThrow(new RuntimeException("open boom")).when(orchestrator).getListFiles();

        Iso8583MessageReader reader = new Iso8583MessageReader(orchestrator);

        ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(new ExecutionContext()));
        assertTrue(exception.getMessage().contains("Error occurred while opening SFTP connection"));
        assertInstanceOf(RuntimeException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("open boom"));
    }

    @Test
    void read_throwsBatchReadException_whenReadFails() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        when(orchestrator.getListFiles()).thenReturn(List.of("first.csv"));
        when(orchestrator.getFileReader("first.csv")).thenThrow(new RuntimeException("read boom"));

        Iso8583MessageReader reader = new Iso8583MessageReader(orchestrator);
        reader.open(new ExecutionContext());

        BatchReadException exception = assertThrows(BatchReadException.class, reader::read);
        assertTrue(exception.getMessage().contains("Error occurred while reading from SFTP file"));
    }
}
