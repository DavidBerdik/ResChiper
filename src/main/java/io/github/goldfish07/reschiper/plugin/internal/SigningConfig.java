package io.github.goldfish07.reschiper.plugin.internal;

import com.android.build.api.dsl.ApplicationBuildType;
import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.dsl.ApkSigningConfig;
import com.android.build.gradle.api.ApplicationVariant;
import io.github.goldfish07.reschiper.plugin.android.AndroidDebugKeyStoreHelper;
import io.github.goldfish07.reschiper.plugin.android.JarSigner;
import io.github.goldfish07.reschiper.plugin.model.KeyStore;
import org.gradle.api.Project;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class SigningConfig {
    public static final String INJECTED_STORE_FILE = "android.injected.signing.store.file";
    public static final String INJECTED_STORE_PASSWORD = "android.injected.signing.store.password";
    public static final String INJECTED_KEY_ALIAS = "android.injected.signing.key.alias";
    public static final String INJECTED_KEY_PASSWORD = "android.injected.signing.key.password";

    @Contract(" -> new")
    public static @NotNull KeyStore empty() {
        return new KeyStore(null, null, null, null);
    }

    /**
     * Returns the DSL signing config for a build type. Injected Android Studio properties and the
     * debug keystore are resolved later at task execution time.
     */
    @Contract("_, _ -> new")
    public static @NotNull KeyStore getSigningConfig(@NotNull Project project, @Nullable String buildTypeName) {
        return fromBuildType(project, buildTypeName);
    }

    /**
     * Returns the DSL signing config for a legacy application variant. Injected Android Studio
     * properties and the debug keystore are resolved later at task execution time.
     */
    @Contract("_, _ -> new")
    public static @NotNull KeyStore getSigningConfig(@NotNull Project project, @NotNull ApplicationVariant variant) {
        return fromVariant(variant);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull KeyStore resolve(@Nullable KeyStore dslOrEmpty, @Nullable KeyStore injected, @Nullable KeyStore debug) {
        if (isUsable(dslOrEmpty))
            return dslOrEmpty;
        if (isUsable(injected))
            return injected;
        if (isUsable(debug))
            return debug;
        return empty();
    }

    public static boolean isUsable(@Nullable KeyStore keyStore) {
        return keyStore != null
                && keyStore.storeFile() != null
                && keyStore.storeFile().exists()
                && !isBlank(keyStore.storePassword())
                && !isBlank(keyStore.keyAlias())
                && !isBlank(keyStore.keyPassword());
    }

    /**
     * Builds a keystore from Android Studio injected signing properties. Returns {@link #empty()}
     * when any value is missing or blank.
     */
    @Contract("_, _, _, _ -> new")
    public static @NotNull KeyStore fromInjected(
            @Nullable String storeFile,
            @Nullable String storePassword,
            @Nullable String keyAlias,
            @Nullable String keyPassword
    ) {
        if (isBlank(storeFile) || isBlank(storePassword) || isBlank(keyAlias) || isBlank(keyPassword))
            return empty();
        return new KeyStore(new File(storeFile), storePassword, keyAlias, keyPassword);
    }

    public static @NotNull KeyStore fromDebugKeystore() {
        JarSigner.Signature debug = AndroidDebugKeyStoreHelper.debugSigningConfig();
        if (debug == null || debug.storeFile() == null)
            return empty();
        return new KeyStore(
                debug.storeFile().toFile(),
                debug.storePassword(),
                debug.keyAlias(),
                debug.keyPassword()
        );
    }

    private static @NotNull KeyStore fromBuildType(@NotNull Project project, @Nullable String buildTypeName) {
        if (isBlank(buildTypeName))
            return empty();
        try {
            ApplicationExtension android = project.getExtensions().getByType(ApplicationExtension.class);
            ApplicationBuildType buildType = android.getBuildTypes().getByName(buildTypeName);
            return fromApkSigningConfig(buildType.getSigningConfig());
        } catch (Exception ignored) {
            return empty();
        }
    }

    private static @NotNull KeyStore fromVariant(@NotNull ApplicationVariant variant) {
        try {
            if (variant.getSigningConfig() == null)
                return empty();
            return new KeyStore(
                    variant.getSigningConfig().getStoreFile(),
                    variant.getSigningConfig().getStorePassword(),
                    variant.getSigningConfig().getKeyAlias(),
                    variant.getSigningConfig().getKeyPassword()
            );
        } catch (Exception ignored) {
            return empty();
        }
    }

    private static @NotNull KeyStore fromApkSigningConfig(@Nullable ApkSigningConfig signingConfig) {
        if (signingConfig == null)
            return empty();
        return new KeyStore(
                signingConfig.getStoreFile(),
                signingConfig.getStorePassword(),
                signingConfig.getKeyAlias(),
                signingConfig.getKeyPassword()
        );
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
