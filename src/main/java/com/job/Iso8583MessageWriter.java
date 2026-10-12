package com.job;

import com.exception.BatchWriteException;
import com.orchestrator.FileOrchestrator;
import com.orchestrator.FileOrchestratorException;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Iso8583MessageWriter is a Spring Batch ItemStreamWriter that writes ISO 8583 messages to an SFTP server.
 * It uses the SftpService to establish a connection, write data to a file, and manage the SFTP session.
 * The writer is step-scoped, meaning it is created and managed within the context of a specific step execution.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class Iso8583MessageWriter implements ItemStreamWriter<String> {
    private final FileOrchestrator fileOrchestrator;

    /**
     * Opens the SFTP connection and prepares to write data to the output file.
     *
     * @param executionContext the execution context for the current step
     */
    @Override
    public void open(@Nonnull ExecutionContext executionContext)  {
        try {
            fileOrchestrator.openSFTConnection();
        } catch (Exception e) {
            throw new RuntimeException("Error opening SFTP session", e);
        }
    }

    /**
     * Writes a chunk of ISO 8583 messages to the SFTP file.
     *
     * @param chunk the chunk of items to be written
     * @throws BatchWriteException if an error occurs while writing to the SFTP file
     */
    @Override
    public void write(@Nonnull Chunk<? extends String> chunk) throws BatchWriteException {
        try {
            for (String line : chunk.getItems()) {
                String fileName = "output"+new Date()+".csv.pgp";
                fileOrchestrator.write(fileName, line);
            }
        } catch (Exception e) {
            throw new BatchWriteException("Error writing to SFTP file", e);
        }
    }

    /**
     * Closes the SFTP connection and the file writer.
     */
    @Override
    public void close() {
        try {
            fileOrchestrator.closeSFTConnection();
        } catch (FileOrchestratorException e) {
            throw new RuntimeException(e);
        }
    }
}