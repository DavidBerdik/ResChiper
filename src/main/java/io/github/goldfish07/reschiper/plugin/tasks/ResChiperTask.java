package io.github.goldfish07.reschiper.plugin.tasks;

import io.github.goldfish07.reschiper.plugin.ResChiper;
import io.github.goldfish07.reschiper.plugin.command.Command;
import io.github.goldfish07.reschiper.plugin.command.model.DuplicateResMergerCommand;
import io.github.goldfish07.reschiper.plugin.command.model.FileFilterCommand;
import io.github.goldfish07.reschiper.plugin.command.model.ObfuscateBundleCommand;
import io.github.goldfish07.reschiper.plugin.command.model.StringFilterCommand;
import io.github.goldfish07.reschiper.plugin.Extension;
import io.github.goldfish07.reschiper.plugin.internal.AGP;
import io.github.goldfish07.reschiper.plugin.internal.BuildToolInfo;
import io.github.goldfish07.reschiper.plugin.model.KeyStore;
import io.github.goldfish07.reschiper.plugin.internal.Bundle;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom Gradle task for running ResChiper.
 */
@CacheableTask
public class ResChiperTask extends DefaultTask {

    private static final Logger logger = Logger.getLogger(ResChiperTask.class.getName());
    private final Extension resChiperExtension = (Extension) getProject().getExtensions().getByName("resChiper");
    private final RegularFileProperty storeFile = getProject().getObjects().fileProperty();
    private final Property<String> keyAlias = getProject().getObjects().property(String.class);
    private final Property<String> keyPassword = getProject().getObjects().property(String.class);
    private final Property<String> storePassword = getProject().getObjects().property(String.class);
    private final DirectoryProperty buildTools = getProject().getObjects().directoryProperty();
    private final RegularFileProperty bundlePath = getProject().getObjects().fileProperty();
    private final RegularFileProperty obfuscatedBundlePath = getProject().getObjects().fileProperty();
    private final RegularFileProperty universalApkPath = getProject().getObjects().fileProperty();
    private String variant;
    private String projectName;
    private String agpVersion;
    private String gradleVersion;
    private com.android.sdklib.BuildToolInfo buildToolInfo;
    private File buildDir;

