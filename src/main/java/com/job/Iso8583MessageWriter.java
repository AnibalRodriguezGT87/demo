package com.job;

import com.exception.BatchWriteException;
import com.exception.SftpException;
import com.sftp.SftpService;
import jakarta.annotation.Nonnull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.stereotype.Component;

/**
 * Iso8583MessageWriter is a Spring Batch ItemStreamWriter that writes ISO 8583 messages to an SFTP server.
 * It uses the SftpService to establish a connection, write data to a file, and manage the SFTP session.
 * The writer is step-scoped, meaning it is created and managed within the context of a specific step execution.
 */
@Component
@StepScope
public class Iso8583MessageWriter implements ItemStreamWriter<String> {

    private final SftpService sftpService;

    public Iso8583MessageWriter(SftpService sftpService) {
        this.sftpService = sftpService;
    }

    /**
     * Opens the SFTP connection and prepares to write data to the output file.
     *
     * @param executionContext the execution context for the current step
     */
    @Override
    public void open(@Nonnull ExecutionContext executionContext)  {
        try {
            sftpService.openSftpSession();
            sftpService.setOutputStream();
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
                sftpService.setInputStream(line);
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
            String fileName = "output.csv";
            sftpService.writeSftpFile(fileName);
            sftpService.closeSession();
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
    }
}