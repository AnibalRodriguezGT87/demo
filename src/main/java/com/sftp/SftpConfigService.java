package com.sftp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

/**
 * SftpConfigService class provides configuration for SFTP session factory.
 * It uses properties defined in SftpProperties to configure the SFTP connection.
 */
@Configuration
public class SftpConfigService {
    private final SftpProperties properties;

    public SftpConfigService(SftpProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates and configures a DefaultSftpSessionFactory bean for SFTP connections.
     *
     * @return Configured DefaultSftpSessionFactory instance
     */
    @Bean
    public DefaultSftpSessionFactory sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);

        factory.setHost(properties.getHost());
        factory.setPort(properties.getPort());
        factory.setUser(properties.getUsername());
        factory.setAllowUnknownKeys(true);
        if (properties.getPrivateKey() != null && !properties.getPrivateKey().isBlank()) {
            factory.setPrivateKey(new FileSystemResource(properties.getPrivateKey()));
            if (properties.getPrivateKeyPassphrase() != null && !properties.getPrivateKeyPassphrase().isBlank()) {
                factory.setPrivateKeyPassphrase(properties.getPrivateKeyPassphrase());
            }

        } else if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            factory.setPassword(properties.getPassword());
        }

        return factory;
    }
}
