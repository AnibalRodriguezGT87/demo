package com.pgp;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PgpServiceTest {

    private final PgpService service = new PgpService();

    @Test
    void decrypt_shouldReturnOriginalMessage_whenPrivateKeyMatches() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");

        PgpArtifacts artifacts = createPgpArtifacts(
                "owner@example.com",
                "OwnerPassphrase!123",
                "mensaje secreto"
        );

        try (InputStream encryptedStream = Files.newInputStream(artifacts.encryptedFile());
             InputStream privateKeyStream = Files.newInputStream(artifacts.privateKeyFile())) {

            try (InputStream decrypted = service.decrypt(encryptedStream, privateKeyStream, artifacts.passphrase())) {
                String result = new String(decrypted.readAllBytes(), StandardCharsets.UTF_8);
                assertEquals("mensaje secreto", result);
            }
        }
    }

    @Test
    void decrypt_shouldFail_whenPrivateKeyDoesNotMatchEncryptedContent() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");

        PgpArtifacts ownerArtifacts = createPgpArtifacts(
                "owner@example.com",
                "OwnerPassphrase!123",
                "mensaje secreto"
        );

        PgpArtifacts wrongArtifacts = createPgpArtifacts(
                "wrong@example.com",
                "WrongPassphrase!456",
                "otra cosa"
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            try (InputStream encryptedStream = Files.newInputStream(ownerArtifacts.encryptedFile());
                 InputStream privateKeyStream = Files.newInputStream(wrongArtifacts.privateKeyFile())) {
                service.decrypt(encryptedStream, privateKeyStream, wrongArtifacts.passphrase());
            }
        });

        assertTrue(exception.getMessage().contains("No matching private key found"));
    }

    @Test
    void decrypt_shouldFail_whenEncryptedContentIsMalformed() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");

        Path tempFile = Files.createTempFile("pgp-invalid-", ".txt");
        Files.writeString(tempFile, "this is not a valid encrypted PGP message", StandardCharsets.UTF_8);

        Path keyHome = Files.createTempDirectory("pgp-invalid-key-");
        String uid = "invalid@example.com";
        String passphrase = "InvalidPassphrase!123";

        runGpg(keyHome, "--batch", "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--quick-generate-key", uid, "rsa2048", "encrypt", "0");

        Path privateKeyFile = keyHome.resolve("private-key.asc");
        runGpg(keyHome, "--batch", "--yes", "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--armor", "--output", privateKeyFile.toString(), "--export-secret-keys", uid);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            try (InputStream encryptedStream = Files.newInputStream(tempFile);
                 InputStream privateKeyStream = Files.newInputStream(privateKeyFile)) {
                service.decrypt(encryptedStream, privateKeyStream, passphrase);
            }
        });

        assertTrue(exception.getMessage().contains("No encrypted data")
                || exception.getMessage().contains("No matching private key")
                || exception.getMessage().contains("No PGPLiteralData"));
    }

    @Test
    void decrypt_shouldFail_whenMessageIsEmpty() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");
        PgpArtifacts artifacts = createPgpArtifacts(
                "empty-message@example.com",
                "EmptyMessagePassphrase!123",
                "test"
        );

        try (InputStream privateKeyStream = Files.newInputStream(artifacts.privateKeyFile())) {
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> service.decrypt(InputStream.nullInputStream(), privateKeyStream, artifacts.passphrase()));
            assertEquals("No encrypted data found in PGP message", exception.getMessage());
        }
    }

    @Test
    void decrypt_shouldFail_whenFirstPacketIsNotAnEncryptedDataList() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");
        PgpArtifacts artifacts = createPgpArtifacts(
                "missing-list@example.com",
                "MissingListPassphrase!123",
                "test"
        );

        try (InputStream privateKeyStream = Files.newInputStream(artifacts.privateKeyFile())) {
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> service.decrypt(new ByteArrayInputStream(literalPacket()), privateKeyStream,
                            artifacts.passphrase()));
            assertEquals("No encrypted data list found in PGP message", exception.getMessage());
        }
    }

    @Test
    void decrypt_shouldContinueToEncryptedDataList_afterAnUnrelatedPacket() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");
        PgpArtifacts artifacts = createPgpArtifacts(
                "leading-packet@example.com",
                "LeadingPacketPassphrase!123",
                "mensaje despues del paquete"
        );

        byte[] encryptedBytes;
        try (InputStream armoredEncrypted = Files.newInputStream(artifacts.encryptedFile());
             InputStream decodedEncrypted = PGPUtil.getDecoderStream(armoredEncrypted)) {
            encryptedBytes = decodedEncrypted.readAllBytes();
        }
        byte[] input = new byte[literalPacket().length + encryptedBytes.length];
        System.arraycopy(literalPacket(), 0, input, 0, literalPacket().length);
        System.arraycopy(encryptedBytes, 0, input, literalPacket().length, encryptedBytes.length);

        try (InputStream privateKeyStream = Files.newInputStream(artifacts.privateKeyFile());
             InputStream decrypted = service.decrypt(new ByteArrayInputStream(input), privateKeyStream,
                     artifacts.passphrase())) {
            assertEquals("mensaje despues del paquete",
                    new String(decrypted.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void decrypt_shouldFail_whenMatchingPrivateKeyPassphraseIsWrong() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");
        PgpArtifacts artifacts = createPgpArtifacts(
                "wrong-passphrase@example.com",
                "CorrectPassphrase!123",
                "test"
        );

        try (InputStream encryptedStream = Files.newInputStream(artifacts.encryptedFile());
             InputStream privateKeyStream = Files.newInputStream(artifacts.privateKeyFile())) {
            assertThrows(PGPException.class,
                    () -> service.decrypt(encryptedStream, privateKeyStream, "WrongPassphrase!456"));
        }
    }

    @Test
    void decrypt_shouldRejectSignedPacketsWithoutLiteralData() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");
        PgpArtifacts recipient = createPgpArtifacts(
                "signed-packets-recipient@example.com",
                "RecipientPassphrase!123",
                "test"
        );
        Path signingKeyHome = Files.createTempDirectory("pgp-signing-packets-");
        String signingUid = "signature-packets@example.com";
        runGpg(signingKeyHome, "--batch", "--pinentry-mode", "loopback",
                "--passphrase", "",
                "--quick-generate-key", signingUid, "rsa2048", "sign", "0");
        Path signingPrivateKey = signingKeyHome.resolve("signing-key.asc");
        runGpg(signingKeyHome, "--batch", "--yes", "--armor",
                "--output", signingPrivateKey.toString(), "--export-secret-keys", signingUid);

        byte[] signaturePackets;
        try (InputStream secretKeyInput = Files.newInputStream(signingPrivateKey)) {
            PGPSecretKeyRingCollection secretKeyRings = new PGPSecretKeyRingCollection(
                    PGPUtil.getDecoderStream(secretKeyInput),
                    new JcaKeyFingerprintCalculator());
            PGPSecretKeyRing secretKeyRing = secretKeyRings.getKeyRings().next();
            PGPSecretKey secretKey = secretKeyRing.getSecretKeys().next();
            PGPPrivateKey privateKey = secretKey.extractPrivateKey(
                    new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(new char[0]));
            PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
                    new JcaPGPContentSignerBuilder(secretKey.getPublicKey().getAlgorithm(),
                            HashAlgorithmTags.SHA256).setProvider("BC"));
            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);

            ByteArrayOutputStream signatureOutput = new ByteArrayOutputStream();
            signatureGenerator.generateOnePassVersion(false).encode(signatureOutput);
            signatureGenerator.generate().encode(signatureOutput);
            signaturePackets = signatureOutput.toByteArray();
        }

        byte[] encryptedSignaturePackets;
        try (InputStream publicKeyInput = Files.newInputStream(recipient.publicKeyFile())) {
            encryptedSignaturePackets = encryptRawPayload(signaturePackets, publicKeyInput);
        }

        try (InputStream privateKeyInput = Files.newInputStream(recipient.privateKeyFile())) {
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> service.decrypt(new ByteArrayInputStream(encryptedSignaturePackets),
                            privateKeyInput, recipient.passphrase()));
            assertEquals("No PGPLiteralData found in PGP message", exception.getMessage());
        }
    }

    @Test
    void encrypt_shouldReturnEncryptedDataThatCanBeDecrypted() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");

        PgpArtifacts artifacts = createPgpArtifacts(
                "encrypt@example.com",
                "EncryptPassphrase!123",
                "mensaje cifrado"
        );

        try (InputStream plainText = Files.newInputStream(artifacts.messageFile());
             InputStream publicKeyStream = Files.newInputStream(artifacts.publicKeyFile());
             InputStream encryptedStream = service.encrypt(plainText, publicKeyStream);
             InputStream privateKeyStream = Files.newInputStream(artifacts.privateKeyFile())) {

            byte[] encryptedBytes = encryptedStream.readAllBytes();
            assertTrue(encryptedBytes.length > 0);

            try (InputStream decrypted = service.decrypt(new ByteArrayInputStream(encryptedBytes), privateKeyStream, artifacts.passphrase())) {
                String result = new String(decrypted.readAllBytes(), StandardCharsets.UTF_8);
                assertEquals("mensaje cifrado", result);
            }
        }
    }

    @Test
    void encrypt_shouldFail_whenPublicKeyHasNoEncryptionKey() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");
        Path keyHome = Files.createTempDirectory("pgp-signing-key-");
        String uid = "signing-only@example.com";
        runGpg(keyHome, "--batch", "--pinentry-mode", "loopback",
                "--passphrase", "",
                "--quick-generate-key", uid, "ed25519", "sign", "0");

        Path publicKeyFile = keyHome.resolve("signing-key.asc");
        runGpg(keyHome, "--batch", "--yes", "--armor",
                "--output", publicKeyFile.toString(), "--export", uid);

        try (InputStream publicKeyStream = Files.newInputStream(publicKeyFile)) {
            PGPException exception = assertThrows(PGPException.class,
                    () -> service.encrypt(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)),
                            publicKeyStream));
            assertEquals("No encryption key found in public key input", exception.getMessage());
        }
    }

    @Test
    void encrypt_shouldFail_whenPublicKeyCollectionIsEmpty() {
        PGPException exception = assertThrows(PGPException.class,
                () -> service.encrypt(new ByteArrayInputStream(new byte[0]),
                        new ByteArrayInputStream(new byte[0])));

        assertEquals("No encryption key found in public key input", exception.getMessage());
    }

    private static byte[] literalPacket() {
        return new byte[]{(byte) 0xAC, 6, (byte) 'b', 0, 0, 0, 0, 0};
    }

    private static byte[] encryptRawPayload(byte[] payload, InputStream publicKeyInput) throws Exception {
        PGPPublicKeyRingCollection keyRings = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(publicKeyInput),
                new JcaKeyFingerprintCalculator());
        PGPPublicKey encryptionKey = null;
        for (var ringIterator = keyRings.getKeyRings(); ringIterator.hasNext() && encryptionKey == null;) {
            PGPPublicKeyRing ring = ringIterator.next();
            for (var keyIterator = ring.getPublicKeys(); keyIterator.hasNext();) {
                PGPPublicKey key = keyIterator.next();
                if (key.isEncryptionKey()) {
                    encryptionKey = key;
                    break;
                }
            }
        }
        if (encryptionKey == null) {
            throw new IllegalStateException("Test recipient key has no encryption key");
        }

        PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
                        .setWithIntegrityPacket(true)
                        .setProvider("BC"));
        encryptedDataGenerator.addMethod(
                new JcePublicKeyKeyEncryptionMethodGenerator(encryptionKey).setProvider("BC"));
        ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();
        try (OutputStream packetOutput = encryptedDataGenerator.open(encryptedOutput, new byte[4096])) {
            packetOutput.write(payload);
        }
        return encryptedOutput.toByteArray();
    }

    private static boolean isGpgAvailable() {
        try {
            Process process = new ProcessBuilder("gpg", "--version").start();
            int code = process.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static PgpArtifacts createPgpArtifacts(String uid, String passphrase, String message) throws Exception {
        Path keyHome = Files.createTempDirectory("pgp-test-home-");

        runGpg(keyHome,
                "--batch",
                "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--quick-generate-key", uid, "rsa2048", "encrypt", "0");

        Path messageFile = keyHome.resolve("message.txt");
        Files.writeString(messageFile, message, StandardCharsets.UTF_8);

        Path encryptedFile = keyHome.resolve("message.txt.asc");
        runGpg(keyHome,
                "--batch",
                "--yes",
                "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--armor",
                "--output", encryptedFile.toString(),
                "--encrypt",
                "--recipient", uid,
                messageFile.toString());

        Path publicKeyFile = keyHome.resolve("public-key.asc");
        runGpg(keyHome,
                "--batch",
                "--yes",
                "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--armor",
                "--output", publicKeyFile.toString(),
                "--export",
                uid);

        Path privateKeyFile = keyHome.resolve("private-key.asc");
        runGpg(keyHome,
                "--batch",
                "--yes",
                "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--armor",
                "--output", privateKeyFile.toString(),
                "--export-secret-keys",
                uid);

        return new PgpArtifacts(encryptedFile, privateKeyFile, passphrase, publicKeyFile, messageFile);
    }

    private static void runGpg(Path gnupgHome, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("gpg");
        command.add("--homedir");
        command.add(gnupgHome.toString());
        command.addAll(List.of(args));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("gpg command failed: " + output);
        }
    }

    private record PgpArtifacts(Path encryptedFile, Path privateKeyFile, String passphrase, Path publicKeyFile, Path messageFile) {
    }
}
