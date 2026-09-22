package com.sftp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SftpProperties class holds the configuration properties for SFTP connections.
 * It includes host, port, username, password, and file extensions for encrypted and temporary files.
 */
@Component
@ConfigurationProperties(prefix = "sftp")
@Getter
@Setter
public class SftpProperties {

    private String host;
    private int port;
    private String username;
    private String password;
    private String fileExtension = ".gpg";
    private String tempFileExtension = ".txt";
    private String privateKey;
    private String privateKeyPassphrase;

}
