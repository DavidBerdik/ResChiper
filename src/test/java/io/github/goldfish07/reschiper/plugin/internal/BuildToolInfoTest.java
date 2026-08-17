package io.github.goldfish07.reschiper.plugin.internal;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildToolInfoTest {

    @TempDir
    Path tempDir;

    @Test
    void highestInstalledBuildToolsVersionPicksNewestRevision() throws IOException {
        Path buildToolsRoot = tempDir.resolve("build-tools");
        Files.createDirectories(buildToolsRoot.resolve("34.0.0"));
        Files.createDirectories(buildToolsRoot.resolve("35.0.0"));
        Files.createDirectories(buildToolsRoot.resolve("not-a-revision"));

        assertEquals("35.0.0", BuildToolInfo.highestInstalledBuildToolsVersion(buildToolsRoot));
    }

    @Test
    void highestInstalledBuildToolsVersionReturnsNullWhenMissing() {
        assertNull(BuildToolInfo.highestInstalledBuildToolsVersion(tempDir.resolve("build-tools")));
    }

    @Test
    void configuredVersionIsPreferredOverHighestInstalled() throws IOException {
        Path sdk = createSdkWithBuildTools("34.0.0", "35.0.0");

        com.android.sdklib.BuildToolInfo info = BuildToolInfo.getBuildToolInfo(sdk, "34.0.0");

        assertEquals("34.0.0", info.getRevision().toString());
        assertTrue(info.getLocation().endsWith(Path.of("build-tools", "34.0.0")));
    }

    @Test
    void highestInstalledVersionIsUsedWhenConfiguredVersionIsBlank() throws IOException {
        Path sdk = createSdkWithBuildTools("34.0.0", "35.0.0");

        com.android.sdklib.BuildToolInfo info = BuildToolInfo.getBuildToolInfo(sdk, "  ");

        assertEquals("35.0.0", info.getRevision().toString());
    }

    @Test
    void missingBuildToolsInstallationThrows() {
        GradleException exception = assertThrows(
                GradleException.class,
                () -> BuildToolInfo.getBuildToolInfo(tempDir, null)
        );

        assertTrue(exception.getMessage().contains("No Android build-tools installation was found"));
    }

    @Test
    void missingAapt2Throws() throws IOException {
        Path buildToolsDir = tempDir.resolve("build-tools").resolve("34.0.0");
        Files.createDirectories(buildToolsDir);

        GradleException exception = assertThrows(
                GradleException.class,
                () -> BuildToolInfo.getBuildToolInfo(tempDir, "34.0.0")
        );

        assertTrue(exception.getMessage().contains("aapt2 was not found"));
    }

    private Path createSdkWithBuildTools(String... versions) throws IOException {
        for (String version : versions) {
            Path buildToolsDir = tempDir.resolve("build-tools").resolve(version);
            Files.createDirectories(buildToolsDir);
            Files.createFile(buildToolsDir.resolve("aapt2"));
            Files.createFile(buildToolsDir.resolve("aapt2.exe"));
        }
        return tempDir;
    }
}
