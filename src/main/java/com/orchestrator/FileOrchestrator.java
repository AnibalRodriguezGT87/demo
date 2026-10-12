package com.orchestrator;

import com.exception.SftpException;
import com.crypto.PGPProperties;
import com.crypto.PgpService;
import com.sftp.SftpProperties;
import com.sftp.SftpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

@Service
@RequiredArgsConstructor
public class FileOrchestrator {

    private final SftpService sftpService;
    private final PgpService pgpService;
    private final PGPProperties pgpProperties;
    private final SftpProperties sftpProperties;
    private Iterator<String> fileIterator;

    /**
     * Opens an SFTP connection using the SftpService.
     *
     * @throws FileOrchestratorException if an error occurs while opening the SFTP connection
     */
    public void openSFTConnection() throws FileOrchestratorException {
        try {
            sftpService.openSftpSession();
        } catch (SftpException e) {
            throw new FileOrchestratorException("Error occurred while opening SFTP connection: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a list of files from the SFTP server and initializes the file iterator.
     *
     * @throws FileOrchestratorException if an error occurs while listing files on the SFTP server
     */
    public void setListFiles() throws FileOrchestratorException {
        try {
            fileIterator = sftpService.listFiles();
        } catch (SftpException e) {
            throw new FileOrchestratorException("Error occurred while listing files: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the SFTP connection using the SftpService.
     *
     * @throws FileOrchestratorException if an error occurs while closing the SFTP connection
     */
    public void closeSFTConnection() throws FileOrchestratorException {
        try {
            sftpService.closeSession();
        } catch (SftpException e) {
            throw new FileOrchestratorException("Error occurred while closing SFTP connection: " + e.getMessage(), e);
        }
    }

    /**
     * Returns an iterator for the list of files retrieved from the SFTP server.
     *
     * @return an Iterator<String> for iterating over the list of files
     */
    public Iterator<String> getListFiles() throws FileOrchestratorException {
        try {
            fileIterator = sftpService.listFiles();
            return fileIterator;
        } catch (SftpException e) {
            throw new FileOrchestratorException("Error occurred while listing files: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a file from the SFTP server and returns a BufferedReader for reading the file's contents.
     * If PGP encryption is enabled, the file will be decrypted before returning.
     *
     * @param fileName the name of the file to retrieve
     * @return a BufferedReader for reading the file's contents
     * @throws FileOrchestratorException if an error occurs during file retrieval or decryption
     */
    public BufferedReader getFileReader(String fileName) throws FileOrchestratorException {
        try {
            Path localFile = Files.createTempFile("sftp-", sftpProperties.getTempFileExtension());

            try (InputStream encryptedFileStream = sftpService.readFile(fileName)) {
                if (pgpProperties.isEnabled()) {
                    try (InputStream decryptedInput = pgpService.decrypt(encryptedFileStream)) {
                        Files.copy(decryptedInput, localFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    Files.copy(encryptedFileStream, localFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            return Files.newBufferedReader(localFile);
        } catch (Exception e){
            throw new FileOrchestratorException("Error occurred while reading file: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the next decrypted file from the SFTP server. If PGP encryption is enabled, the file will be decrypted before returning.
     *
     * @return a BufferedReader for reading the decrypted file, or null if there are no more files to process
     * @throws FileOrchestratorException if an error occurs during file retrieval or decryption
     */
    public BufferedReader getNextFile() throws FileOrchestratorException {
        try {
            if (fileIterator != null && fileIterator.hasNext()) {
                String fileName = fileIterator.next();
                Path localFile = Files.createTempFile("sftp-", sftpProperties.getTempFileExtension());

                try (InputStream encryptedFileStream = sftpService.readFile(fileName)) {
                    if (pgpProperties.isEnabled()) {
                        try (InputStream decryptedInput = pgpService.decrypt(encryptedFileStream)) {
                            Files.copy(decryptedInput, localFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } else {
                        Files.copy(encryptedFileStream, localFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                return Files.newBufferedReader(localFile);
            } else {
                return null; // No more files to process
            }
        } catch (Exception e) {
            throw new FileOrchestratorException("Error occurred while retrieving the next file: " + e.getMessage(), e);
        }
    }

    /**
     * Writes data to a file on the SFTP server. If PGP encryption is enabled, the data will be encrypted before writing.
     *
     * @param fileName        the name of the file to write
     * @param data            the data to write to the file
     * @throws FileOrchestratorException  if an error occurs during file writing or encryption
     */
    public void write(String fileName, String data) throws FileOrchestratorException {
        writeFileIntoSFTPServer(fileName, data, sftpProperties.getRemoteDirectoryOutput() );
    }

    /**
     * Writes data to a file on the SFTP server. If PGP encryption is enabled, the data will be encrypted before writing.
     *
     * @param remoteDirectory the remote directory on the SFTP server where the file will be written
     * @param fileName        the name of the file to write
     * @param data            the data to write to the file
     * @throws FileOrchestratorException  if an error occurs during file writing or encryption
     */
    public void write(String fileName, String data, String remoteDirectory) throws FileOrchestratorException {
        writeFileIntoSFTPServer(fileName, data, remoteDirectory);
    }

    /**
     * Writes data to a file on the SFTP server. If PGP encryption is enabled, the data will be encrypted before writing.
     *
     * @param remoteDirectory the remote directory on the SFTP server where the file will be written
     * @param fileName        the name of the file to write
     * @param data            the data to write to the file
     * @throws FileOrchestratorException if an error occurs during file writing or encryption
     */
    private void writeFileIntoSFTPServer(String fileName, String data, String remoteDirectory) throws FileOrchestratorException {
        try {
            String filePath = remoteDirectory + "/" + fileName;

            if (pgpProperties.isEnabled()) {
                InputStream encryptedFile = pgpService.encrypt(data);
                sftpService.write(encryptedFile, filePath);
                return;
            }
            sftpService.write(data, filePath);
        } catch (Exception e) {
            throw new FileOrchestratorException("Error occurred while writing file: " + e.getMessage(), e);
        }
    }
}