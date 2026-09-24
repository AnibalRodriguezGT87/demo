package com.pgp;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
