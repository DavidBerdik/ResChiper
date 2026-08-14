package io.github.goldfish07.reschiper.plugin.command.extensions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversalApkPackagerTest {

    @TempDir
    Path tempDir;

    @Test
    void nullKeystoreDoesNotThrowAndCannotSign() {
        assertFalse(UniversalApkPackager.canSign(null, null, null, null));
        assertFalse(UniversalApkPackager.canSign(null, "alias", "store", "key"));
    }

    @Test
    void missingKeystoreFileCannotSign() {
        Path missing = tempDir.resolve("missing.jks");

        assertFalse(UniversalApkPackager.canSign(missing, "alias", "store", "key"));
    }

    @Test
    void existingKeystoreWithCredentialsCanSign() throws IOException {
        File storeFile = tempDir.resolve("release.jks").toFile();
        assertTrue(storeFile.createNewFile());

        assertTrue(UniversalApkPackager.canSign(storeFile.toPath(), "alias", "store", "key"));
    }

    @Test
    void existingKeystoreWithoutCredentialsCannotSign() throws IOException {
        File storeFile = tempDir.resolve("release.jks").toFile();
        assertTrue(storeFile.createNewFile());

        assertFalse(UniversalApkPackager.canSign(storeFile.toPath(), null, "store", "key"));
        assertFalse(UniversalApkPackager.canSign(storeFile.toPath(), "alias", null, "key"));
        assertFalse(UniversalApkPackager.canSign(storeFile.toPath(), "alias", "store", null));
    }
}
