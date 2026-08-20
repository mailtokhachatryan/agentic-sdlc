package com.agenticdev.sdlc.jira.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class JiraClientConfig {

    @Bean
    RestClient jiraRestClient(JiraProperties props) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) props.connectTimeout().toMillis());
        rf.setReadTimeout((int) props.readTimeout().toMillis());

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(rf)
                .baseUrl(props.baseUrl() == null ? "" : props.baseUrl());

        if (props.email() != null && !props.email().isBlank()
                && props.apiToken() != null && !props.apiToken().isBlank()) {
            String token = Base64.getEncoder().encodeToString(
                    (props.email() + ":" + props.apiToken()).getBytes(StandardCharsets.UTF_8));
            builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + token);
        }
        return builder.build();
    }
}
