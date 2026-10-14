package com.sftp;

import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.Test;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class SftpTemplateTest {

    @Test
    void execute_returnsCallbackResult_andClosesSession() throws Exception {
        SessionFactory<SftpClient.DirEntry> sessionFactory = mock(SessionFactory.class);
        Session<SftpClient.DirEntry> session = mock(Session.class);
        when(sessionFactory.getSession()).thenReturn(session);

        SftpTemplate template = new SftpTemplate(sessionFactory);

        String result = template.execute(currentSession -> {
            assertSame(session, currentSession);
            return "done";
        });

        assertEquals("done", result);
        verify(sessionFactory).getSession();
        verify(session).close();
    }
}
