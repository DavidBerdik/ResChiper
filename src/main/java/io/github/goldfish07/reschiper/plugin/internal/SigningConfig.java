package io.github.goldfish07.reschiper.plugin.internal;

import com.android.build.api.dsl.ApplicationBuildType;
import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.dsl.ApkSigningConfig;
import com.android.build.gradle.api.ApplicationVariant;
import io.github.goldfish07.reschiper.plugin.android.AndroidDebugKeyStoreHelper;
import io.github.goldfish07.reschiper.plugin.android.JarSigner;
import io.github.goldfish07.reschiper.plugin.model.KeyStore;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
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
     * Resolves signing for a build type: DSL config, then Android Studio injected properties,
     * then the debug keystore. Returns {@link #empty()} only when none of those exist.
     */
    @Contract("_, _ -> new")
    public static @NotNull KeyStore getSigningConfig(@NotNull Project project, @Nullable String buildTypeName) {
        return resolve(project, fromBuildType(project, buildTypeName));
    }

    /**
     * Resolves signing for a legacy application variant using the same fallback order as
     * {@link #getSigningConfig(Project, String)}.
     */
    @Contract("_, _ -> new")
    public static @NotNull KeyStore getSigningConfig(@NotNull Project project, @NotNull ApplicationVariant variant) {
        return resolve(project, fromVariant(variant));
    }

    static @NotNull KeyStore resolve(@NotNull Project project, @Nullable KeyStore dslOrEmpty) {
        return resolve(dslOrEmpty, fromInjectedProperties(project), fromDebugKeystore());
    }

    static @NotNull KeyStore resolve(@Nullable KeyStore dslOrEmpty, @Nullable KeyStore injected, @Nullable KeyStore debug) {
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

    private static @NotNull KeyStore fromInjectedProperties(@NotNull Project project) {
        String storeFile = readProperty(project, INJECTED_STORE_FILE);
        String storePassword = readProperty(project, INJECTED_STORE_PASSWORD);
        String keyAlias = readProperty(project, INJECTED_KEY_ALIAS);
        String keyPassword = readProperty(project, INJECTED_KEY_PASSWORD);
        if (isBlank(storeFile) || isBlank(storePassword) || isBlank(keyAlias) || isBlank(keyPassword))
            return empty();
        return new KeyStore(new File(storeFile), storePassword, keyAlias, keyPassword);
    }

    private static @NotNull KeyStore fromDebugKeystore() {
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

    private static @Nullable String readProperty(@NotNull Project project, @NotNull String name) {
        Object extra = project.findProperty(name);
        if (extra != null) {
            String value = extra.toString();
            if (!isBlank(value))
                return value;
        }
        Provider<String> gradleProperty = project.getProviders().gradleProperty(name);
        String value = gradleProperty.getOrNull();
        if (!isBlank(value))
            return value;
        return null;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
