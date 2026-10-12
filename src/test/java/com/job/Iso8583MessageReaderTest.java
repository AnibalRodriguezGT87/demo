package com.job;

import com.exception.BatchReadException;
import com.orchestrator.FileOrchestrator;
import com.orchestrator.FileOrchestratorException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Iso8583MessageReaderTest {

    @Test
    void read_returnsNextBufferedReaderFromOrchestrator() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        BufferedReader firstReader = new BufferedReader(new StringReader("first line\nsecond line\n"));
        when(orchestrator.getNextFile()).thenReturn(firstReader, null);

        Iso8583MessageReader reader = new Iso8583MessageReader(orchestrator);
        reader.open(new ExecutionContext());

        assertSame(firstReader, reader.read());
        assertNull(reader.read());
        reader.close();

        verify(orchestrator).openSFTConnection();
        verify(orchestrator).setListFiles();
        verify(orchestrator).closeSFTConnection();
    }

    @Test
    void open_throwsItemStreamException_whenSessionCannotBeCreated() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        doThrow(new FileOrchestratorException("Error occurred while opening SFTP connection:boom",
                new RuntimeException("open cause")))
                .when(orchestrator).openSFTConnection();

        Iso8583MessageReader reader = new Iso8583MessageReader(orchestrator);

        ItemStreamException exception = assertThrows(ItemStreamException.class, () -> reader.open(new ExecutionContext()));
        assertInstanceOf(FileOrchestratorException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Error occurred while opening SFTP connection"));
    }

    @Test
    void read_throwsBatchReadException_whenReadFails() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        when(orchestrator.getNextFile()).thenThrow(new RuntimeException("read boom"));

        Iso8583MessageReader reader = new Iso8583MessageReader(orchestrator);
        reader.open(new ExecutionContext());

        BatchReadException exception = assertThrows(BatchReadException.class, reader::read);
        assertTrue(exception.getMessage().contains("Error occurred while reading from SFTP file"));
    }

    @Test
    void close_throwsItemStreamException_whenCloseFails() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        doThrow(new FileOrchestratorException("close boom", new RuntimeException("close cause")))
                .when(orchestrator).closeSFTConnection();

        Iso8583MessageReader reader = new Iso8583MessageReader(orchestrator);
        reader.open(new ExecutionContext());

        ItemStreamException exception = assertThrows(ItemStreamException.class, reader::close);
        assertInstanceOf(FileOrchestratorException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("Error occurred while closing SFTP file reader"));
    }
}
