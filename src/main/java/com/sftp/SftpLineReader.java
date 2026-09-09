package com.sftp;

import com.exception.SftpException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@StepScope
public class SftpLineReader extends AbstractItemCountingItemStreamItemReader<String> {
    private final DefaultSftpSessionFactory sessionFactory;
    private BufferedReader reader;

    public SftpLineReader(DefaultSftpSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        setName("sftpLineReader");
    }

    @Override
    protected void doOpen() throws SftpException {
        try {

            SftpSession session = sessionFactory.getSession();
            Path tempFile = Files.createTempFile("sftp-", ".csv");
            try (OutputStream os = Files.newOutputStream(tempFile)) {
                session.read("upload/data.csv", os);
            }
            reader = Files.newBufferedReader(tempFile);
        } catch (Exception e) {
            throw new SftpException("Error occurred while opening SFTP connection:" + e.getMessage(), e);
        }
    }

    @Override
    protected String doRead() throws SftpException {
        try {
            if (reader == null) {
                return null;
            }
            return reader.readLine();
        } catch (Exception e) {
            throw new SftpException("Error occurred while reading from SFTP file:" + e.getMessage(), e);
        }
    }


    @Override
    protected void doClose() throws SftpException {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception e) {
                throw new SftpException("Error occurred while closing SFTP file reader:" + e.getMessage(), e);
            }
        }
    }
}
