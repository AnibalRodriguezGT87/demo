package com.sftp;

import com.exception.SftpException;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Service class for handling SFTP operations, including reading and writing files,
 * as well as decrypting PGP-encrypted files.
 */
@Service
@RequiredArgsConstructor
public class SftpService {

    private final DefaultSftpSessionFactory sessionFactory;
    private final SftpProperties sftpProperties;
    private SftpSession session;

    /**
     * Opens an SFTP session using the provided session factory.
     *
     * @throws SftpException if an error occurs while opening the SFTP connection
     */
    public void openSftpSession() throws SftpException {
        try {
            session = sessionFactory.getSession();
        } catch (Exception e) {
            throw new SftpException("Error occurred while opening SFTP connection:" + e.getMessage(), e);
        }
    }

    /**
     * Reads a file from the SFTP server and returns an InputStream for the file's contents.
     * The file is temporarily stored on the local filesystem before being returned as an InputStream.
     *
     * @param inputFile the path to the file on the SFTP server
     * @return an InputStream for reading the file's contents
     * @throws SftpException if an error occurs while reading the file
     */
    public InputStream readFile(String inputFile) throws SftpException {
        try {
            Path tempFile = Files.createTempFile("sftp-", sftpProperties.getTempFileExtension());
            String remoteFilePath = sftpProperties.getRemoteDirectoryInput() + "/" + inputFile;
            try (OutputStream os = Files.newOutputStream(tempFile)) {
                session.read(remoteFilePath, os);
            }
            return Files.newInputStream(tempFile);
        } catch (Exception e) {
            throw new SftpException("Error occurred reading decrypted file:" + e.getMessage(), e);
        }
    }

    /**
     * Lists the files in the remote input directory that match the configured file extensions.
     *
     * @return an Iterable of file names in the remote input directory
     * @throws SftpException if an error occurs while listing files in the remote directory
     */
    public Iterator<String> listFiles() throws SftpException {
        try {
            return Arrays.stream(session.list(sftpProperties.getRemoteDirectoryInput()))
                    .map(SftpClient.DirEntry::getFilename)
                    .filter(name -> name != null && !name.isBlank()
                            && Arrays.stream(sftpProperties.getFileExtension().split(","))
                            .anyMatch(name::endsWith))
                    .iterator();
        } catch (Exception e) {
            throw new SftpException("Error occurred while listing files in remote directory:" + e.getMessage(), e);
        }
    }

    /**
     * Writes the contents of the provided ByteArrayOutputStream to the specified remote directory and file on the SFTP server.
     *
     * @param data the string containing the data to write
     * @param fileName     the name of the file to write to
     * @throws SftpException if an error occurs while writing to the SFTP server
     */
    public void write(String data, String fileName) throws SftpException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(data.getBytes());
            ByteArrayInputStream in = new ByteArrayInputStream(outputStream.toByteArray());
            session.write(in, sftpProperties.getRemoteDirectoryOutput() + "/" + fileName);
            outputStream.close();
            in.close();
        } catch (Exception e) {
            throw new SftpException("Error closing SFTP writer", e);
        }
    }

    /**
     * Writes the provided InputStream to the specified remote directory and file on the SFTP server.
     *
     * @param outputStream the InputStream containing the data to write
     * @param filePath     the path of the file to write to
     * @throws SftpException if an error occurs while writing to the SFTP server
     */
    public void write(InputStream outputStream, String filePath) throws SftpException {
        try {
            session.write(outputStream, filePath);
        } catch (Exception e) {
            throw new SftpException("Error writing SFTP writer", e);
        }
    }

    /**
     * Closes the currently opened SFTP session.
     *
     * @throws SftpException if an error occurs while closing the session
     */
    public void closeSession() throws SftpException {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            throw new SftpException("Error occurred while closing the sftp session:" + e.getMessage(), e);
        }
    }
}
