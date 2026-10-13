package com.orchestrator;

import com.sftp.SftpProperties;
import com.sftp.SftpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileOrchestrator {

    private final SftpService sftpService;
    private final SftpProperties sftpProperties;

    /**
     * Returns the list of files retrieved from the SFTP server.
     *
     * @return a list of file names
     */
    public List<String> getListFiles() {
        try {
            return sftpService.listRemoteFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves a file from the SFTP server and returns a BufferedReader for reading the file's contents.
     *
     * @param fileName the name of the file to retrieve
     * @return a BufferedReader for reading the file's contents
     * @throws FileOrchestratorException if an error occurs during file retrieval
     */
    public BufferedReader getFileReader(String fileName) throws FileOrchestratorException {
        try {
            return sftpService.readFile(fileName);
        } catch (Exception e){
            throw new FileOrchestratorException("Error occurred while reading file: " + e.getMessage(), e);
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
            sftpService.write(data, filePath);
        } catch (Exception e) {
            throw new FileOrchestratorException("Error occurred while writing file: " + e.getMessage(), e);
        }
    }
}