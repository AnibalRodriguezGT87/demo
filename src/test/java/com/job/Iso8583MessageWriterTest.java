package com.job;

import com.exception.BatchWriteException;
import com.orchestrator.FileOrchestrator;
import com.orchestrator.FileOrchestratorException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.infrastructure.item.Chunk;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class Iso8583MessageWriterTest {

    @Test
    void write_uploadsEachChunkItemToSftpWithGeneratedFileName() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        Iso8583MessageWriter writer = new Iso8583MessageWriter(orchestrator);

        writer.write(new Chunk<>(List.of("first", "second")));

        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(orchestrator, times(2)).write(fileNameCaptor.capture(), dataCaptor.capture());

        assertEquals(List.of("first", "second"), dataCaptor.getAllValues());
        assertEquals(2, fileNameCaptor.getAllValues().size());
        assertTrue(fileNameCaptor.getAllValues().getFirst().startsWith("output"));
        assertTrue(fileNameCaptor.getAllValues().getFirst().endsWith(".csv.pgp"));
    }

    @Test
    void write_throwsBatchWriteException_whenWriterFails() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        doThrow(new FileOrchestratorException("write boom", new RuntimeException("write cause")))
                .when(orchestrator).write(anyString(), eq("line"));

        Iso8583MessageWriter writer = new Iso8583MessageWriter(orchestrator);

        BatchWriteException exception = assertThrows(BatchWriteException.class, () ->
                writer.write(new Chunk<>(List.of("line"))));

        assertTrue(exception.getMessage().contains("Error writing to SFTP file"));
        assertInstanceOf(FileOrchestratorException.class, exception.getCause());
    }
}
