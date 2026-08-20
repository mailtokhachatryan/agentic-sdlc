package com.agenticdev.sdlc.github.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class GitHubConfigValidator {

    private final GitHubProperties props;

    public GitHubConfigValidator(GitHubProperties props) {
        this.props = props;
    }

    @PostConstruct
    void validate() {
        if (!props.enabled()) return;

        if (props.authMode() == null) {
            throw new IllegalStateException("app.github.enabled=true but app.github.auth-mode is not set");
        }
        switch (props.authMode()) {
            case PAT -> {
                if (props.pat() == null || isBlank(props.pat().token())) {
                    throw new IllegalStateException("app.github.auth-mode=pat but app.github.pat.token is empty");
                }
            }
            case APP -> {
                if (props.app() == null
                        || isBlank(props.app().appId())
                        || isBlank(props.app().installationId())
                        || isBlank(props.app().privateKeyPem())) {
                    throw new IllegalStateException(
                            "app.github.auth-mode=app but one of app-id/installation-id/private-key-pem is empty");
                }
            }
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
