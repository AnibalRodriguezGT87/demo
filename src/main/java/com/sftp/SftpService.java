package com.sftp;

import com.crypto.PGPProperties;
import com.crypto.PgpService;
import com.exception.SftpException;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/**
 * Service class for handling SFTP operations, including reading and writing files,
 * as well as decrypting PGP-encrypted files.
 */
@Service
@RequiredArgsConstructor
public class SftpService {

    private final SftpProperties sftpProperties;
    private final PGPProperties pgpProperties;
    private final SftpTemplate sftpTemplate;
    private final PgpService pgpService;

    /**
     * Reads a file from the SFTP server and returns a BufferedReader for the file's contents.
     * The file is temporarily stored on the local filesystem before being returned as a BufferedReader.
     *
     * @param inputFile  the name of the file to read
     * @return a BufferedReader for reading the file's contents
     */
    public BufferedReader readFile(String inputFile) throws IOException {
        return sftpTemplate.execute(session -> {
            Path tempFile = Files.createTempFile("sftp-", sftpProperties.getTempFileExtension());
            String remoteFilePath = sftpProperties.getRemoteDirectoryInput() + "/" + inputFile;
            try (OutputStream os = Files.newOutputStream(tempFile)) {
                session.read(remoteFilePath, os);
            }

            if (pgpProperties.isEnabled()) {
                InputStream decryptedInput = pgpService.decrypt(Files.newInputStream(tempFile));
                Path decryptedFile = Files.createTempFile("sftp-", sftpProperties.getTempFileExtension());
                Files.copy(decryptedInput, decryptedFile, StandardCopyOption.REPLACE_EXISTING);
                return Files.newBufferedReader(decryptedFile);
            }

            return Files.newBufferedReader(tempFile);
        });
    }

    /**
     * Lists the files in the remote input directory that match the configured file extensions.
     *
     * @return a List of file names in the remote input directory
     * @throws IOException if an error occurs while listing files in the remote directory
     */
    public List<String> listRemoteFiles() throws IOException {
        return sftpTemplate.execute(session ->
                Arrays.stream(session.list(sftpProperties.getRemoteDirectoryInput()))
                .map(SftpClient.DirEntry::getFilename)
                .filter(name -> name != null && !name.isBlank()
                        && Arrays.stream(sftpProperties.getFileExtension().split(","))
                        .anyMatch(name::endsWith))
                .toList());
    }

    /**
     * Writes data to a file on the SFTP server. If PGP encryption is enabled, the data will be encrypted before writing.
     *
     * @param data      the data to write to the file
     * @param filePath  the path of the file on the SFTP server
     * @throws SftpException if an error occurs during file writing or encryption
     * @throws IOException   if an I/O error occurs
     */
    public void write(String data, String filePath) throws SftpException, IOException {
        sftpTemplate.execute(session -> {
            try {
                if (pgpProperties.isEnabled()) {
                    InputStream encryptedFile = pgpService.encrypt(data);
                    session.write(encryptedFile, filePath);
                    return null;
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(data.getBytes());
                ByteArrayInputStream in = new ByteArrayInputStream(outputStream.toByteArray());
                session.write(in, filePath);
                outputStream.close();
                in.close();
                return null;
            } catch (Exception e) {
                try {
                    throw new SftpException("Error occurred while writing file: " + e.getMessage(), e);
                } catch (SftpException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
