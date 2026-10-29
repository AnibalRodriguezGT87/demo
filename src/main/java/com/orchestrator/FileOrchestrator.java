package com.orchestrator;

import com.sftp.SftpProperties;
import com.sftp.SftpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
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
	 * the remote directory to the local directory with maxDepth = 0 (default).
	 */
	public void synchronizeRemoteDirectory() {
		synchronizeRemoteDirectory(0);
	}

	/**
	 * Synchronizes the remote directory on the SFTP server by downloading all files from
	 * the remote directory to the local directory up to the specified max depth.
	 * @param maxDepth the maximum depth of subdirectories to traverse (default 0)
	 */
	public void synchronizeRemoteDirectory(int maxDepth) {
		try {
			String rootRemote = sftpProperties.getRemoteDirectoryInput();
			String rootLocal = sftpProperties.getLocalDirectory();

			log.info("Starting synchronization of remote directory: {} with maxDepth: {}", rootRemote, maxDepth);
			processDirectory(rootRemote, rootLocal, 0, maxDepth);
			log.info("Directory synchronization and download complete.");
		}
		catch (IOException ex) {
			log.error("Error during remote directory synchronization", ex);
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Recursively processes remote directories up to maxDepth, listing entries and
	 * orchestrating downloads.
	 */
	private void processDirectory(String remotePath, String localPath, int currentDepth, int maxDepth)
			throws IOException {
		List<SftpService.RemoteEntry> entries = sftpService.listEntries(remotePath);
		if (entries.isEmpty()) {
			log.info("No entries found in remote path: {}", remotePath);
			return;
		}

		for (SftpService.RemoteEntry entry : entries) {
			String name = entry.name();
			if (entry.isDirectory()) {
				if (currentDepth < maxDepth) {
					String subRemotePath = remotePath + "/" + name;
					String subLocalPath = Paths.get(localPath, name).toString();

					Files.createDirectories(Paths.get(subLocalPath));
					log.info("Entering subdirectory: {}", subRemotePath);
					processDirectory(subRemotePath, subLocalPath, currentDepth + 1, maxDepth);
				}
			}
			else {
				boolean downloadAllFiles = sftpProperties.getFileExtension().contains(".*");
				boolean matchesExtension = downloadAllFiles
						|| Arrays.stream(sftpProperties.getFileExtension().split(",")).anyMatch(name::endsWith);
				if (matchesExtension) {
					log.info("Orchestrating download for remote file: {}/{}", remotePath, name);
					Files.createDirectories(Paths.get(localPath));
					sftpService.downloadFile(remotePath, localPath, name);
				}
			}
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
	@Deprecated(forRemoval = true)
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
	@Deprecated(forRemoval = true)
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
	@Deprecated(forRemoval = true)
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

	/**
	 * Uploads a local file to the default remote output directory configured in
	 * {@link SftpProperties}, using the default cleanup configuration flag.
	 * @param localFilePath the path to the local file to upload
	 * @throws FileOrchestratorException if an error occurs during file upload or
	 * encryption
	 * @throws IllegalArgumentException if the local file does not exist or is invalid
	 */
	public void uploadFile(String localFilePath) throws FileOrchestratorException {
		uploadFile(localFilePath, sftpProperties.getRemoteDirectoryOutput(), sftpProperties.isCleanupLocal());
	}

	/**
	 * Uploads a local file to a specified remote target directory on the SFTP server. If
	 * PGP encryption is enabled, the data will be streamed and encrypted on-the-fly
	 * before writing. Optionally deletes the local file after a successful upload if
	 * cleanupLocal is set to true.
	 * @param localFilePath the path to the local file to upload
	 * @param remoteTargetDir the remote directory on the SFTP server where the file will
	 * be written
	 * @param cleanupLocal whether to delete the local file upon successful upload
	 * @throws FileOrchestratorException if an error occurs during file upload or
	 * encryption
	 * @throws IllegalArgumentException if the local file does not exist or is invalid
	 */
	public void uploadFile(String localFilePath, String remoteTargetDir, boolean cleanupLocal)
            throws FileOrchestratorException, FileOrchestratorException {
		File localFile = new File(localFilePath);

		if (!localFile.exists() || !localFile.isFile()) {
			throw new IllegalArgumentException("Local file does not exist or is invalid: " + localFilePath);
		}

		if (remoteTargetDir == null || remoteTargetDir.isEmpty()) {
			throw new IllegalArgumentException("Remote target directory is empty");
		}

		String filename = localFile.getName();
		String fullRemotePath = ".".equals(remoteTargetDir) ? filename
				: remoteTargetDir.endsWith("/") ? remoteTargetDir + filename : remoteTargetDir + "/" + filename;

		log.info("Uploading {} to remote path: {}", filename, fullRemotePath);

		try {
			sftpService.write(localFile.toPath(), fullRemotePath);

			if (cleanupLocal) {
				boolean deleted = deleteLocalFile(localFile);
				if (!deleted) {
					log.warn("Failed to delete local file after upload: {}", localFilePath);
				}
			}
		}
		catch (Exception e) {
			throw new FileOrchestratorException("Error occurred while uploading file: " + e.getMessage(), e);
		}
	}

	protected boolean deleteLocalFile(File file) {
		return file.delete();
	}

}
