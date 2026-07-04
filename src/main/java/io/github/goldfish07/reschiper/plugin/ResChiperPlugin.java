package io.github.goldfish07.reschiper.plugin;

import com.android.build.api.variant.ApplicationAndroidComponentsExtension;
import com.android.build.api.artifact.SingleArtifact;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import io.github.goldfish07.reschiper.plugin.internal.AGP;
import io.github.goldfish07.reschiper.plugin.internal.SigningConfig;
import io.github.goldfish07.reschiper.plugin.tasks.ResChiperTask;
import org.gradle.api.GradleException;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Plugin for integrating ResChiper into an Android Gradle project.
 */
public class ResChiperPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project project) {
        checkApplicationPlugin(project);
        Extension extension = project.getExtensions().create("resChiper", Extension.class);
        ProjectBuildConfiguration buildConfiguration = ProjectBuildConfiguration.from(project);
        Object android = project.getExtensions().getByName("android");
        if (android instanceof AppExtension appExtension) {
            project.afterEvaluate(project1 -> appExtension.getApplicationVariants().all(variant -> createResChiperTask(project1, extension, buildConfiguration, variant)));
        } else {
            ApplicationAndroidComponentsExtension androidComponents =
                    project.getExtensions().getByType(ApplicationAndroidComponentsExtension.class);
            androidComponents.onVariants(
                    androidComponents.selector().all(),
                    (Action<com.android.build.api.variant.ApplicationVariant>) variant ->
                            createResChiperTask(
                                    project,
                                    extension,
                                    buildConfiguration,
                                    variant.getName(),
                                    variant.getBuildType(),
                                    variant.getArtifacts().get(SingleArtifact.BUNDLE.INSTANCE)
                            )
            );
        }
    }

    /**
     * Creates a ResChiper task for the given variant.
     *
     * @param project The Gradle project.
     * @param variant The Android application variant.
     */
    private void createResChiperTask(
            @NotNull Project project,
            @NotNull Extension extension,
            @NotNull ProjectBuildConfiguration buildConfiguration,
            @NotNull ApplicationVariant variant
    ) {
        String variantName = variant.getName().substring(0, 1).toUpperCase() + variant.getName().substring(1);
        String bundleTaskName = "bundle" + variantName;
        if (project.getTasks().findByName(bundleTaskName) == null)
            return;
        String taskName = "resChiper" + variantName;
        ResChiperTask resChiperTask;
        if (project.getTasks().findByName(taskName) == null)
            resChiperTask = project.getTasks().create(taskName, ResChiperTask.class);
        else
            resChiperTask = (ResChiperTask) project.getTasks().getByName(taskName);

        String finalizeBundleTaskName = "sign" + variantName + "Bundle";
        if (project.getTasks().findByName(finalizeBundleTaskName) == null)
            return;
        Task finalizeBundleTask = project.getTasks().getByName(finalizeBundleTaskName);
        resChiperTask.setVariant(variant.getName());
        resChiperTask.setResChiperExtension(extension);
        resChiperTask.setKeyStore(SigningConfig.getSigningConfig(variant));
        resChiperTask.setBundleFile(getBundleFileProvider(finalizeBundleTask));
        resChiperTask.setBuildDirectory(project.getLayout().getBuildDirectory().get().getAsFile());
        resChiperTask.setBuildConfiguration(
                buildConfiguration.projectName(),
                buildConfiguration.agpVersion(),
                buildConfiguration.gradleVersion()
        );

        Task bundleTask = project.getTasks().getByName(bundleTaskName);
        Task bundlePackageTask = project.getTasks().getByName("package" + variantName + "Bundle");
        bundleTask.dependsOn(resChiperTask);
        resChiperTask.dependsOn(bundlePackageTask);
        resChiperTask.dependsOn(finalizeBundleTask);
    }

    private void createResChiperTask(
            @NotNull Project project,
            @NotNull Extension extension,
            @NotNull ProjectBuildConfiguration buildConfiguration,
            @NotNull String rawVariantName,
            @NotNull String buildTypeName,
            @NotNull Provider<RegularFile> bundleFile
    ) {
        String variantName = rawVariantName.substring(0, 1).toUpperCase() + rawVariantName.substring(1);
        String bundleTaskName = "bundle" + variantName;
        String taskName = "resChiper" + variantName;
        String packageBundleTaskName = "package" + variantName + "Bundle";
        String finalizeBundleTaskName = "sign" + variantName + "Bundle";

        TaskProvider<ResChiperTask> resChiperTask = project.getTasks().register(taskName, ResChiperTask.class, task -> {
            task.setVariant(rawVariantName);
            task.setResChiperExtension(extension);
            task.setKeyStore(SigningConfig.getSigningConfig(project, buildTypeName));
            task.setBundleFile(bundleFile);
            task.setBuildDirectory(project.getLayout().getBuildDirectory().get().getAsFile());
            task.setBuildConfiguration(
                    buildConfiguration.projectName(),
                    buildConfiguration.agpVersion(),
                    buildConfiguration.gradleVersion()
            );
            task.dependsOn(packageBundleTaskName);
            task.dependsOn(finalizeBundleTaskName);
        });

        project.getTasks()
                .matching(task -> task.getName().equals(bundleTaskName))
                .configureEach(task -> task.dependsOn(resChiperTask));
    }

    @SuppressWarnings("unchecked")
    private Provider<RegularFile> getBundleFileProvider(@NotNull Task task) {
        return (Provider<RegularFile>) task.property("finalBundleFile");
    }

    /**
     * Checks if the Android Application plugin is applied to the project.
     *
     * @param project The Gradle project.
     */
    private void checkApplicationPlugin(@NotNull Project project) {
        if (!project.getPlugins().hasPlugin("com.android.application"))
            throw new GradleException("Android Application plugin 'com.android.application' is required");
    }

    private record ProjectBuildConfiguration(String projectName, String agpVersion, String gradleVersion) {
        private static @NotNull ProjectBuildConfiguration from(@NotNull Project project) {
            return new ProjectBuildConfiguration(
                    project.getRootProject().getName(),
                    AGP.getAGPVersion(project),
                    project.getGradle().getGradleVersion()
            );
        }
    }
}
