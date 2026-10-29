package com.sftp;

import com.crypto.PGPProperties;
import com.crypto.PgpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service class for handling SFTP operations, including reading and writing files, as
 * well as decrypting PGP-encrypted files.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SftpService {

	private final PGPProperties pgpProperties;

	private final SftpTemplate sftpTemplate;

	private final PgpService pgpService;

	/**
	 * Representation of a remote directory entry (file or folder).
	 */
	public record RemoteEntry(String name, boolean isDirectory) {
	}

	/**
	 * Downloads a file from the SFTP server. If PGP encryption is enabled, the file will
	 * be decrypted after download.
	 * @param remotePath the remote directory path on the SFTP server
	 * @param targetLocalPath the local directory path where the file will be saved
	 * @param fileName the name of the file to download
	 * @throws IOException if an I/O error occurs during download or decryption
	 */
	public void downloadFile(String remotePath, String targetLocalPath, String fileName) throws IOException {
		sftpTemplate.execute(session -> {
			Path encryptedFile = Files.createTempFile("sftp-enc-", ".temp");
			Path decryptedTempFile = null;
			try {
				String fullRemotePath = remotePath + "/" + fileName;
				try (OutputStream os = Files.newOutputStream(encryptedFile)) {
					session.read(fullRemotePath, os);
				}

				Path destinationFile = Paths.get(targetLocalPath, fileName);
				if (pgpProperties.isEnabled()) {
					decryptedTempFile = Files.createTempFile(Paths.get(targetLocalPath), "sftp-dec-", ".tmp");

					try (InputStream encryptedInput = Files.newInputStream(encryptedFile);
							OutputStream decryptedOutput = Files.newOutputStream(decryptedTempFile)) {
						pgpService.decrypt(encryptedInput, decryptedOutput);
					}
					Files.move(decryptedTempFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
					decryptedTempFile = null;
				}
				else {
					Files.move(encryptedFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			finally {
				Files.deleteIfExists(encryptedFile);
				if (decryptedTempFile != null) {
					Files.deleteIfExists(decryptedTempFile);
				}
			}
			return null;
		});
	}

	/**
	 * Lists entries (files and directories) in a given remote directory.
	 * <p>
	 * If the specified remote path does not exist, a warning is logged and an empty list
	 * is returned.
	 * @param remotePath the remote directory path
	 * @return a list of RemoteEntry containing name and isDirectory flag, or an empty
	 * list if the path does not exist
	 * @throws IOException if an I/O error occurs while listing entries
	 */
	public List<RemoteEntry> listEntries(String remotePath) throws IOException {
		return sftpTemplate.execute(session -> {
			if (!session.exists(remotePath)) {
				log.warn("Remote path does not exist: {}", remotePath);
				return Collections.emptyList();
			}

			Iterable<SftpClient.DirEntry> entries = List.of(session.list(remotePath));
			List<RemoteEntry> result = new ArrayList<>();
			for (SftpClient.DirEntry entry : entries) {
				String name = entry.getFilename();
				if (name != null && !name.isBlank() && !".".equals(name) && !"..".equals(name)) {
					boolean isDir = entry.getAttributes().isDirectory();
					result.add(new RemoteEntry(name, isDir));
				}
			}
			return result;
		});
	}

	/**
	 * Writes data to a file on the SFTP server. If PGP encryption is enabled, the data
	 * will be encrypted before writing.
	 * @param data the data to write to the file
	 * @param filePath the path of the file on the SFTP server
	 * @throws IOException if an I/O error occurs
	 */
	@Deprecated(forRemoval = true)
	public void write(String data, String filePath) throws IOException {
		sftpTemplate.execute(session -> {
			try {
				if (pgpProperties.isEnabled()) {
					try (InputStream encryptedFile = pgpService.encrypt(data)) {
						session.write(encryptedFile, filePath);
					}
					return null;
				}

				try (InputStream in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
					session.write(in, filePath);
				}
				return null;

			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Writes a local file to the SFTP server using direct on-the-fly streaming. If PGP
	 * encryption is enabled, the file is encrypted chunk-by-chunk and streamed directly
	 * into the remote SFTP output stream with no intermediate disk or memory footprint.
	 * @param localFilePath the path of the local file to upload
	 * @param filePath the target path on the SFTP server
	 * @throws IOException if an I/O error occurs during upload or encryption
	 */
	public void write(Path localFilePath, String filePath) throws IOException {
		sftpTemplate.execute(session -> {
			try {
				// If PGP is enabled and session supports direct SftpClient streaming
				if (pgpProperties.isEnabled() && session instanceof SftpSession sftpSession) {
					SftpClient sftpClient = sftpSession.getClientInstance();
					try (OutputStream remoteOut = sftpClient.write(filePath)) {
						pgpService.encryptStream(localFilePath, remoteOut);
					}
					return null;
				}

				// Fallback or plain text upload when PGP is disabled
				try (InputStream in = Files.newInputStream(localFilePath)) {
					session.write(in, filePath);
				}
				return null;
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to write file to SFTP: " + e.getMessage(), e);
			}
		});
	}

}
