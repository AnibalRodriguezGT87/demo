package com.sftp;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SftpTemplate {
    private final SessionFactory<SftpClient.DirEntry> sessionFactory;

    public SftpTemplate(SessionFactory<SftpClient.DirEntry> sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public <T> T execute(SessionCallback<SftpClient.DirEntry, T> callback) throws IOException {
        try (Session<SftpClient.DirEntry> session = sessionFactory.getSession()) {
            return callback.doInSession(session);
        }
    }
}