package com.sftp;

import com.exception.BatchWriteException;
import com.exception.SftpException;
import jakarta.annotation.Nonnull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class SftpFileWriter implements ItemStreamWriter<String> {

    private final SftpService sftpService;

    public SftpFileWriter(SftpService sftpService) {
        this.sftpService = sftpService;
    }

    @Override
    public void open(@Nonnull ExecutionContext executionContext)  {
        try {
            sftpService.openSftpSession();
            sftpService.setOutputStream();
        } catch (Exception e) {
            throw new RuntimeException("Error opening SFTP session", e);
        }
    }

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

    @Override
    public void close() {
        try {
            String remoteDirectory = "upload";
            String fileName = "output.csv";
            sftpService.writeSftpFile(remoteDirectory, fileName);
            sftpService.closeSession();
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
    }
}