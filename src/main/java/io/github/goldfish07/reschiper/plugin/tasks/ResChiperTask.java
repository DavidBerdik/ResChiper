package io.github.goldfish07.reschiper.plugin.tasks;

import io.github.goldfish07.reschiper.plugin.command.Command;
import io.github.goldfish07.reschiper.plugin.command.model.DuplicateResMergerCommand;
import io.github.goldfish07.reschiper.plugin.command.model.FileFilterCommand;
import io.github.goldfish07.reschiper.plugin.command.model.ObfuscateBundleCommand;
import io.github.goldfish07.reschiper.plugin.command.model.StringFilterCommand;
import io.github.goldfish07.reschiper.plugin.Extension;
import io.github.goldfish07.reschiper.plugin.ResChiper;
import io.github.goldfish07.reschiper.plugin.internal.BuildToolInfo;
import io.github.goldfish07.reschiper.plugin.internal.SigningConfig;
import io.github.goldfish07.reschiper.plugin.model.KeyStore;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom Gradle task for running ResChiper.
 */
public class ResChiperTask extends DefaultTask {

    private static final Logger logger = Logger.getLogger(ResChiperTask.class.getName());
    private String variantName;
    private String buildTypeName;
    private KeyStore keyStore;
    private Provider<RegularFile> bundleFile;
    private Provider<Directory> sdkDirectory;
    private String configuredBuildToolsVersion;
    private Provider<String> injectedStoreFile;
    private Provider<String> injectedStorePassword;
    private Provider<String> injectedKeyAlias;
    private Provider<String> injectedKeyPassword;
    private File buildDirectory;
    private String projectName;
    private String agpVersion;
    private String gradleVersion;
    private boolean enableObfuscation = true;
    private String obfuscationMode = "default";
    private boolean enableFileFiltering;
    private boolean enableFilterStrings;
    private boolean mergeDuplicateResources;
    private Path mappingFile;
    private String obfuscatedBundleName;
    private String unusedStringFile = "";
    private Set<String> fileFilterList = new HashSet<>();
    private Set<String> whiteList = new HashSet<>();
    private Set<String> localeWhiteList = new HashSet<>();
    private boolean buildUniversalApk;
    private String universalApkName = "universal.apk";
    private Path bundlePath;
    private Path obfuscatedBundlePath;
    private Path universalApkPath;

    /**
     * Constructor for the ResChiperTask.
     */
    public ResChiperTask() {
        setDescription("Assemble resource proguard for bundle file");
        setGroup("bundle");
        getOutputs().upToDateWhen(task -> false);
    }

    public void setVariant(String variantName) {
        this.variantName = variantName;
    }

    public void setBuildType(String buildTypeName) {
        this.buildTypeName = buildTypeName;
    }

