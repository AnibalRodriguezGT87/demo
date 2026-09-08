package com.sftp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

@Configuration
public class SftpService {

    @Bean
    public DefaultSftpSessionFactory sftpSessionFactory() {
        DefaultSftpSessionFactory factory =
                new DefaultSftpSessionFactory(true);

        factory.setHost("localhost");
        factory.setPort(2222);
        factory.setUser("test");
        factory.setPassword("test");

        factory.setAllowUnknownKeys(true);
        return factory;
    }
}
