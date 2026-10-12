package com.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

/**
 * PgpService class provides functionality to decrypt PGP encrypted data using a private key and passphrase.
 * It uses Bouncy Castle library for PGP operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PgpService {

    private final PGPProperties pgpProperties;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Decrypts the given PGP encrypted input stream using the private key and passphrase specified in the PGP properties.
     *
     * @param encryptedStream InputStream containing the PGP encrypted data
     * @return InputStream containing the decrypted data
     */
    public InputStream decrypt(InputStream encryptedStream) {
        try {
            String passphrase = pgpProperties.getPassphrase();
            InputStream privateKeyStream = Files.newInputStream(Paths.get(pgpProperties.getPrivateKey()));

            PGPSecretKeyRingCollection secretKeyRings =
                    new PGPSecretKeyRingCollection(
                            PGPUtil.getDecoderStream(privateKeyStream),
                            new JcaKeyFingerprintCalculator());

            PGPObjectFactory pgpFactory =
                    new PGPObjectFactory(
                            PGPUtil.getDecoderStream(encryptedStream),
                            new JcaKeyFingerprintCalculator());

            Object obj = pgpFactory.nextObject();

            if (obj == null) {
                throw new IllegalStateException("No encrypted data found in PGP message");
            }

            PGPEncryptedDataList encryptedDataList;

            if (obj instanceof PGPEncryptedDataList) {
                encryptedDataList = (PGPEncryptedDataList) obj;
            } else {
                Object nextObject = pgpFactory.nextObject();
                if (!(nextObject instanceof PGPEncryptedDataList)) {
                    throw new IllegalStateException("No encrypted data list found in PGP message");
                }
                encryptedDataList = (PGPEncryptedDataList) nextObject;
            }

            PGPPublicKeyEncryptedData encryptedData = null;
            PGPPrivateKey privateKey = null;

            Iterator<?> iterator = encryptedDataList.getEncryptedDataObjects();

            while (iterator.hasNext()) {
                PGPPublicKeyEncryptedData current = (PGPPublicKeyEncryptedData) iterator.next();
                log.info( "Encrypted Key ID: {}", Long.toHexString(current.getKeyIdentifier().getKeyId()));
                PGPSecretKey secretKey = secretKeyRings.getSecretKey(current.getKeyIdentifier().getKeyId());

                if (secretKey != null) {
                    log.info("Matched Secret Key ID: {}", Long.toHexString(secretKey.getKeyID()));
                    log.info("Algorithm: {}", secretKey.getPublicKey().getAlgorithm());
                    privateKey = secretKey.extractPrivateKey(
                                    new JcePBESecretKeyDecryptorBuilder()
                                            .setProvider("BC")
                                            .build(passphrase.toCharArray()));

                    encryptedData = current;
                    break;
                }
            }

            if (privateKey == null) {
                throw new IllegalStateException("No matching private key found");
            }

            PublicKeyDataDecryptorFactory decryptorFactory =
                    new JcePublicKeyDataDecryptorFactoryBuilder()
                            .setProvider("BC")
                            .build(privateKey);

            InputStream clearStream = encryptedData.getDataStream(decryptorFactory);

            PGPObjectFactory plainFactory =
                    new PGPObjectFactory(
                            clearStream,
                            new JcaKeyFingerprintCalculator());

            Object message = plainFactory.nextObject();

            if (message instanceof PGPCompressedData compressedData) {

                PGPObjectFactory compressedFactory =
                        new PGPObjectFactory(
                                compressedData.getDataStream(),
                                new JcaKeyFingerprintCalculator());

                message = compressedFactory.nextObject();
                plainFactory = compressedFactory;
            }

            while (message != null) {
                log.info("PGP Object Type: {}", message.getClass().getName());

                if (message instanceof PGPLiteralData literalData) {
                    InputStream literalInputStream = literalData.getInputStream();
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    literalInputStream.transferTo(output);

                    return new ByteArrayInputStream(output.toByteArray());
                }

                if (message instanceof PGPOnePassSignatureList) {
                    log.info("PGP Signature detected");
                }

                if (message instanceof PGPSignatureList) {
                    log.info("PGP Signature list detected");
                }
                message = plainFactory.nextObject();
            }

            throw new IllegalStateException("No PGPLiteralData found in PGP message");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt PGP content with the configured private key", e);
        }
    }

    /**
     * Encrypts the given plain text input stream using the public key specified in the PGP properties.
     *
     * @param data the string containing the plain text data to be encrypted
     * @return InputStream containing the encrypted data
     * @throws Exception if encryption fails or no encryption key is found in the public key input
     */
    public InputStream encrypt(String data) throws Exception {

        InputStream publicKey = Files.newInputStream(Paths.get(pgpProperties.getPublicKey()));

        PGPPublicKeyRingCollection keyRings = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(publicKey),
                new JcaKeyFingerprintCalculator());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Iterator<PGPPublicKeyRing> ringIterator = keyRings.getKeyRings();

        PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(
                        SymmetricKeyAlgorithmTags.AES_256)
                        .setWithIntegrityPacket(true)
                        .setSecureRandom(new SecureRandom())
                        .setProvider("BC")
        );

        boolean hasEncryptionKey = false;
        while (ringIterator.hasNext()) {
            PGPPublicKeyRing ring = ringIterator.next();
            Iterator<PGPPublicKey> keyIterator = ring.getPublicKeys();

            while (keyIterator.hasNext()) {
                PGPPublicKey key = keyIterator.next();

                if (key.isEncryptionKey()) {
                    hasEncryptionKey = true;
                    encryptedDataGenerator.addMethod(
                            new JcePublicKeyKeyEncryptionMethodGenerator(key).setProvider("BC"));
                }
            }
        }

        if (!hasEncryptionKey) {
            throw new PGPException("No encryption key found in public key input");
        }

        try (OutputStream encryptedOutput = encryptedDataGenerator.open(output, new byte[4096])) {
            PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(data.getBytes());
            ByteArrayInputStream plainTexByteArray = new ByteArrayInputStream(outputStream.toByteArray());

            try (OutputStream literalOutput = literalDataGenerator.open(
                    encryptedOutput,
                    PGPLiteralData.BINARY,
                    "file",
                    new Date(),
                    new byte[4096])) {
                plainTexByteArray.transferTo(literalOutput);
            }
            outputStream.close();
            plainTexByteArray.close();
        }

        return new ByteArrayInputStream(output.toByteArray());
    }
}