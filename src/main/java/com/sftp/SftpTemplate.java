package com.sftp;

import lombok.RequiredArgsConstructor;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SftpTemplate class provides a template for executing SFTP operations using a session
 * factory. It allows executing operations within an SFTP session and handles session
 * management.
 */
@Component
@RequiredArgsConstructor
public class SftpTemplate {

	private final SessionFactory<SftpClient.DirEntry> sessionFactory;

	/**
	 * Executes the provided callback within an SFTP session.
	 * @param callback the callback to execute within the session
	 * @param <T> the type of the result returned by the callback
	 * @return the result of the callback execution
	 * @throws IOException if an I/O error occurs during session execution
	 */
	public <T> T execute(SessionCallback<SftpClient.DirEntry, T> callback) throws IOException {
		try (Session<SftpClient.DirEntry> session = sessionFactory.getSession()) {
			return callback.doInSession(session);
		}
	}

}