package io.github.goldfish07.reschiper.plugin.internal;

import com.android.build.api.dsl.ApplicationBuildType;
import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.dsl.ApkSigningConfig;
import com.android.build.gradle.api.ApplicationVariant;
import io.github.goldfish07.reschiper.plugin.model.KeyStore;
import org.gradle.api.Project;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SigningConfig {
    @Contract(" -> new")
    public static @NotNull KeyStore empty() {
        return new KeyStore(null, null, null, null);
    }

    @Contract("_, _ -> new")
    public static @NotNull KeyStore getSigningConfig(@NotNull Project project, @Nullable String buildTypeName) {
        if (buildTypeName == null)
            return empty();
        ApplicationExtension android = project.getExtensions().getByType(ApplicationExtension.class);
        ApplicationBuildType buildType = android.getBuildTypes().getByName(buildTypeName);
        ApkSigningConfig signingConfig = buildType.getSigningConfig();
        if (signingConfig == null)
            return empty();
        return new KeyStore(
                signingConfig.getStoreFile(),
                signingConfig.getStorePassword(),
                signingConfig.getKeyAlias(),
                signingConfig.getKeyPassword()
        );
    }

    @Contract("_ -> new")
    public static @NotNull KeyStore getSigningConfig(@NotNull ApplicationVariant variant) {
        return new KeyStore(
                variant.getSigningConfig().getStoreFile(),
                variant.getSigningConfig().getStorePassword(),
                variant.getSigningConfig().getKeyAlias(),
                variant.getSigningConfig().getKeyPassword()
        );
    }
}
