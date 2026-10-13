package com.job;

import com.exception.BatchReadException;
import com.orchestrator.FileOrchestrator;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.util.Iterator;

/**
 * Iso8583MessageReader is a Spring Batch ItemReader that reads ISO 8583 messages from an SFTP server.
 * It uses the SftpService to establish a connection, read a decrypted file, and retrieve lines of data.
 * The reader is step-scoped, meaning it is created and managed within the context of a specific step execution.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class Iso8583MessageReader implements ItemStreamReader<BufferedReader> {

    private final FileOrchestrator fileOrchestrator;
    private Iterator<String> readers;
    /**
     * Opens the SFTP connection and prepares to read the decrypted file.
     *
     */
    @Override
    public void open(@Nonnull ExecutionContext executionContext) throws ItemStreamException {
        try {
            readers = fileOrchestrator.getListFiles().iterator();
        } catch (Exception e) {
            throw new ItemStreamException("Error occurred while opening SFTP connection:" + e.getMessage(), e);
        }
    }

    /**
     * Reads a line from the decrypted SFTP file.
     *
     * @return the next line of data from the file
     * @throws ItemStreamException if an error occurs while reading from the SFTP file
     */
    @Override
    public BufferedReader read() throws BatchReadException {
        try {
            if (readers.hasNext()) {
                return fileOrchestrator.getFileReader(readers.next());
            }
            return null;
        } catch (Exception e) {
            throw new BatchReadException("Error occurred while reading from SFTP file:" + e.getMessage(), e);
        }
    }
}
