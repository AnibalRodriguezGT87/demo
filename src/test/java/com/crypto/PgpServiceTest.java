package com.crypto;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class PgpServiceTest {

    @Test
    void encrypt_shouldReturnEncryptedDataThatCanBeDecrypted() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");

        Path keyHome = Files.createTempDirectory("pgp-test-home-");
        String uid = "encrypt@example.com";
        String passphrase = "EncryptPassphrase!123";
        String message = "mensaje cifrado";

        runGpg(keyHome,
                "--batch",
                "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--quick-generate-key", uid, "rsa2048", "encrypt", "0");

        Path publicKeyFile = keyHome.resolve("public-key.asc");
        runGpg(keyHome,
                "--batch", "--yes", "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--armor", "--output", publicKeyFile.toString(), "--export", uid);

        Path privateKeyFile = keyHome.resolve("private-key.asc");
        runGpg(keyHome,
                "--batch", "--yes", "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--armor", "--output", privateKeyFile.toString(), "--export-secret-keys", uid);

        PGPProperties properties = new PGPProperties();
        properties.setPublicKey(publicKeyFile.toString());
        properties.setPrivateKey(privateKeyFile.toString());
        properties.setPassphrase(passphrase);

        PgpService service = new PgpService(properties);
        try (InputStream encrypted = service.encrypt(message);
             InputStream decrypted = service.decrypt(encrypted)) {
            assertEquals(message, new String(decrypted.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void decrypt_shouldFail_whenPrivateKeyDoesNotMatchEncryptedContent() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");

        Path firstHome = Files.createTempDirectory("pgp-first-");
        Path secondHome = Files.createTempDirectory("pgp-second-");
        String firstUid = "owner@example.com";
        String secondUid = "wrong@example.com";
        String firstPassphrase = "OwnerPassphrase!123";
        String secondPassphrase = "WrongPassphrase!456";

        runGpg(firstHome, "--batch", "--pinentry-mode", "loopback",
                "--passphrase", firstPassphrase,
                "--quick-generate-key", firstUid, "rsa2048", "encrypt", "0");
        runGpg(secondHome, "--batch", "--pinentry-mode", "loopback",
                "--passphrase", secondPassphrase,
                "--quick-generate-key", secondUid, "rsa2048", "encrypt", "0");

        Path firstPublic = firstHome.resolve("public.asc");
        Path firstPrivate = firstHome.resolve("private.asc");
        Path secondPrivate = secondHome.resolve("private.asc");
        runGpg(firstHome, "--batch", "--yes", "--pinentry-mode", "loopback",
                "--passphrase", firstPassphrase,
                "--armor", "--output", firstPublic.toString(), "--export", firstUid);
        runGpg(firstHome, "--batch", "--yes", "--pinentry-mode", "loopback",
                "--passphrase", firstPassphrase,
                "--armor", "--output", firstPrivate.toString(), "--export-secret-keys", firstUid);
        runGpg(secondHome, "--batch", "--yes", "--pinentry-mode", "loopback",
                "--passphrase", secondPassphrase,
                "--armor", "--output", secondPrivate.toString(), "--export-secret-keys", secondUid);

        PGPProperties validProps = new PGPProperties();
        validProps.setPublicKey(firstPublic.toString());
        validProps.setPrivateKey(firstPrivate.toString());
        validProps.setPassphrase(firstPassphrase);

        PgpService validService = new PgpService(validProps);
        String encrypted = new String(validService.encrypt("mensaje secreto").readAllBytes(), StandardCharsets.UTF_8);

        PGPProperties wrongProps = new PGPProperties();
        wrongProps.setPublicKey(firstPublic.toString());
        wrongProps.setPrivateKey(secondPrivate.toString());
        wrongProps.setPassphrase(secondPassphrase);

        PgpService wrongService = new PgpService(wrongProps);
        Exception exception = assertThrows(Exception.class,
                () -> wrongService.decrypt(new java.io.ByteArrayInputStream(encrypted.getBytes(StandardCharsets.UTF_8))));

        assertNotNull(exception.getMessage());
    }

    @Test
    void encrypt_shouldFail_whenPublicKeyHasNoEncryptionKey() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");

        Path keyHome = Files.createTempDirectory("pgp-signing-key-");
        String uid = "signing-only@example.com";
        runGpg(keyHome,
                "--batch",
                "--pinentry-mode", "loopback",
                "--passphrase", "",
                "--quick-generate-key", uid, "ed25519", "sign", "0");

        Path publicKeyFile = keyHome.resolve("signing-key.asc");
        runGpg(keyHome,
                "--batch", "--yes", "--armor",
                "--output", publicKeyFile.toString(), "--export", uid);

        PGPProperties properties = new PGPProperties();
        properties.setPublicKey(publicKeyFile.toString());

        PgpService service = new PgpService(properties);
        PGPException exception = assertThrows(PGPException.class, () -> service.encrypt("test"));
        assertEquals("No encryption key found in public key input", exception.getMessage());
    }

    @Test
    void encrypt_shouldFail_whenPublicKeyCollectionIsEmpty() {
        PGPProperties properties = new PGPProperties();
        properties.setPublicKey("not-a-real-key.asc");

        PgpService service = new PgpService(properties);
        assertThrows(Exception.class, () -> service.encrypt("test-data"));
    }

    @Test
    void decrypt_shouldHandleCompressedSignedMessages() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");

        Path keyHome = Files.createTempDirectory("pgp-compressed-sign-");
        String encryptUid = "encrypt@example.com";
        String signUid = "sign@example.com";
        String passphrase = "SignedPassphrase!123";
        String message = "signed and compressed cargo";

        runGpg(keyHome,
                "--batch",
                "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--quick-generate-key", encryptUid, "rsa2048", "encrypt", "0");
        runGpg(keyHome,
                "--batch",
                "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--quick-generate-key", signUid, "rsa2048", "sign", "0");

        Path plainFile = keyHome.resolve("payload.txt");
        Files.writeString(plainFile, message, StandardCharsets.UTF_8);

        Path privateKeyFile = keyHome.resolve("private.asc");
        Path encryptedFile = keyHome.resolve("payload.asc");

        runGpg(keyHome,
                "--batch", "--yes", "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--armor", "--output", privateKeyFile.toString(), "--export-secret-keys", encryptUid);

        runGpg(keyHome,
                "--batch", "--yes", "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--compress-algo", "ZIP",
                "--local-user", signUid,
                "--sign", "--encrypt", "--recipient", encryptUid,
                "--output", encryptedFile.toString(), plainFile.toString());

        PGPProperties properties = new PGPProperties();
        properties.setPrivateKey(privateKeyFile.toString());
        properties.setPassphrase(passphrase);

        PgpService service = new PgpService(properties);
        try (InputStream encrypted = Files.newInputStream(encryptedFile);
             InputStream decrypted = service.decrypt(encrypted)) {
            assertEquals(message, new String(decrypted.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void decrypt_shouldFail_whenStreamDoesNotContainEncryptedDataList() throws Exception {
        assumeTrue(isGpgAvailable(), "gpg is required for PGP tests");

        Path keyHome = Files.createTempDirectory("pgp-invalid-stream-");
        String uid = "invalid-stream@example.com";
        String passphrase = "InvalidStreamPassphrase!123";

        runGpg(keyHome,
                "--batch",
                "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--quick-generate-key", uid, "rsa2048", "encrypt", "0");

        Path privateKeyFile = keyHome.resolve("private-key.asc");
        runGpg(keyHome,
                "--batch", "--yes", "--pinentry-mode", "loopback",
                "--passphrase", passphrase,
                "--armor", "--output", privateKeyFile.toString(), "--export-secret-keys", uid);

        PGPProperties properties = new PGPProperties();
        properties.setPrivateKey(privateKeyFile.toString());
        properties.setPassphrase(passphrase);

        PgpService service = new PgpService(properties);
        ByteArrayInputStream invalidStream = makeLiteralDataPacket("not encrypted");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.decrypt(invalidStream));
        assertTrue(exception.getMessage().contains("No encrypted data list found in PGP message"));
    }

    private static ByteArrayInputStream makeLiteralDataPacket(String content) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStream literalOutput = new PGPLiteralDataGenerator().open(
                out,
                PGPLiteralData.BINARY,
                "message.txt",
                new Date(),
                new byte[4096])) {
            literalOutput.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private static void runGpg(Path gnupgHome, String... args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command().add("gpg");
        builder.command().add("--homedir");
        builder.command().add(gnupgHome.toString());
        builder.command().addAll(java.util.List.of(args));
        builder.redirectErrorStream(true);

        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new java.io.IOException("gpg command failed: " + output);
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
}
