package io.github.goldfish07.reschiper.plugin.internal;

import io.github.goldfish07.reschiper.plugin.model.KeyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigningConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void usableDslConfigWinsOverInjectedAndDebug() throws IOException {
        KeyStore dsl = usableKeyStore("dsl");
        KeyStore injected = usableKeyStore("injected");
        KeyStore debug = usableKeyStore("debug");

        assertSame(dsl, SigningConfig.resolve(dsl, injected, debug));
    }

    @Test
    void injectedConfigWinsWhenDslIsUnusable() throws IOException {
        KeyStore injected = usableKeyStore("injected");
        KeyStore debug = usableKeyStore("debug");

        assertSame(injected, SigningConfig.resolve(SigningConfig.empty(), injected, debug));
    }

    @Test
    void debugConfigWinsWhenDslAndInjectedAreUnusable() throws IOException {
        KeyStore debug = usableKeyStore("debug");

        assertSame(debug, SigningConfig.resolve(SigningConfig.empty(), SigningConfig.empty(), debug));
    }

    @Test
    void emptyWhenNoUsableConfigExists() {
        KeyStore resolved = SigningConfig.resolve(SigningConfig.empty(), SigningConfig.empty(), SigningConfig.empty());

        assertFalse(SigningConfig.isUsable(resolved));
        assertNull(resolved.storeFile());
    }

    @Test
    void missingStoreFileIsNotUsable() {
        KeyStore missing = new KeyStore(tempDir.resolve("missing.jks").toFile(), "store", "alias", "key");

        assertFalse(SigningConfig.isUsable(missing));
    }

    @Test
    void existingStoreFileWithCredentialsIsUsable() throws IOException {
        assertTrue(SigningConfig.isUsable(usableKeyStore("release")));
    }

    @Test
    void blankPasswordIsNotUsable() throws IOException {
        File storeFile = tempDir.resolve("blank.jks").toFile();
        assertTrue(storeFile.createNewFile());

        assertFalse(SigningConfig.isUsable(new KeyStore(storeFile, " ", "alias", "key")));
    }

    @Test
    void fromInjectedReturnsEmptyWhenAnyValueIsMissing() throws IOException {
        File storeFile = tempDir.resolve("injected.jks").toFile();
        assertTrue(storeFile.createNewFile());
        String path = storeFile.getAbsolutePath();

        assertFalse(SigningConfig.isUsable(SigningConfig.fromInjected(null, "store", "alias", "key")));
        assertFalse(SigningConfig.isUsable(SigningConfig.fromInjected(path, null, "alias", "key")));
        assertFalse(SigningConfig.isUsable(SigningConfig.fromInjected(path, "store", null, "key")));
        assertFalse(SigningConfig.isUsable(SigningConfig.fromInjected(path, "store", "alias", null)));
        assertFalse(SigningConfig.isUsable(SigningConfig.fromInjected(path, " ", "alias", "key")));
    }

    @Test
    void fromInjectedReturnsUsableKeyStoreWhenAllValuesArePresent() throws IOException {
        File storeFile = tempDir.resolve("injected.jks").toFile();
        assertTrue(storeFile.createNewFile());

        KeyStore injected = SigningConfig.fromInjected(
                storeFile.getAbsolutePath(),
                "store-pass",
                "injected-alias",
                "key-pass"
        );

        assertTrue(SigningConfig.isUsable(injected));
        assertEquals(storeFile, injected.storeFile());
        assertEquals("injected-alias", injected.keyAlias());
    }

    private KeyStore usableKeyStore(String name) throws IOException {
        File storeFile = tempDir.resolve(name + ".jks").toFile();
        assertTrue(storeFile.createNewFile());
        return new KeyStore(storeFile, "store-pass", name + "-alias", "key-pass");
    }
}
