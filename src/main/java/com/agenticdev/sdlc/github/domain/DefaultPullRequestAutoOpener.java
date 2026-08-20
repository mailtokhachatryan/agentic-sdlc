package com.agenticdev.sdlc.github.domain;

import com.agenticdev.sdlc.coding.domain.PullRequestAutoOpener;
import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import com.agenticdev.sdlc.github.config.GitHubProperties;
import com.agenticdev.sdlc.github.persistence.PullRequestRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "app.github", name = "enabled", havingValue = "true")
public class DefaultPullRequestAutoOpener implements PullRequestAutoOpener {

    private static final Logger log = LoggerFactory.getLogger(DefaultPullRequestAutoOpener.class);

    private final PullRequestService service;
    private final GitHubProperties props;

    public DefaultPullRequestAutoOpener(PullRequestService service, GitHubProperties props) {
        this.service = service;
        this.props = props;
    }

    @Override
    public void openFor(CodingRunRecord run) {
        if (!run.isAutoOpenPr()) return;
        if (!Boolean.TRUE.equals(run.getTestsPassed())) {
            log.info("Skipping auto-open PR for run {}: tests did not pass", run.getId());
            return;
        }
        if (!props.enabled()) {
            log.info("Skipping auto-open PR for run {}: app.github.enabled=false", run.getId());
            return;
        }
        try {
            PullRequestRecord record = service.createRun(
                    run.getId(),
                    run.getPrTitle(),
                    run.getPrBody(),
                    false,
                    null,
                    null);
            service.executePr(record.getId());
            log.info("Auto-opened PR {} for coding run {}", record.getId(), run.getId());
        } catch (RuntimeException e) {
            log.warn("Auto-open PR failed for run {}: {}", run.getId(), e.getMessage());
        }
    }
}
