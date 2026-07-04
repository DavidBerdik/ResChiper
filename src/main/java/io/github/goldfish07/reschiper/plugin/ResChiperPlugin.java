package io.github.goldfish07.reschiper.plugin;

import com.android.build.api.variant.ApplicationAndroidComponentsExtension;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import io.github.goldfish07.reschiper.plugin.internal.AGP;
import io.github.goldfish07.reschiper.plugin.tasks.ResChiperTask;
import org.gradle.api.GradleException;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Plugin for integrating ResChiper into an Android Gradle project.
 */
public class ResChiperPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project project) {
        checkApplicationPlugin(project);
        project.getExtensions().create("resChiper", Extension.class);
        Object android = project.getExtensions().getByName("android");
        if (android instanceof AppExtension appExtension) {
            project.afterEvaluate(project1 -> appExtension.getApplicationVariants().all(variant -> createResChiperTask(project1, variant)));
        } else {
            ApplicationAndroidComponentsExtension androidComponents =
                    project.getExtensions().getByType(ApplicationAndroidComponentsExtension.class);
            androidComponents.onVariants(
                    androidComponents.selector().all(),
                    (Action<com.android.build.api.variant.ApplicationVariant>) variant ->
                            createResChiperTask(project, variant.getName(), variant.getBuildType())
            );
        }
    }

    /**
     * Creates a ResChiper task for the given variant.
     *
     * @param project The Gradle project.
     * @param variant The Android application variant.
     */
    private void createResChiperTask(@NotNull Project project, @NotNull ApplicationVariant variant) {
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

        resChiperTask.setVariantScope(variant);
        resChiperTask.doFirst(task -> {
            printResChiperBuildConfiguration();
            printProjectBuildConfiguration(project);
        });

        Task bundleTask = project.getTasks().getByName(bundleTaskName);
        Task bundlePackageTask = project.getTasks().getByName("package" + variantName + "Bundle");
        bundleTask.dependsOn(resChiperTask);
        resChiperTask.dependsOn(bundlePackageTask);

        String finalizeBundleTaskName = "sign" + variantName + "Bundle";
        if (project.getTasks().findByName(finalizeBundleTaskName) != null)
            resChiperTask.dependsOn(project.getTasks().getByName(finalizeBundleTaskName));
    }

    private void createResChiperTask(@NotNull Project project, @NotNull String rawVariantName, @NotNull String buildTypeName) {
        String variantName = rawVariantName.substring(0, 1).toUpperCase() + rawVariantName.substring(1);
        String bundleTaskName = "bundle" + variantName;
        String taskName = "resChiper" + variantName;
        String packageBundleTaskName = "package" + variantName + "Bundle";
        String finalizeBundleTaskName = "sign" + variantName + "Bundle";

        TaskProvider<ResChiperTask> resChiperTask = project.getTasks().register(taskName, ResChiperTask.class, task -> {
            task.setVariant(rawVariantName, buildTypeName);
            task.dependsOn(packageBundleTaskName);
            task.dependsOn(finalizeBundleTaskName);
            task.doFirst(it -> {
                printResChiperBuildConfiguration();
                printProjectBuildConfiguration(project);
            });
        });

        project.getTasks()
                .matching(task -> task.getName().equals(bundleTaskName))
                .configureEach(task -> task.dependsOn(resChiperTask));
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

    /**
     * Prints the ResChiper build configuration information.
     */
    private void printResChiperBuildConfiguration() {
        System.out.println("----------------------------------------");
        System.out.println(" ResChiper Plugin Configuration:");
        System.out.println("----------------------------------------");
        System.out.println("- ResChiper version:\t" + ResChiper.VERSION);
        System.out.println("- BundleTool version:\t" + ResChiper.BT_VERSION);
        System.out.println("- AGP version:\t\t" + ResChiper.AGP_VERSION);
        System.out.println("- Gradle Wrapper:\t" + ResChiper.GRADLE_WRAPPER_VERSION);
    }

    /**
     * Prints the project's build information.
     *
     * @param project The Android Gradle project.
     */
    private void printProjectBuildConfiguration(@NotNull Project project) {
        System.out.println("----------------------------------------");
        System.out.println(" App Build Information:");
        System.out.println("----------------------------------------");
        System.out.println("- Project name:\t\t\t" + project.getRootProject().getName());
        System.out.println("- AGP version:\t\t\t" + AGP.getAGPVersion(project));
        System.out.println("- Running Gradle version:\t" + project.getGradle().getGradleVersion());
    }
}
