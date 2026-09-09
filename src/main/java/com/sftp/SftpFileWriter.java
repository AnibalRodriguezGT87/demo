package com.sftp;

import com.exception.SftpException;
import jakarta.annotation.Nonnull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Component
@StepScope
public class SftpFileWriter implements ItemStreamWriter<String> {

    private final DefaultSftpSessionFactory sessionFactory;
    private ByteArrayOutputStream outputStream;
    private final SftpProperties properties;
    private SftpSession session;
    @Value("#{jobParameters['remoteDirectory']}")
    private String remoteDirectory;
    @Value("#{jobParameters['fileNameOutput']}")
    private String fileName;

    public SftpFileWriter(DefaultSftpSessionFactory sessionFactory, SftpProperties properties) {
        this.sessionFactory = sessionFactory;
        this.properties = properties;
    }

    @Override
    public void open(@Nonnull ExecutionContext executionContext)  {
        try {
            session = sessionFactory.getSession();
            outputStream = new ByteArrayOutputStream();
        } catch (Exception e) {
            throw new RuntimeException("Error opening SFTP session", e);
        }
    }

    @Override
    public void write(@Nonnull Chunk<? extends String> chunk) throws SftpException {
        try {
            for (String line : chunk.getItems()) {
                outputStream.write(line.getBytes());
                outputStream.write('\n');
            }
        } catch (Exception e) {
            throw new SftpException("Error writing to SFTP file", e);
        }
    }

    @Override
    public void close() {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(outputStream.toByteArray());
            //TODO properties.getRemoteInputFile() should be replaced with remoteDirectory + "/" +
            // fileName if it is intended to write to a specific file in the remote directory
            session.write(in, remoteDirectory + "/" + fileName);
        } catch (Exception e) {
            throw new RuntimeException("Error closing SFTP writer", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}