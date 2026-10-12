package com.job;

import com.exception.BatchWriteException;
import com.orchestrator.FileOrchestrator;
import com.orchestrator.FileOrchestratorException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class Iso8583MessageWriterTest {

    @Test
    void write_and_close_uploadFileToSftp() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        Iso8583MessageWriter writer = new Iso8583MessageWriter(orchestrator);

        writer.open(new ExecutionContext());
        writer.write(new Chunk<>(List.of("first", "second")));
        writer.close();

        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(orchestrator, times(2)).write(fileNameCaptor.capture(), dataCaptor.capture());

        verify(orchestrator).openSFTConnection();
        verify(orchestrator).closeSFTConnection();
        assertEquals(List.of("first", "second"), dataCaptor.getAllValues());
        assertEquals(2, fileNameCaptor.getAllValues().size());
        assertTrue(fileNameCaptor.getAllValues().getFirst().startsWith("output"));
        assertTrue(fileNameCaptor.getAllValues().getFirst().endsWith(".csv.pgp"));
    }

    @Test
    void write_throwsBatchWriteException_whenWriterFails() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        doThrow(new RuntimeException("not open")).when(orchestrator).write(anyString(), eq("line"));

        Iso8583MessageWriter writer = new Iso8583MessageWriter(orchestrator);

        BatchWriteException exception = assertThrows(BatchWriteException.class, () ->
                writer.write(new Chunk<>(List.of("line"))));

        assertTrue(exception.getMessage().contains("Error writing to SFTP file"));
    }

    @Test
    void open_throwsRuntimeException_whenSessionCannotBeCreated() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        doThrow(new FileOrchestratorException("open boom", new RuntimeException("open cause")))
                .when(orchestrator).openSFTConnection();

        Iso8583MessageWriter writer = new Iso8583MessageWriter(orchestrator);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> writer.open(new ExecutionContext()));
        assertTrue(exception.getMessage().contains("Error opening SFTP session"));
        assertInstanceOf(FileOrchestratorException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("open boom"));
    }

    @Test
    void close_throwsRuntimeException_whenUploadFails() throws Exception {
        FileOrchestrator orchestrator = mock(FileOrchestrator.class);
        doThrow(new FileOrchestratorException("upload boom", new RuntimeException("close cause")))
                .when(orchestrator).closeSFTConnection();

        Iso8583MessageWriter writer = new Iso8583MessageWriter(orchestrator);
        writer.open(new ExecutionContext());

        RuntimeException exception = assertThrows(RuntimeException.class, writer::close);
        assertInstanceOf(FileOrchestratorException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("upload boom"));
    }
}
