package com.pgp;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;

@Service
@Slf4j
public class PgpService {

    public InputStream decrypt(InputStream encryptedStream, InputStream privateKeyStream, String passphrase) throws Exception {

        PGPSecretKeyRingCollection secretKeyRings =
                new PGPSecretKeyRingCollection(
                        PGPUtil.getDecoderStream(privateKeyStream),
                        new JcaKeyFingerprintCalculator());

        PGPObjectFactory pgpFactory =
                new PGPObjectFactory(
                        PGPUtil.getDecoderStream(encryptedStream),
                        new JcaKeyFingerprintCalculator());

        Object obj = pgpFactory.nextObject();

        PGPEncryptedDataList encryptedDataList;

        if (obj instanceof PGPEncryptedDataList) {
            encryptedDataList = (PGPEncryptedDataList) obj;
        } else {
            encryptedDataList = (PGPEncryptedDataList) pgpFactory.nextObject();
        }

        PGPPublicKeyEncryptedData encryptedData = null;
        PGPPrivateKey privateKey = null;

        Iterator<?> iterator = encryptedDataList.getEncryptedDataObjects();

        while (iterator.hasNext()) {
            PGPPublicKeyEncryptedData current = (PGPPublicKeyEncryptedData) iterator.next();
            log.info( "Encrypted Key ID: {}", Long.toHexString(current.getKeyID()));
            PGPSecretKey secretKey = secretKeyRings.getSecretKey(current.getKeyID());

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
    }
}