package com.agenticdev.sdlc.planning.persistence;

import com.agenticdev.sdlc.jira.JiraTicket;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.planning.domain.FileChange;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import com.agenticdev.sdlc.planning.domain.PlanRisk;
import com.agenticdev.sdlc.planning.domain.PlanTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PlanRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    PlanRepository repo;

    @Test
    void roundTripsPlanAndJiraSnapshot() {
        JiraTicket ticket = new JiraTicket("AS-1", "Sum", "Desc", "Open", "Story");
        PlanRecord r = PlanRecord.jiraPending(Provider.OPENAI, "gpt-4o", ticket);
        PlanResult plan = new PlanResult(
                "s", "a",
                List.of(new PlanTask("t1", "d", "S")),
                List.of(new FileChange("f.java", FileChange.ChangeType.MODIFY, "why")),
                List.of(new PlanRisk("r", "m")),
                List.of("q?"),
                "md"
        );
        r.markCompleted(plan, 100L);
        repo.saveAndFlush(r);

        PlanRecord loaded = repo.findById(r.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(PlanStatus.COMPLETED);
        assertThat(loaded.getJiraSnapshot()).isEqualTo(ticket);
        assertThat(loaded.getPlan()).isEqualTo(plan);
    }
}
