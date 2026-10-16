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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

/**
 * PgpService class provides functionality to decrypt PGP encrypted data using a private
 * key and passphrase. It uses Bouncy Castle library for PGP operations.
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
	 * Decrypts the given encrypted input stream using the private key and passphrase
	 * specified in the PGP properties.
	 * @param encryptedStream the input stream containing the encrypted data
	 * @param decryptedOutput the output stream where the decrypted data will be written
	 */
	public void decrypt(InputStream encryptedStream, OutputStream decryptedOutput) {

		try {
			try (InputStream privateKeyStream = Files.newInputStream(Paths.get(pgpProperties.getPrivateKey()))) {
				PGPSecretKeyRingCollection secretKeyRings = new PGPSecretKeyRingCollection(
						PGPUtil.getDecoderStream(privateKeyStream), new JcaKeyFingerprintCalculator());

				PGPObjectFactory pgpFactory = new PGPObjectFactory(PGPUtil.getDecoderStream(encryptedStream),
						new JcaKeyFingerprintCalculator());

				Object obj = pgpFactory.nextObject();

				PGPEncryptedDataList encryptedDataList = (obj instanceof PGPEncryptedDataList)
						? (PGPEncryptedDataList) obj : (PGPEncryptedDataList) pgpFactory.nextObject();

				PGPPublicKeyEncryptedData encryptedData = null;
				PGPPrivateKey privateKey = null;

				Iterator<?> iterator = encryptedDataList.getEncryptedDataObjects();
				String passphrase = pgpProperties.getPassphrase();

				while (iterator.hasNext()) {
					PGPPublicKeyEncryptedData current = (PGPPublicKeyEncryptedData) iterator.next();
					PGPSecretKey secretKey = secretKeyRings.getSecretKey(current.getKeyIdentifier().getKeyId());

					if (secretKey != null) {
						privateKey = secretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC")
							.build(passphrase.toCharArray()));
						encryptedData = current;
						break;
					}
				}

				if (privateKey == null) {
					throw new IllegalStateException("No matching private key found");
				}

				PublicKeyDataDecryptorFactory decryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
					.setProvider("BC")
					.build(privateKey);

				InputStream clearStream = encryptedData.getDataStream(decryptorFactory);

				PGPObjectFactory plainFactory = new PGPObjectFactory(clearStream, new JcaKeyFingerprintCalculator());

				Object message = plainFactory.nextObject();

				if (message instanceof PGPCompressedData compressedData) {
					plainFactory = new PGPObjectFactory(compressedData.getDataStream(),
							new JcaKeyFingerprintCalculator());
					message = plainFactory.nextObject();
				}

				while (message != null) {
					if (message instanceof PGPLiteralData literalData) {
						try (InputStream literalInputStream = literalData.getInputStream()) {
							literalInputStream.transferTo(decryptedOutput);
						}
					}
					message = plainFactory.nextObject();
				}

				if (encryptedData.isIntegrityProtected()) {
					if (!encryptedData.verify()) {
						throw new SecurityException("PGP integrity verification failed");
					}
				}
			}

		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to decrypt PGP content", e);
		}
	}

	/**
	 * Encrypts the given plain text input stream using the public key specified in the
	 * PGP properties.
	 * @param data the string containing the plain text data to be encrypted
	 * @return InputStream containing the encrypted data
	 * @throws Exception if encryption fails or no encryption key is found in the public
	 * key input
	 */
	public InputStream encrypt(String data) throws Exception {
		Path publicKeyPath = Paths.get(pgpProperties.getPublicKey());

		try (InputStream publicKeyInput = Files.newInputStream(publicKeyPath)) {
			PGPPublicKeyRingCollection keyRings = new PGPPublicKeyRingCollection(
					PGPUtil.getDecoderStream(publicKeyInput), new JcaKeyFingerprintCalculator());

			PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
					new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256).setWithIntegrityPacket(true)
						.setSecureRandom(new SecureRandom())
						.setProvider("BC"));

			boolean hasEncryptionKey = false;

			Iterator<PGPPublicKeyRing> ringIterator = keyRings.getKeyRings();

			while (ringIterator.hasNext()) {
				PGPPublicKeyRing ring = ringIterator.next();
				Iterator<PGPPublicKey> keyIterator = ring.getPublicKeys();

				while (keyIterator.hasNext()) {
					PGPPublicKey key = keyIterator.next();

					if (key.isEncryptionKey() && !key.hasRevocation()) {
						encryptedDataGenerator
							.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(key).setProvider("BC"));
						hasEncryptionKey = true;
					}
				}
			}

			if (!hasEncryptionKey) {
				throw new PGPException("No valid encryption key found");
			}
			ByteArrayOutputStream encryptedBuffer = new ByteArrayOutputStream();

			try (OutputStream encryptedOutput = encryptedDataGenerator.open(encryptedBuffer, new byte[8192]);

					InputStream plainTextInput = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {

				PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();

				try (OutputStream literalOutput = literalDataGenerator.open(encryptedOutput, PGPLiteralData.BINARY,
						PGPLiteralData.CONSOLE, new Date(), new byte[8192])) {

					plainTextInput.transferTo(literalOutput);
				}
			}

			return new ByteArrayInputStream(encryptedBuffer.toByteArray());
		}
	}

}