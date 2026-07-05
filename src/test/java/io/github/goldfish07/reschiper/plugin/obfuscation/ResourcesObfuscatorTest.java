package io.github.goldfish07.reschiper.plugin.obfuscation;

import io.github.goldfish07.reschiper.plugin.bundle.ResourceMapping;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcesObfuscatorTest {

    @Test
    void applyWhitelistOverridesRewritesDirectoryMappingsToRawPaths() {
        ResourceMapping resourceMapping = new ResourceMapping();
        resourceMapping.putDirMapping("res/raw", "res/a");
        resourceMapping.putEntryFileMapping("base/res/raw/sample_payload.json", "res/a/b.json");

        ResourcesObfuscator.applyWhitelistOverrides(
                resourceMapping,
                Map.of("res/raw", "res/raw"),
                Map.of("base/res/raw/sample_payload.json", "res/raw/b.json"),
                Set.of()
        );

        assertEquals("res/raw", resourceMapping.getDirMapping().get("res/raw"));
        assertEquals("res/raw/b.json", resourceMapping.getEntryFilesMapping().get("base/res/raw/sample_payload.json"));
    }

    @Test
    void applyWhitelistOverridesKeepsWhitelistedResourcesFullyRaw() {
        ResourceMapping resourceMapping = new ResourceMapping();
        resourceMapping.putResourceMapping(
                "io.github.goldfish07.reschiper.sample.R.raw.sample_payload",
                "io.github.goldfish07.reschiper.sample.R.raw.a"
        );
        resourceMapping.putEntryFileMapping("base/res/raw/sample_payload.json", "res/a/b.json");

        ResourcesObfuscator.applyWhitelistOverrides(
                resourceMapping,
                Map.of(),
                Map.of("base/res/raw/sample_payload.json", "res/raw/sample_payload.json"),
                Set.of("io.github.goldfish07.reschiper.sample.R.raw.sample_payload")
        );

        assertFalse(resourceMapping.getResourceMapping().containsKey("io.github.goldfish07.reschiper.sample.R.raw.sample_payload"));
        assertEquals("res/raw/sample_payload.json", resourceMapping.getEntryFilesMapping().get("base/res/raw/sample_payload.json"));
    }

    @Test
    void rawWildcardWhitelistMatchesExtensionlessResourceFiles() {
        assertTrue(ResourcesObfuscator.isFileInWhiteList(
                "res/raw/com_android_billingclient_heterodyne_info",
                Set.of("res/raw/*")
        ));
    }
}
