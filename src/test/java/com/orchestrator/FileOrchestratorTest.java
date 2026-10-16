package com.orchestrator;

import com.sftp.SftpProperties;
import com.sftp.SftpService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FileOrchestratorTest {

	@Test
	void synchronizeRemoteDirectory_returnsWhenNoFilesAreFound() throws Exception {
		SftpService sftpService = mock(SftpService.class);
		when(sftpService.listRemoteFiles()).thenReturn(List.of());
		SftpProperties properties = new SftpProperties();
		properties.setRemoteDirectoryInput("/remote/input");
		FileOrchestrator orchestrator = new FileOrchestrator(sftpService, properties);

		orchestrator.synchronizeRemoteDirectory();

		verify(sftpService).listRemoteFiles();
		verify(sftpService, never()).downloadFile(anyString(), anyString(), anyString());
	}

	@Test
	void synchronizeRemoteDirectory_downloadsFilesFromRemoteDirectory() throws Exception {
		SftpService sftpService = mock(SftpService.class);
		when(sftpService.listRemoteFiles()).thenReturn(List.of("a.txt", "b.txt"));
		SftpProperties properties = new SftpProperties();
		properties.setRemoteDirectoryInput("/remote/input");
		properties.setLocalDirectory("/local");
		FileOrchestrator orchestrator = new FileOrchestrator(sftpService, properties);

		orchestrator.synchronizeRemoteDirectory();

		verify(sftpService, times(2)).listRemoteFiles();
		verify(sftpService).downloadFile("/remote/input", "/local", "a.txt");
		verify(sftpService).downloadFile("/remote/input", "/local", "b.txt");
	}

	@Test
	void synchronizeRemoteDirectory_wrapsListingIOException() throws Exception {
		SftpService sftpService = mock(SftpService.class);
		IOException cause = new IOException("listing failed");
		when(sftpService.listRemoteFiles()).thenThrow(cause);
		FileOrchestrator orchestrator = new FileOrchestrator(sftpService, new SftpProperties());

		assertThatThrownBy(orchestrator::synchronizeRemoteDirectory).isInstanceOf(RuntimeException.class)
			.hasCause(cause);
	}

	@Test
	void synchronizeRemoteDirectory_wrapsDownloadIOException() throws Exception {
		SftpService sftpService = mock(SftpService.class);
		when(sftpService.listRemoteFiles()).thenReturn(List.of("a.txt"));
		IOException cause = new IOException("download failed");
		doThrow(cause).when(sftpService).downloadFile("/remote/input", "/local", "a.txt");
		SftpProperties properties = new SftpProperties();
		properties.setRemoteDirectoryInput("/remote/input");
		properties.setLocalDirectory("/local");
		FileOrchestrator orchestrator = new FileOrchestrator(sftpService, properties);

		assertThatThrownBy(orchestrator::synchronizeRemoteDirectory).isInstanceOf(RuntimeException.class)
			.hasCause(cause);
	}

	@Test
	void write_usesConfiguredRemoteDirectory() throws Exception {
		SftpService sftpService = mock(SftpService.class);
		SftpProperties sftpProperties = new SftpProperties();
		sftpProperties.setRemoteDirectoryOutput("/remote/output");

		FileOrchestrator orchestrator = new FileOrchestrator(sftpService, sftpProperties);

		orchestrator.write("orders.csv", "payload");

		verify(sftpService).write("payload", "/remote/output/orders.csv");
	}

	@Test
	void write_usesOverrideDirectory_whenProvided() throws Exception {
		SftpService sftpService = mock(SftpService.class);
		SftpProperties sftpProperties = new SftpProperties();
		sftpProperties.setRemoteDirectoryOutput("/remote/output");

		FileOrchestrator orchestrator = new FileOrchestrator(sftpService, sftpProperties);

		orchestrator.write("orders.csv", "payload", "/custom");

		verify(sftpService).write("payload", "/custom/orders.csv");
	}

	@Test
	void write_wrapsExceptionsFromSftpService() throws Exception {
		SftpService sftpService = mock(SftpService.class);
		SftpProperties sftpProperties = new SftpProperties();
		sftpProperties.setRemoteDirectoryOutput("/remote/output");
		doThrow(new RuntimeException("boom")).when(sftpService).write("payload", "/remote/output/orders.csv");

		FileOrchestrator orchestrator = new FileOrchestrator(sftpService, sftpProperties);

		assertThatThrownBy(() -> orchestrator.write("orders.csv", "payload"))
			.isInstanceOf(FileOrchestratorException.class)
			.hasMessageContaining("Error occurred while writing file");
	}

	@Test
	void write_withExplicitDirectory_wrapsExceptionsFromSftpService() throws Exception {
		SftpService sftpService = mock(SftpService.class);
		SftpProperties sftpProperties = new SftpProperties();
		sftpProperties.setRemoteDirectoryOutput("/remote/output");
		doThrow(new RuntimeException("boom")).when(sftpService).write("payload", "/custom/orders.csv");

		FileOrchestrator orchestrator = new FileOrchestrator(sftpService, sftpProperties);

		assertThatThrownBy(() -> orchestrator.write("orders.csv", "payload", "/custom"))
			.isInstanceOf(FileOrchestratorException.class)
			.hasMessageContaining("Error occurred while writing file");
	}

}
