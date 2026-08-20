package com.agenticdev.sdlc.jira;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestJiraClient implements JiraClient {

    private final RestClient jiraRestClient;

    public RestJiraClient(RestClient jiraRestClient) {
        this.jiraRestClient = jiraRestClient;
    }

    @Override
    public JiraTicket fetch(String jiraKey) {
        try {
            JsonNode body = jiraRestClient.get()
                    .uri("/rest/api/3/issue/{key}?fields=summary,description,status,issuetype", jiraKey)
                    .retrieve()
                    .body(JsonNode.class);

            if (body == null) {
                throw new JiraClientException("Empty response for " + jiraKey);
            }
            JsonNode fields = body.path("fields");
            return new JiraTicket(
                    body.path("key").asText(jiraKey),
                    fields.path("summary").asText(""),
                    renderDescription(fields.path("description")),
                    fields.path("status").path("name").asText(""),
                    fields.path("issuetype").path("name").asText("")
            );
        } catch (RestClientException e) {
            throw new JiraClientException("Failed to fetch Jira ticket " + jiraKey, e);
        }
    }

    private String renderDescription(JsonNode description) {
        if (description.isMissingNode() || description.isNull()) return "";
        if (description.isTextual()) return description.asText();
        return description.toString();
    }
}
