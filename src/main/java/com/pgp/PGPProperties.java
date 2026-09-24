package com.pgp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PGPProperties class holds the configuration properties for PGP encryption and decryption.
 * It includes the passphrase and private key used for PGP operations.
 */
@Component
@ConfigurationProperties(prefix = "pgp")
@Getter
@Setter
public class PGPProperties {
    private String passphrase;
    private String privateKey;
    private String publicKey;
    private boolean enabled = false;

}
