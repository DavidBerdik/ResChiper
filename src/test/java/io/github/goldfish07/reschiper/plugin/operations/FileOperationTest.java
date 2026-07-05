package io.github.goldfish07.reschiper.plugin.operations;

import com.android.tools.build.bundletool.model.ZipPath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileOperationTest {

    @Test
    void extensionlessResourceFileHasNoSuffix() {
        assertEquals("", FileOperation.getFileSuffix(ZipPath.create("res/raw/com_android_billingclient_heterodyne_info")));
    }

    @Test
    void extensionlessResourceFileUsesFullNameAsPrefix() {
        assertEquals(
                "com_android_billingclient_heterodyne_info",
                FileOperation.getFilePrefixByFileName("com_android_billingclient_heterodyne_info")
        );
    }
}
