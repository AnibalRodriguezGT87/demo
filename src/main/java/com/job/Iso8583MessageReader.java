package com.job;

import com.exception.BatchReadException;
import com.sftp.SftpService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

/**
 * Iso8583MessageReader is a Spring Batch ItemReader that reads ISO 8583 messages from an SFTP server.
 * It uses the SftpService to establish a connection, read a decrypted file, and retrieve lines of data.
 * The reader is step-scoped, meaning it is created and managed within the context of a specific step execution.
 */
@Component
@StepScope
public class Iso8583MessageReader extends AbstractItemCountingItemStreamItemReader<String> {

    private final SftpService sftpService;

    public Iso8583MessageReader(SftpService sftpService) {
        this.sftpService = sftpService;
        setName("sftpLineReader");
    }

    /**
     * Opens the SFTP connection and prepares to read the decrypted file.
     *
     * @throws BatchReadException if an error occurs while opening the SFTP connection or reading the file
     */
    @Override
    protected void doOpen() throws BatchReadException {
        try {
            sftpService.openSftpSession();
            sftpService.readFirstEncryptedFile();
        } catch (Exception e) {
            throw new BatchReadException("Error occurred while opening SFTP connection:" + e.getMessage(), e);
        }
    }

    /**
     * Reads a line from the decrypted SFTP file.
     *
     * @return the next line of data from the file
     * @throws BatchReadException if an error occurs while reading from the SFTP file
     */
    @Override
    protected String doRead() throws BatchReadException {
        try {
            return sftpService.getRowLine();
        } catch (Exception e) {
            throw new BatchReadException("Error occurred while reading from SFTP file:" + e.getMessage(), e);
        }
    }

    /**
     * Closes the SFTP connection and the file reader.
     *
     * @throws BatchReadException if an error occurs while closing the SFTP connection or file reader
     */
    @Override
    protected void doClose() throws BatchReadException {
        try {
            sftpService.closeReader();
            sftpService.closeSession();
        } catch (Exception e) {
            throw new BatchReadException("Error occurred while closing SFTP file reader:" + e.getMessage(), e);
        }
    }
}
