package com.agenticdev.sdlc.github.auth;

import com.agenticdev.sdlc.github.config.GitHubProperties;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider;
import org.kohsuke.github.authorization.OrgAppInstallationAuthorizationProvider;
import org.kohsuke.github.extras.authorization.JWTTokenProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(prefix = "app.github", name = "enabled", havingValue = "true")
public class GitHubClientFactory {

    @Bean
    @ConditionalOnProperty(prefix = "app.github", name = "auth-mode", havingValue = "pat")
    GitHub githubPat(GitHubProperties props) throws IOException {
        return new GitHubBuilder()
                .withOAuthToken(props.pat().token())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.github", name = "auth-mode", havingValue = "app")
    GitHub githubApp(GitHubProperties props) throws Exception {
        JWTTokenProvider jwt = new JWTTokenProvider(
                props.app().appId(),
                loadPrivateKey(props.app().privateKeyPem()));
        AppInstallationAuthorizationProvider installation = new AppInstallationAuthorizationProvider(
                app -> app.getInstallationById(Long.parseLong(props.app().installationId())),
                jwt);
        return new GitHubBuilder()
                .withAuthorizationProvider(installation)
                .build();
    }

    /** Reads a PEM-encoded RSA private key (supports the string being either PEM text or a file path). */
    private static java.security.PrivateKey loadPrivateKey(String pemOrPath) throws Exception {
        String pem = pemOrPath;
        if (!pemOrPath.contains("BEGIN")) {
            pem = java.nio.file.Files.readString(java.nio.file.Paths.get(pemOrPath));
        }
        String clean = pem.replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s+", "");
        byte[] bytes = java.util.Base64.getDecoder().decode(clean);
        return java.security.KeyFactory.getInstance("RSA")
                .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(bytes));
    }
}
