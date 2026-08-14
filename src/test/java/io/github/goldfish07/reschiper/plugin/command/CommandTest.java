package io.github.goldfish07.reschiper.plugin.command;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTest {

    @Test
    void shouldSignWhenDisableSignIsAbsent() {
        assertTrue(Command.shouldSign(Optional.empty()));
    }

    @Test
    void shouldSignWhenDisableSignIsFalse() {
        assertTrue(Command.shouldSign(Optional.of(false)));
    }

    @Test
    void shouldNotSignWhenDisableSignIsTrue() {
        assertFalse(Command.shouldSign(Optional.of(true)));
    }

    @Test
    void shouldBuildUniversalApkWhenEnabled() {
        assertTrue(Command.shouldBuildUniversalApk(Optional.of(true)));
    }

    @Test
    void shouldNotBuildUniversalApkWhenFlagIsAbsent() {
        assertFalse(Command.shouldBuildUniversalApk(Optional.empty()));
    }

    @Test
    void shouldNotBuildUniversalApkWhenDisabled() {
        assertFalse(Command.shouldBuildUniversalApk(Optional.of(false)));
    }
}
