package com.sftp;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class SftpConfigServiceTest {

	@Test
	void sftpSessionFactory_configuresConnectionAndPrivateKeyAuthentication() throws Exception {
		SftpProperties properties = baseProperties();
		properties.setPrivateKey("C:\\keys\\private.key");
		properties.setPrivateKeyPassphrase("key-passphrase");
		properties.setPassword("unused-password");

		DefaultSftpSessionFactory factory = new SftpConfigService(properties).sftpSessionFactory();

		assertConfiguredConnection(factory, properties);
		assertEquals("key-passphrase", field(factory, "privateKeyPassphrase"));
		assertNull(field(factory, "password"));
		assertResourcePath(field(factory, "privateKey"), "C:\\keys\\private.key");
	}

	@Test
	void sftpSessionFactory_allowsPrivateKeyWithoutPassphrase() throws Exception {
		SftpProperties properties = baseProperties();
		properties.setPrivateKey("C:\\keys\\private.key");

		DefaultSftpSessionFactory factory = new SftpConfigService(properties).sftpSessionFactory();

		assertResourcePath(field(factory, "privateKey"), "C:\\keys\\private.key");
		assertNull(field(factory, "privateKeyPassphrase"));
	}

	@Test
	void sftpSessionFactory_ignoresBlankPrivateKeyPassphrase() throws Exception {
		SftpProperties properties = baseProperties();
		properties.setPrivateKey("C:\\keys\\private.key");
		properties.setPrivateKeyPassphrase("   ");

		DefaultSftpSessionFactory factory = new SftpConfigService(properties).sftpSessionFactory();

		assertResourcePath(field(factory, "privateKey"), "C:\\keys\\private.key");
		assertNull(field(factory, "privateKeyPassphrase"));
	}

	@Test
	void sftpSessionFactory_usesPasswordWhenPrivateKeyIsUnset() throws Exception {
		SftpProperties properties = baseProperties();
		properties.setPassword("login-password");

		DefaultSftpSessionFactory factory = new SftpConfigService(properties).sftpSessionFactory();

		assertConfiguredConnection(factory, properties);
		assertEquals("login-password", field(factory, "password"));
		assertNull(field(factory, "privateKey"));
	}

	@Test
	void sftpSessionFactory_ignoresBlankPasswordWhenPrivateKeyIsUnset() throws Exception {
		SftpProperties properties = baseProperties();
		properties.setPassword("   ");

		DefaultSftpSessionFactory factory = new SftpConfigService(properties).sftpSessionFactory();

		assertNull(field(factory, "password"));
		assertNull(field(factory, "privateKey"));
	}

	@Test
	void sftpSessionFactory_ignoresMissingPasswordWhenPrivateKeyIsUnset() throws Exception {
		SftpProperties properties = baseProperties();

		DefaultSftpSessionFactory factory = new SftpConfigService(properties).sftpSessionFactory();

		assertNull(field(factory, "password"));
		assertNull(field(factory, "privateKey"));
	}

	@Test
	void sftpSessionFactory_fallsBackToPasswordWhenPrivateKeyIsBlank() throws Exception {
		SftpProperties properties = baseProperties();
		properties.setPrivateKey("   ");
		properties.setPassword("login-password");

		DefaultSftpSessionFactory factory = new SftpConfigService(properties).sftpSessionFactory();

		assertEquals("login-password", field(factory, "password"));
		assertNull(field(factory, "privateKey"));
	}

	private static SftpProperties baseProperties() {
		SftpProperties properties = new SftpProperties();
		properties.setHost("sftp.example.com");
		properties.setPort(2222);
		properties.setUser("file-user");
		properties.setKnownHosts("C:\\keys\\known_hosts");
		return properties;
	}

	private static void assertConfiguredConnection(DefaultSftpSessionFactory factory, SftpProperties properties)
			throws Exception {
		assertTrue(factory.isSharedSession());
		assertEquals(properties.getHost(), field(factory, "host"));
		assertEquals(properties.getPort(), field(factory, "port"));
		assertEquals(properties.getUser(), field(factory, "user"));
		assertFalse((boolean) field(factory, "allowUnknownKeys"));
		assertResourcePath(field(factory, "knownHosts"), properties.getKnownHosts());
	}

	private static void assertResourcePath(Object resource, String expectedPath) throws Exception {
		Resource configuredResource = assertInstanceOf(Resource.class, resource);
		assertEquals(expectedPath, configuredResource.getFile().getPath());
	}

	private static Object field(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

}
