package com.agenticdev.sdlc.github.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubConfigValidatorTest {

    private GitHubProperties props(boolean enabled, GitHubProperties.AuthMode mode,
                                   String pat, String appId, String installId, String pem) {
        return new GitHubProperties(
                enabled, mode,
                new GitHubProperties.Pat(pat),
                new GitHubProperties.App(appId, installId, pem),
                "agentic/", List.of("agentic-sdlc"),
                new GitHubProperties.Commit("A", "a@b", "feat"),
                new GitHubProperties.Retry(3, Duration.ofMillis(500), Duration.ofSeconds(5), 2.0),
                "body"
        );
    }

    @Test
    void disabledSkipsAllChecks() {
        GitHubConfigValidator v = new GitHubConfigValidator(
                props(false, null, null, null, null, null));
        assertThatCode(v::validate).doesNotThrowAnyException();
    }

    @Test
    void patMode_requiresToken() {
        GitHubConfigValidator v = new GitHubConfigValidator(
                props(true, GitHubProperties.AuthMode.PAT, "", null, null, null));
        assertThatThrownBy(v::validate).hasMessageContaining("pat.token");
    }

    @Test
    void patMode_withToken_passes() {
        GitHubConfigValidator v = new GitHubConfigValidator(
                props(true, GitHubProperties.AuthMode.PAT, "ghp_xxx", null, null, null));
        assertThatCode(v::validate).doesNotThrowAnyException();
    }

    @Test
    void appMode_requiresAllThree() {
        GitHubConfigValidator v = new GitHubConfigValidator(
                props(true, GitHubProperties.AuthMode.APP, null, "123", "", "pem"));
        assertThatThrownBy(v::validate).hasMessageContaining("installation-id");
    }

    @Test
    void appMode_withAllFields_passes() {
        GitHubConfigValidator v = new GitHubConfigValidator(
                props(true, GitHubProperties.AuthMode.APP, null, "123", "456", "pem"));
        assertThatCode(v::validate).doesNotThrowAnyException();
    }

    @Test
    void enabledWithoutAuthMode_fails() {
        GitHubConfigValidator v = new GitHubConfigValidator(
                props(true, null, null, null, null, null));
        assertThatThrownBy(v::validate).hasMessageContaining("auth-mode");
    }
}
