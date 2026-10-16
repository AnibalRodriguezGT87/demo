package com.orchestrator;

import com.sftp.SftpProperties;
import com.sftp.SftpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * FileOrchestrator is a service class that orchestrates file operations with an SFTP
 * server. It provides methods to list files, read files, and write files to the SFTP
 * server. The class handles exceptions and wraps them in a custom
 * FileOrchestratorException for better error reporting.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileOrchestrator {

	private final SftpService sftpService;

	private final SftpProperties sftpProperties;

	/**
	 * Synchronizes the remote directory on the SFTP server by downloading all files from
	 * the remote directory to the local directory.
	 *
	 */
	public void synchronizeRemoteDirectory() {
		try {
			List<String> remoteFiles = sftpService.listRemoteFiles();
			if (remoteFiles.isEmpty()) {
				log.info("No files found in remote path: {}", sftpProperties.getRemoteDirectoryInput());
				return;
			}

			log.info("Found {} files to process in remote path.", remoteFiles.size());
			for (String file : sftpService.listRemoteFiles()) {
				log.info("Processing remote file: {}", sftpProperties.getRemoteDirectoryInput() + "/" + file);
				sftpService.downloadFile(sftpProperties.getRemoteDirectoryInput(), sftpProperties.getLocalDirectory(),
						file);
			}
			log.info("Directory synchronization and download complete.");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Writes data to a file on the SFTP server. If PGP encryption is enabled, the data
	 * will be encrypted before writing.
	 * @param fileName the name of the file to write
	 * @param data the data to write to the file
	 * @throws FileOrchestratorException if an error occurs during file writing or
	 * encryption
	 */
	public void write(String fileName, String data) throws FileOrchestratorException {
		writeFileIntoSFTPServer(fileName, data, sftpProperties.getRemoteDirectoryOutput());
	}

	/**
	 * Writes data to a file on the SFTP server. If PGP encryption is enabled, the data
	 * will be encrypted before writing.
	 * @param remoteDirectory the remote directory on the SFTP server where the file will
	 * be written
	 * @param fileName the name of the file to write
	 * @param data the data to write to the file
	 * @throws FileOrchestratorException if an error occurs during file writing or
	 * encryption
	 */
	public void write(String fileName, String data, String remoteDirectory) throws FileOrchestratorException {
		writeFileIntoSFTPServer(fileName, data, remoteDirectory);
	}

	/**
	 * Writes data to a file on the SFTP server. If PGP encryption is enabled, the data
	 * will be encrypted before writing.
	 * @param remoteDirectory the remote directory on the SFTP server where the file will
	 * be written
	 * @param fileName the name of the file to write
	 * @param data the data to write to the file
	 * @throws FileOrchestratorException if an error occurs during file writing or
	 * encryption
	 */
	private void writeFileIntoSFTPServer(String fileName, String data, String remoteDirectory)
			throws FileOrchestratorException {
		try {
			String filePath = remoteDirectory + "/" + fileName;
			sftpService.write(data, filePath);
		}
		catch (Exception e) {
			throw new FileOrchestratorException("Error occurred while writing file: " + e.getMessage(), e);
		}
	}

}