    /**
     * Constructor for the ResChiperTask.
     */
    public ResChiperTask() {
        setDescription("Assemble resource proguard for bundle file");
        setGroup("bundle");
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Sets the variant scope for the task.
     *
     * @param variant The ApplicationVariant for the Android application.
     */
    public void setVariantScope(String variant) {
        this.variant = variant;
        bundlePath.set(Bundle.getBundleFilePath(getProject(), variant).toFile());
        obfuscatedBundlePath.set(new File(bundlePath.get().getAsFile().getParentFile(), resChiperExtension.getObfuscatedBundleName()));
        universalApkPath.set(new File(bundlePath.get().getAsFile().getParentFile(), resChiperExtension.getUniversalApkName()));
        buildTools.set(BuildToolInfo.getBuildToolInfo(getProject()).getLocation().toFile());
    }

    public void setProjectFields(Project project) {
        projectName = project.getRootProject().getName();
        agpVersion = AGP.getAGPVersion(project);
        gradleVersion = project.getGradle().getGradleVersion();
        buildToolInfo = BuildToolInfo.getBuildToolInfo(project);
        buildDir = project.getBuildDir();
    }

    public void setKeystore(KeyStore keyStore) {
        storeFile.set(keyStore.storeFile());
        keyAlias.set(keyStore.keyAlias());
        keyPassword.set(keyStore.keyPassword());
        storePassword.set(keyStore.storePassword());
    }

    /**
     * Executes the ResChiperTask.
     *
     * @throws Exception If an error occurs during execution.
     */
    @TaskAction
    public void execute() throws Exception {
        printResChiperBuildConfiguration();
        printProjectBuildConfiguration();

        logger.log(Level.INFO, resChiperExtension.toString());
        printSignConfiguration();
        printOutputFileLocation();
        prepareUnusedFile();
        Command.Builder builder = Command.builder();
        builder.setBundlePath(bundlePath.get().getAsFile().toPath());
        builder.setOutputPath(obfuscatedBundlePath.get().getAsFile().toPath());
        builder.setUniversalApkPath(universalApkPath.get().getAsFile().toPath());
        builder.setBuildUniversalApk(resChiperExtension.getBuildUniversalApk());
        builder.setBuildToolInfo(buildToolInfo);

        ObfuscateBundleCommand.Builder obfuscateBuilder = ObfuscateBundleCommand.builder()
                .setEnableObfuscate(resChiperExtension.getEnableObfuscation())
                .setObfuscationMode(resChiperExtension.getObfuscationMode())
                .setMergeDuplicatedResources(resChiperExtension.getMergeDuplicateResources())
                .setWhiteList(resChiperExtension.getWhiteList())
                .setFilterFile(resChiperExtension.getEnableFileFiltering())
                .setFileFilterRules(resChiperExtension.getFileFilterList())
                .setRemoveStr(resChiperExtension.getEnableFilterStrings())
                .setUnusedStrPath(resChiperExtension.getUnusedStringFile())
                .setLanguageWhiteList(resChiperExtension.getLocaleWhiteList());
        if (resChiperExtension.getMappingFile() != null)
            obfuscateBuilder.setMappingPath(resChiperExtension.getMappingFile());

        builder.setStoreFile(storeFile.get().getAsFile().toPath())
                .setKeyAlias(keyAlias.get())
                .setKeyPassword(keyPassword.get())
                .setStorePassword(storePassword.get());

        builder.setObfuscateBundleBuilder(obfuscateBuilder.build());

        FileFilterCommand.Builder fileFilterBuilder = FileFilterCommand.builder();
        fileFilterBuilder.setFileFilterRules(resChiperExtension.getFileFilterList());
        builder.setFileFilterBuilder(fileFilterBuilder.build());

        StringFilterCommand.Builder stringFilterBuilder = StringFilterCommand.builder();
        builder.setStringFilterBuilder(stringFilterBuilder.build());

        DuplicateResMergerCommand.Builder duplicateResMergeBuilder = DuplicateResMergerCommand.builder();
        builder.setDuplicateResMergeBuilder(duplicateResMergeBuilder.build());

        Command command = builder.build(builder.build(), Command.TYPE.OBFUSCATE_BUNDLE);
        command.execute(Command.TYPE.OBFUSCATE_BUNDLE);
    }

    /**
     * Prepares the unused file for filtering.
     */
    private void prepareUnusedFile() {
        String simpleName = variant.replace("Release", "");
        String name = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        String resourcePath = buildDir + "/outputs/mapping/" + name + "/release/unused_strings.txt";
        File usedFile = new File(resourcePath);

        if (usedFile.exists()) {
            System.out.println("find unused_strings.txt: " + usedFile.getAbsolutePath());
            if (resChiperExtension.getEnableFilterStrings())
                if (resChiperExtension.getUnusedStringFile() == null || resChiperExtension.getUnusedStringFile().isBlank()) {
                    resChiperExtension.setUnusedStringFile(usedFile.getAbsolutePath());
                    logger.log(Level.SEVERE, "replace unused_strings.txt!");
                }
        } else
            logger.log(Level.SEVERE, "not exists unused_strings.txt: " + usedFile.getAbsolutePath()
                    + "\nuse default path: " + resChiperExtension.getUnusedStringFile());
    }

    /**
     * Prints the signing configuration.
     */
    private void printSignConfiguration() {
        System.out.println("----------------------------------------");
        System.out.println(" Signing Configuration");
        System.out.println("----------------------------------------");
        System.out.println("\tKeyStoreFile:\t\t" + storeFile.get().getAsFile());
        System.out.println("\tKeyPassword:\t" + encrypt(keyPassword.get()));
        System.out.println("\tAlias:\t\t\t" + encrypt(keyAlias.get()));
        System.out.println("\tStorePassword:\t" + encrypt(storePassword.get()));
    }

    /**
     * Prints the output file location.
     */
    private void printOutputFileLocation() {
        System.out.println("----------------------------------------");
        System.out.println(" Output configuration");
        System.out.println("----------------------------------------");
        System.out.println("\tFolder:\t\t" + obfuscatedBundlePath.get().getAsFile().getParentFile());
        System.out.println("\tFile:\t\t" + obfuscatedBundlePath.get().getAsFile().getName());
        System.out.println("----------------------------------------");
    }

    /**
     * Encrypts a value for printing (partially).
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    private @NotNull String encrypt(String value) {
        if (value == null)
            return "/";
        if (value.length() > 2)
            return value.substring(0, value.length() / 2) + "****";
        return "****";
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
     */
    private void printProjectBuildConfiguration() {
        System.out.println("----------------------------------------");
        System.out.println(" App Build Information:");
        System.out.println("----------------------------------------");
        System.out.println("- Project name:\t\t\t" + projectName);
        System.out.println("- AGP version:\t\t\t" + agpVersion);
        System.out.println("- Running Gradle version:\t" + gradleVersion);
    }
}
