package io.github.goldfish07.reschiper.plugin.internal;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.jetbrains.annotations.NotNull;

public class AGP {
    public static @NotNull String getAGPVersion(@NotNull Project project) {
        String agpVersion = null;
        for (ResolvedArtifact artifact : project.getRootProject().getBuildscript()
                .getConfigurations()
                .getByName(ScriptHandler.CLASSPATH_CONFIGURATION)
                .getResolvedConfiguration()
                .getResolvedArtifacts()) {
            ResolvedModuleVersion moduleVersion = artifact.getModuleVersion();
            if (moduleVersion == null)
                continue;

            ModuleVersionIdentifier identifier = moduleVersion.getId();
            if (identifier == null)
                continue;

            if ("com.android.tools.build".equals(identifier.getGroup())
                    && "gradle".equals(identifier.getName())) {
                agpVersion = identifier.getVersion();
                break;
            }
        }
        if (agpVersion == null)
            throw new GradleException("Failed to get AGP version");
        return agpVersion;
    }
}
