package io.github.goldfish07.reschiper.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import io.github.goldfish07.reschiper.plugin.model.KeyStore;
import io.github.goldfish07.reschiper.plugin.tasks.ResChiperTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.jetbrains.annotations.NotNull;

/**
 * Plugin for integrating ResChiper into an Android Gradle project.
 */
public class ResChiperPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project project) {
        checkApplicationPlugin(project);

        project.getPlugins().withId("com.android.application", plugin -> {
            AppExtension androidExtension = project.getExtensions().getByType(AppExtension.class);
            //AppExtension android = (AppExtension) project.getExtensions().getByName("android");
            project.getExtensions().create("resChiper", Extension.class);

            project.afterEvaluate(evaluatedProject -> {
                // Access all application variants (debug, release, etc.)
                androidExtension.getApplicationVariants().all(variant -> createResChiperTask(project, variant));
            });
        });
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

        try {
            KeyStore keyStore = new KeyStore(
                    variant.getSigningConfig().getStoreFile(),
                    variant.getSigningConfig().getStorePassword(),
                    variant.getSigningConfig().getKeyAlias(),
                    variant.getSigningConfig().getKeyPassword()
            );
            resChiperTask.setKeystore(keyStore);
        } catch (NullPointerException ignored){}

        resChiperTask.setVariantScope(variant.getName());
        resChiperTask.setProjectFields(project);

        Task bundleTask = project.getTasks().getByName(bundleTaskName);
        Task bundlePackageTask = project.getTasks().getByName("package" + variantName + "Bundle");
        bundleTask.dependsOn(resChiperTask);
        resChiperTask.dependsOn(bundlePackageTask);

        String finalizeBundleTaskName = "sign" + variantName + "Bundle";
        if (project.getTasks().findByName(finalizeBundleTaskName) != null)
            resChiperTask.dependsOn(project.getTasks().getByName(finalizeBundleTaskName));
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
}
