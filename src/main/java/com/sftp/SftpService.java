package com.sftp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

@Configuration
public class SftpService {
    private final SftpProperties properties;

    public SftpService(SftpProperties properties) {
        this.properties = properties;
    }

    @Bean
    public DefaultSftpSessionFactory sftpSessionFactory() {
        DefaultSftpSessionFactory factory =
                new DefaultSftpSessionFactory(true);

        factory.setHost(properties.getHost());
        factory.setPort(properties.getPort());
        factory.setUser(properties.getUsername());
        factory.setPassword(properties.getPassword());

        factory.setAllowUnknownKeys(true);
        return factory;
    }
}