    public void setResChiperExtension(Extension resChiperExtension) {
        this.enableObfuscation = resChiperExtension.getEnableObfuscation();
        this.obfuscationMode = resChiperExtension.getObfuscationMode();
        this.enableFileFiltering = resChiperExtension.getEnableFileFiltering();
        this.enableFilterStrings = resChiperExtension.getEnableFilterStrings();
        this.mergeDuplicateResources = resChiperExtension.getMergeDuplicateResources();
        this.mappingFile = resChiperExtension.getMappingFile();
        this.obfuscatedBundleName = resChiperExtension.getObfuscatedBundleName();
        this.unusedStringFile = resChiperExtension.getUnusedStringFile();
        this.fileFilterList = copyOf(resChiperExtension.getFileFilterList());
        this.whiteList = copyOf(resChiperExtension.getWhiteList());
        this.localeWhiteList = copyOf(resChiperExtension.getLocaleWhiteList());
        this.buildUniversalApk = resChiperExtension.getBuildUniversalApk();
        this.universalApkName = resChiperExtension.getUniversalApkName();
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public void setBundleFile(Provider<RegularFile> bundleFile) {
        this.bundleFile = bundleFile;
    }

    public void setSdkDirectory(Provider<Directory> sdkDirectory) {
        this.sdkDirectory = sdkDirectory;
    }

    public void setConfiguredBuildToolsVersion(@Nullable String configuredBuildToolsVersion) {
        this.configuredBuildToolsVersion = configuredBuildToolsVersion;
    }

    public void setInjectedSigning(
            Provider<String> injectedStoreFile,
            Provider<String> injectedStorePassword,
            Provider<String> injectedKeyAlias,
            Provider<String> injectedKeyPassword
    ) {
        this.injectedStoreFile = injectedStoreFile;
        this.injectedStorePassword = injectedStorePassword;
        this.injectedKeyAlias = injectedKeyAlias;
        this.injectedKeyPassword = injectedKeyPassword;
    }

    public void setBuildDirectory(File buildDirectory) {
        this.buildDirectory = buildDirectory;
    }

    public void setBuildConfiguration(String projectName, String agpVersion, String gradleVersion) {
        this.projectName = projectName;
        this.agpVersion = agpVersion;
        this.gradleVersion = gradleVersion;
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
        logger.log(Level.INFO, describeConfiguration());
        bundlePath = bundleFile.get().getAsFile().toPath();
        obfuscatedBundlePath = new File(bundlePath.toFile().getParentFile(), obfuscatedBundleName).toPath();
        universalApkPath = new File(bundlePath.toFile().getParentFile(), universalApkName).toPath();
        com.android.sdklib.BuildToolInfo buildToolInfo = BuildToolInfo.getBuildToolInfo(
                sdkDirectory.get().getAsFile().toPath(),
                configuredBuildToolsVersion
        );
        KeyStore resolvedKeyStore = resolveKeyStore();
        printSignConfiguration(resolvedKeyStore);
        printOutputFileLocation();
        prepareUnusedFile();
        Command.Builder builder = Command.builder();
        builder.setBundlePath(bundlePath);
        builder.setOutputPath(obfuscatedBundlePath);
        builder.setUniversalApkPath(universalApkPath);
        builder.setBuildUniversalApk(buildUniversalApk);
        builder.setBuildToolInfo(buildToolInfo);

        ObfuscateBundleCommand.Builder obfuscateBuilder = ObfuscateBundleCommand.builder()
                .setEnableObfuscate(enableObfuscation)
                .setObfuscationMode(obfuscationMode)
                .setMergeDuplicatedResources(mergeDuplicateResources)
                .setWhiteList(whiteList)
                .setFilterFile(enableFileFiltering)
                .setFileFilterRules(fileFilterList)
                .setRemoveStr(enableFilterStrings)
                .setUnusedStrPath(unusedStringFile)
                .setLanguageWhiteList(localeWhiteList);
        if (mappingFile != null)
            obfuscateBuilder.setMappingPath(mappingFile);

        if (resolvedKeyStore.storeFile() != null && resolvedKeyStore.storeFile().exists())
            builder.setStoreFile(resolvedKeyStore.storeFile().toPath())
                    .setKeyAlias(resolvedKeyStore.keyAlias())
                    .setKeyPassword(resolvedKeyStore.keyPassword())
                    .setStorePassword(resolvedKeyStore.storePassword());

        builder.setObfuscateBundleBuilder(obfuscateBuilder.build());

        FileFilterCommand.Builder fileFilterBuilder = FileFilterCommand.builder();
        fileFilterBuilder.setFileFilterRules(fileFilterList);
        builder.setFileFilterBuilder(fileFilterBuilder.build());

        StringFilterCommand.Builder stringFilterBuilder = StringFilterCommand.builder();
        builder.setStringFilterBuilder(stringFilterBuilder.build());

        DuplicateResMergerCommand.Builder duplicateResMergeBuilder = DuplicateResMergerCommand.builder();
        builder.setDuplicateResMergeBuilder(duplicateResMergeBuilder.build());

        Command command = builder.build(builder.build(), Command.TYPE.OBFUSCATE_BUNDLE);
        command.execute(Command.TYPE.OBFUSCATE_BUNDLE);
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
        if (buildTypeName != null && !buildTypeName.isBlank())
            System.out.println("- Build type:\t\t\t" + buildTypeName);
    }

    /**
     * Prepares the unused file for filtering.
     */
    private void prepareUnusedFile() {
        String simpleName = variantName.replace("Release", "");
        String name = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        String resourcePath = buildDirectory + "/outputs/mapping/" + name + "/release/unused_strings.txt";
        File usedFile = new File(resourcePath);

        if (usedFile.exists()) {
            System.out.println("find unused_strings.txt: " + usedFile.getAbsolutePath());
            if (enableFilterStrings)
                if (unusedStringFile == null || unusedStringFile.isBlank()) {
                    unusedStringFile = usedFile.getAbsolutePath();
                    logger.log(Level.SEVERE, "replace unused_strings.txt!");
                }
        } else
            logger.log(Level.SEVERE, "not exists unused_strings.txt: " + usedFile.getAbsolutePath()
                    + "\nuse default path: " + unusedStringFile);
    }

    /**
     * Re-resolves signing at execution time so Android Studio injected credentials are not
     * lost to a configuration-cache snapshot taken from an unsigned CLI build.
     */
    private @NotNull KeyStore resolveKeyStore() {
        KeyStore injected = SigningConfig.fromInjected(
                getOrNull(injectedStoreFile),
                getOrNull(injectedStorePassword),
                getOrNull(injectedKeyAlias),
                getOrNull(injectedKeyPassword)
        );
        return SigningConfig.resolve(keyStore, injected, SigningConfig.fromDebugKeystore());
    }

    private static @Nullable String getOrNull(@Nullable Provider<String> provider) {
        return provider == null ? null : provider.getOrNull();
    }

    private static @NotNull Set<String> copyOf(@Nullable Set<String> values) {
        return values == null ? new HashSet<>() : new HashSet<>(values);
    }

    private @NotNull String describeConfiguration() {
        return "-------------- Extension --------------\n" +
                "\tenableObfuscation=" + enableObfuscation + "\n" +
                "\tobfuscationMode=" + obfuscationMode + "\n" +
                "\tenableFileFiltering=" + enableFileFiltering + "\n" +
                "\tenableFilterStrings=" + enableFilterStrings + "\n" +
                "\tmergeDuplicateResources=" + mergeDuplicateResources + "\n" +
                "\tmappingFile=" + mappingFile + "\n" +
                "\tobfuscatedBundleName=" + obfuscatedBundleName + "\n" +
                "\tunusedStringFile=" + unusedStringFile + "\n" +
                "\tfileFilterList=" + fileFilterList + "\n" +
                "\tlocaleWhiteList=" + localeWhiteList + "\n" +
                "\twhiteList=" + whiteList + "\n" +
                "\tbuildUniversalApk=" + buildUniversalApk + "\n" +
                "\tuniversalApkName=" + universalApkName + "\n";
    }

    /**
     * Prints the signing configuration.
     */
    private void printSignConfiguration(@NotNull KeyStore resolvedKeyStore) {
        System.out.println("----------------------------------------");
        System.out.println(" Signing Configuration");
        System.out.println("----------------------------------------");
        System.out.println("\tKeyStoreFile:\t\t" + resolvedKeyStore.storeFile());
        System.out.println("\tKeyPassword:\t" + encrypt(resolvedKeyStore.keyPassword()));
        System.out.println("\tAlias:\t\t\t" + encrypt(resolvedKeyStore.keyAlias()));
        System.out.println("\tStorePassword:\t" + encrypt(resolvedKeyStore.storePassword()));
    }

    /**
     * Prints the output file location.
     */
    private void printOutputFileLocation() {
        System.out.println("----------------------------------------");
        System.out.println(" Output configuration");
        System.out.println("----------------------------------------");
        System.out.println("\tFolder:\t\t" + obfuscatedBundlePath.getParent());
        System.out.println("\tFile:\t\t" + obfuscatedBundlePath.getFileName());
        if (buildUniversalApk)
            System.out.println("\tUniversal APK:\t" + universalApkPath.getFileName());
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
}
