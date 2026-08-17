package io.github.goldfish07.reschiper.plugin.internal;

import com.android.SdkConstants;
import com.android.repository.Revision;
import org.gradle.api.GradleException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BuildToolInfo {
    public static @NotNull com.android.sdklib.BuildToolInfo getBuildToolInfo(
            @NotNull Path sdkDirectory,
            @Nullable String configuredBuildToolsVersion
    ) {
        Path buildToolsRoot = sdkDirectory.resolve(SdkConstants.FD_BUILD_TOOLS);
        String version = resolveBuildToolsVersion(configuredBuildToolsVersion, buildToolsRoot);
        Path buildToolsDir = buildToolsRoot.resolve(version);

        com.android.sdklib.BuildToolInfo buildToolInfo =
                com.android.sdklib.BuildToolInfo.fromStandardDirectoryLayout(
                        Revision.parseRevision(version),
                        buildToolsDir
                );

        String aapt2 = buildToolInfo.getPath(com.android.sdklib.BuildToolInfo.PathId.AAPT2);
        Path aapt2Path = aapt2 == null ? buildToolsDir.resolve("aapt2") : Paths.get(aapt2);
        if (!Files.isRegularFile(aapt2Path))
            throw new GradleException("aapt2 was not found at " + aapt2Path);

        return buildToolInfo;
    }

    private static @NotNull String resolveBuildToolsVersion(
            @Nullable String configuredBuildToolsVersion,
            @NotNull Path buildToolsRoot
    ) {
        if (configuredBuildToolsVersion != null && !configuredBuildToolsVersion.isBlank())
            return configuredBuildToolsVersion.trim();
        String highest = highestInstalledBuildToolsVersion(buildToolsRoot);
        if (highest != null)
            return highest;
        throw new GradleException("No Android build-tools installation was found in " + buildToolsRoot);
    }

    static @Nullable String highestInstalledBuildToolsVersion(@NotNull Path buildToolsRoot) {
        File[] children = buildToolsRoot.toFile().listFiles(File::isDirectory);
        if (children == null || children.length == 0)
            return null;
        Revision highest = null;
        String highestName = null;
        for (File child : children) {
            try {
                Revision revision = Revision.parseRevision(child.getName());
                if (highest == null || revision.compareTo(highest) > 0) {
                    highest = revision;
                    highestName = child.getName();
                }
            } catch (Exception ignored) {
                // Skip directories that are not build-tools revisions.
            }
        }
        return highestName;
    }
}
