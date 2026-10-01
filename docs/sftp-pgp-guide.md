### SFTP package

The SFTP package provides a different methods to connect in a remote server, read file, read a file from a specific folder, read a encrypted file, read a encrypted file from a specific folder.

#### Features

- [Connect to Server using sftp protocol.](#Read-a-file-stored-to-an-SFTP-server)
- [Read a specific file and decrypted (optional).](#Read-a-file-stored-to-an-SFTP-server)
- [Read a file and decrypted (optional) from a specific directory.](#Read-a-file-stored-to-an-SFTP-server)
- [Write a file and encrypted (optional).](#Write-a-file-to-an-SFTP-server)

--- 
### PGP package

The PGP package provides different methods to encrypt and decrypt a file.

#### Features

- [Decrypted a file.](#Decrypt-a-file-using-PGP-Service)
- [Encrypted a file.](#Encrypt-a-file-using-PGP-Service)

---
## Quick Start Guide

### Application Properties

`Application.properties`: Add properties to connect to an SFTP server, and encrypt or decrypt a file if necessary.

```properties
# ---------- SFTP Configuration ----------
sftp.host={HOST}
sftp.port={PORT}
sftp.user={USER}
sftp.password={PASSWORD}

# -- this two properties are to read and write a file in a specific directory
sftp.remote-directory-input={INPUT-DIRECTORY}
sftp.remote-directory-output={OUTPUT-DIRECTORY}

# -- uncomment this line if you want to read with specific extension, 
# the default extension is .gpg 
# sftp.file-extension = {FILE-EXTENSION}

# -- Uncomment this line if you want to connect using private key authentication
# sftp.private-key-path={PATH}
# sftp.private-key-passphrase={YOUR-PASSPHRASE}

# ---------- PGP Configuration ----------
pgp.passphrase={YOUR-PASSPHRASE}
pgp.private-key={PRIVATE-KEY-PATH}
pgp.public-key={PUBLIC-KEY-PATH}

# -- Uncomment this line if you want to enable pgp service to encrypt 
# or decrypt a file in the sftp service. By default, the value is false
# pgp.enabled={true | false}
```
---

## Read a file stored to an SFTP server
Previously you need to add the sftp configuration: [SFTP Configuration](#application-properties)
```java
import com.sftp.SftpService;

//Inject the SftpService into your class
private final SftpService sftpService;

// Connect to a SFTP server
sftpService.openSftpSession();

// Reading, decrypting and store a file in BufferedReader variable
// Reads a file from the SFTP server. If the file is encrypted, 
// It will be decrypted before reading
// pgp.enable needs to be true to decrypt a file.
// @inputFile the path to the input file on the SFTP server
sftpService.readFile(inputFile);

// Reads the first file in the specified remote directory that matches 
// the encrypted file extension.
// If the file is encrypted, it will be decrypted before reading.
// pgp.enable needs to be true to decrypt a file.
// The default value for an extension is .gpg, 
// but it can be changed in the application.properties
// sftp.file-extension={YOUR-PASSPHRASE}
sftpService.readFirstFile();

// Reads a line from the currently opened SFTP file reader.
sftpService.getRowLine();

// Closes the currently opened SFTP file reader.
sftpService.closeReader();

//Closes the currently opened SFTP session.
sftpService.closeSession();
```

---
## Write a file to an SFTP server
Previously you need to add the sftp configuration: [SFTP Configuration](#application-properties)
```java
import com.sftp.SftpService;

//Inject the SftpService into your class
private final SftpService sftpService;

// Connect to a SFTP server
sftpService.openSftpSession();

// Initializes the output stream for writing data to the SFTP server.
sftpService. setOutputStream();

// Writes the provided binary data to the output stream.
// @data: the binary data to write
sftpService.setInputStream(data);

// Closes the output stream and writes 
// its contents to the specified remote directory and file on the SFTP server.
// @fileName the name of the file to write to
sftpService.write(fileName);

// Closes the currently opened SFTP session.
sftpService.closeSession();
```

---

## Decrypt a file using PGP Service
Previously you need to add the PGP configuration: [PGP Configuration](#application-properties)
```java
import com.pgp.PgpService;

//Inject the PgpService into your class
private final PgpService pgpService;

// encryptedStream  InputStream containing the PGP encrypted data
// privateKeyStream InputStream containing the PGP private key
// passphrase       Passphrase for the private key
// @return          InputStream containing the decrypted data
pgpService.decryptFile(encryptedStream, privateKeyStream, passphrase);
```
---

## Encrypt a file using PGP Service
Previously you need to add the PGP configuration: [PGP Configuration](#application-properties)
```java
import com.pgp.PgpService;

//Inject the PgpService into your class
private final PgpService pgpService;

// @plainText        InputStream containing the plain text data
// @ppublicKeyInput  InputStream containing the PGP public key
// @return           InputStream containing the encrypted data
pgpService.encryptFile(plainText, ppublicKeyInput);
```
---