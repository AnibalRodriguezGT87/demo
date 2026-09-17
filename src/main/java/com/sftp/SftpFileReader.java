package com.sftp;

import com.exception.BatchReadException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class SftpFileReader extends AbstractItemCountingItemStreamItemReader<String> {

    private final SftpService sftpService;

    public SftpFileReader(SftpService sftpService) {
        this.sftpService = sftpService;
        setName("sftpLineReader");
    }

    @Override
    protected void doOpen() throws BatchReadException {
        try {
            String inputFile = "upload/data.csv.gpg";
            sftpService.openSftpSession();
            sftpService.readDecryptedFile(inputFile);
        } catch (Exception e) {
            throw new BatchReadException("Error occurred while opening SFTP connection:" + e.getMessage(), e);
        }
    }

    @Override
    protected String doRead() throws BatchReadException {
        try {
            return sftpService.getRowLine();
        } catch (Exception e) {
            throw new BatchReadException("Error occurred while reading from SFTP file:" + e.getMessage(), e);
        }
    }


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
