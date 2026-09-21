package com.sftp;

import com.exception.SftpException;
import com.pgp.PGPProperties;
import com.pgp.PgpService;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;

/**
 * Service class for handling SFTP operations, including reading and writing files,
 * as well as decrypting PGP-encrypted files.
 */
@Service
public class SftpService {

    private final DefaultSftpSessionFactory sessionFactory;
    private final PGPProperties pgpProperties;
    private final SftpProperties properties;
    private final PgpService pgpService;
    private BufferedReader reader;
    private ByteArrayOutputStream outputStream;
    private SftpSession session;

    public SftpService(DefaultSftpSessionFactory sessionFactory,
                       PGPProperties pgpProperties,
                       SftpProperties properties,
                       PgpService pgpService) {
        this.sessionFactory = sessionFactory;
        this.pgpProperties = pgpProperties;
        this.properties = properties;
        this.pgpService = pgpService;
    }

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
     * Reads a PGP-encrypted file from the SFTP server, decrypts it, and stores it in a temporary file.
     *
     * @param inputFile the path to the input file on the SFTP server
     * @throws SftpException if an error occurs while reading or decrypting the file
     */
    public void readDecryptedFile(String inputFile) throws SftpException {
        try {
            Path encryptedFile = Files.createTempFile("sftp-", properties.getEncryptedFileExtension());
            try (OutputStream os = Files.newOutputStream(encryptedFile)) {
                session.read(inputFile, os);
            }

            InputStream encryptedInput = Files.newInputStream(encryptedFile);
            InputStream privateKey = new ClassPathResource(pgpProperties.getPrivateKey()).getInputStream();
            InputStream decryptedInput = pgpService.decrypt(encryptedInput, privateKey, pgpProperties.getPassphrase());

            Path decryptedFile = Files.createTempFile("sftp-", properties.getTempFileExtension());
            Files.copy(decryptedInput, decryptedFile, StandardCopyOption.REPLACE_EXISTING);

            reader = Files.newBufferedReader(decryptedFile);
        } catch (Exception e) {
            throw new SftpException("Error occurred reading decrypted file:" + e.getMessage(), e);
        }
    }

    /**
     * Reads a file from the SFTP server and stores it in a temporary file.
     *
     * @param inputFile the path to the input file on the SFTP server
     * @throws SftpException if an error occurs while reading the file
     */
    public void readFile(String inputFile) throws SftpException {
        try {
            Path tempFile = Files.createTempFile("sftp-", properties.getTempFileExtension());
            try (OutputStream os = Files.newOutputStream(tempFile)) {
                session.read(inputFile, os);
            }
            reader = Files.newBufferedReader(tempFile);
        } catch (Exception e) {
            throw new SftpException("Error occurred reading file:" + e.getMessage(), e);
        }
    }

    /**
     * Reads the first PGP-encrypted file from the specified remote directory, decrypts it, and stores it in a temporary file.
     *
     * @throws SftpException if an error occurs while reading or decrypting the file
     */
    public void  readFirstDecryptedFile(String remoteDirectory) throws SftpException {
        try {
            final String encryptedExtension = Optional.ofNullable(properties.getEncryptedFileExtension())
                    .filter(ext -> !ext.isBlank())
                    .orElse(".gpg");

            String fileName = Arrays.stream(session.list(remoteDirectory))
                    .map(SftpClient.DirEntry::getFilename)
                    .filter(name -> name != null && (name.endsWith(encryptedExtension)
                            || name.endsWith(".gpg")
                            || name.endsWith(".pgp")))
                    .findFirst()
                    .orElseThrow();
            readDecryptedFile(remoteDirectory + "/" + fileName);
        } catch (Exception e) {
            throw new SftpException("Error occurred while listing files in remote directory:" + e.getMessage(), e);
        }
    }

    /**
     * Reads the first file from the specified remote directory and stores it in a temporary file.
     *
     * @param remoteDirectory the path to the remote directory
     * @throws SftpException if an error occurs while reading the file
     */
    public void readFirstFile(String remoteDirectory) throws SftpException {
        try {
            String fileName = Arrays.stream(session.list(remoteDirectory))
                    .map(SftpClient.DirEntry::getFilename)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst()
                    .orElseThrow();
            readFile(remoteDirectory + "/" + fileName);
        } catch (Exception e) {
            throw new SftpException("Error occurred while listing files in remote directory:" + e.getMessage(), e);
        }
    }

    /**
     * Initializes the output stream for writing data to the SFTP server.
     */
    public void setOutputStream() {
        outputStream = new ByteArrayOutputStream();
    }

    /**
     * Writes the provided binary data to the output stream.
     *
     * @param binaryData the binary data to write
     * @throws SftpException if an error occurs while writing to the output stream
     */
    public void setInputStream(String binaryData) throws SftpException {
        try {
            outputStream.write(binaryData.getBytes());
            outputStream.write('\n');
        } catch (Exception e) {
            throw new SftpException("Error writing to SFTP file", e);
        }
    }

    /**
     * Closes the output stream and writes its contents to the specified remote directory and file on the SFTP server.
     *
     * @param remoteDirectory the path to the remote directory
     * @param fileName        the name of the file to write to
     * @throws SftpException if an error occurs while closing the output stream or writing to the SFTP server
     */
    public void writeSftpFile(String remoteDirectory, String fileName) throws SftpException {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(outputStream.toByteArray());
            session.write(in, remoteDirectory + "/" + fileName);
        } catch (Exception e) {
            throw new SftpException("Error closing SFTP writer", e);
        }
    }

    /**
     * Returns the currently opened BufferedReader for reading from the SFTP file.
     *
     * @return the BufferedReader for reading from the SFTP file
     */
    public BufferedReader getBufferedReader() {
        return reader;
    }


    /**
     * Reads a line from the currently opened SFTP file reader.
     *
     * @return a line from the reader, or null if the end of the file is reached
     * @throws SftpException if an error occurs while reading a line
     */
    public String getRowLine() throws SftpException {
        try {
            if (reader == null) {
                return null;
            }
            return reader.readLine();
        } catch (Exception e) {
            throw new SftpException("Error occurred while reading a line from the reader:" + e.getMessage(), e);
        }
    }
    /**
     * Closes the currently opened SFTP file reader.
     *
     * @throws SftpException if an error occurs while closing the reader
     */
    public void closeReader() throws SftpException {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (Exception e) {
            throw new SftpException("Error occurred while closing the reader:" + e.getMessage(), e);
        }
    }

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
