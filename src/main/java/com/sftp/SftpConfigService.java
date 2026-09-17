package com.sftp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

@Configuration
public class SftpConfigService {
    private final SftpProperties properties;

    public SftpConfigService(SftpProperties properties) {
        this.properties = properties;
    }

    @Bean
    public DefaultSftpSessionFactory sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);

        factory.setHost(properties.getHost());
        factory.setPort(properties.getPort());
        factory.setUser(properties.getUsername());
        factory.setPassword(properties.getPassword());
        factory.setPrivateKey(new FileSystemResource("/keys/id_rsa"));
        factory.setAllowUnknownKeys(true);

        return factory;
    }
}
