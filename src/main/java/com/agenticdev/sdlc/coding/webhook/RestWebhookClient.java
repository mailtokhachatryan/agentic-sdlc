package com.agenticdev.sdlc.coding.webhook;

import com.agenticdev.sdlc.coding.agent.CodingAgentProperties;
import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class RestWebhookClient implements WebhookClient {

    private static final Logger log = LoggerFactory.getLogger(RestWebhookClient.class);

    private final RestClient http;

    public RestWebhookClient(CodingAgentProperties props) {
        Duration connect = props.webhook() != null && props.webhook().connectTimeout() != null
                ? props.webhook().connectTimeout() : Duration.ofSeconds(2);
        Duration read = props.webhook() != null && props.webhook().readTimeout() != null
                ? props.webhook().readTimeout() : Duration.ofSeconds(5);
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(connect).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(read);
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean notify(String url, CodingRunRecord record) {
        if (url == null || url.isBlank()) return false;
        try {
            http.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(WebhookPayload.from(record))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Webhook delivered to {} for run {}", url, record.getId());
            return true;
        } catch (RuntimeException e) {
            log.warn("Webhook delivery to {} failed for run {}: {}", url, record.getId(), e.getMessage());
            return false;
        }
    }
}
