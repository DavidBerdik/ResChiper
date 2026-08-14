package io.github.goldfish07.reschiper.plugin.command.extensions;

import com.android.sdklib.BuildToolInfo;
import com.android.tools.build.bundletool.androidtools.Aapt2Command;
import com.android.tools.build.bundletool.commands.BuildApksCommand;
import com.android.tools.build.bundletool.model.Password;
import com.android.tools.build.bundletool.model.SigningConfiguration;
import com.android.tools.build.bundletool.model.exceptions.CommandExecutionException;
import io.github.goldfish07.reschiper.plugin.utils.TimeClock;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The `UniversalApkPackager` class is responsible for building a universal APK based off of the obfuscated
 * AAB (Android App Bundle).
 */
public class UniversalApkPackager {
    private final Path universalApkPath;
    private final BuildToolInfo buildToolInfo;
    private final Path bundlePath;
    private final Path keystoreFile;
    private final String keyAlias;
    private final String storePassword;
    private final String keyPassword;

    /**
     * Constructs a `UniversalApkPackager` instance with the provided parameters.
     *
     * @param universalApkPath A Path object representing where the universal APK is to be stored.
     * @param buildToolInfo A BuildToolInfo object used for providing build environment info to Bundletool.
     * @param bundlePath A Path object representing where the AAB is stored.
     * @param keystoreFile A Path object representing where the keystore file is located, or {@code null} for unsigned.
     * @param keyAlias The key alias, or {@code null} for unsigned.
     * @param storePassword The keystore password, or {@code null} for unsigned.
     * @param keyPassword The key password, or {@code null} for unsigned.
     */
    public UniversalApkPackager(Path universalApkPath, BuildToolInfo buildToolInfo, Path bundlePath,
                                @Nullable Path keystoreFile, @Nullable String keyAlias,
                                @Nullable String storePassword, @Nullable String keyPassword) {
        this.universalApkPath = universalApkPath;
        this.buildToolInfo = buildToolInfo;
        this.bundlePath = bundlePath;
        this.keystoreFile = keystoreFile;
        this.keyAlias = keyAlias;
        this.storePassword = prefixPassword(storePassword);
        this.keyPassword = prefixPassword(keyPassword);
    }

    /**
     * Executes the packaging of the universal APK using the parameters provided at `UniversalApkPackager` object
     * creation time.
     */
    public void packageApk() throws Exception {
        System.out.println(
                """
                ----------------------------------------
                 Packaging Universal APK:
                ----------------------------------------
                - Building APK Set Archive (APKS) file...""");

        final TimeClock timeClock = new TimeClock();
        final Path apksPath = new File(universalApkPath.toString() + "s").toPath();
        final Path aapt2Path = new File(buildToolInfo.getPath(BuildToolInfo.PathId.AAPT2)).toPath();

        // Builds the APKS file
        BuildApksCommand.Builder command = BuildApksCommand.builder()
                .setBundlePath(bundlePath)
                .setOutputFile(apksPath)
                .setAapt2Command(Aapt2Command.createFromExecutablePath(aapt2Path))
                .setApkBuildMode(BuildApksCommand.ApkBuildMode.UNIVERSAL)
                .setOutputFormat(BuildApksCommand.OutputFormat.APK_SET);

        if (canSign(keystoreFile, keyAlias, storePassword, keyPassword)) {
            command.setSigningConfiguration(SigningConfiguration.extractFromKeystore(
                    keystoreFile,
                    keyAlias,
                    Optional.of(Password.createFromStringValue(storePassword)),
                    Optional.of(Password.createFromStringValue(keyPassword))
            ));
        }

        command.build().execute();

        /*
            The APKS file is a zip file. Since a universal APK was built, this file will contain a single APK called
            "universal.apk" that we should extract.
         */
        System.out.println("- Extracting universal APK from APKS...");
        try (ZipFile zipFile = new ZipFile(apksPath.toFile())) {
            ZipEntry zipEntry = zipFile.getEntry("universal.apk");
            if (zipEntry == null)
                throw CommandExecutionException.builder()
                        .withInternalMessage("APKS file %s does not contain universal.apk", apksPath)
                        .build();

            try (InputStream inputStream = zipFile.getInputStream(zipEntry);
                 FileOutputStream fileOutputStream = new FileOutputStream(universalApkPath.toFile())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }
        }
        Files.deleteIfExists(apksPath);
        System.out.printf("- Universal APK packaged in %s%n\n", timeClock.getElapsedTime());
    }

    static boolean canSign(@Nullable Path keystoreFile, @Nullable String keyAlias,
                           @Nullable String storePassword, @Nullable String keyPassword) {
        return keystoreFile != null
                && keystoreFile.toFile().exists()
                && keyAlias != null
                && storePassword != null
                && keyPassword != null;
    }

    private static @Nullable String prefixPassword(@Nullable String password) {
        if (password == null)
            return null;
        return "pass:" + password;
    }
}
