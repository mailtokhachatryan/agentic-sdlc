CREATE TABLE pull_requests (
    id                UUID PRIMARY KEY,
    coding_run_id     UUID NOT NULL REFERENCES coding_runs(id),
    status            VARCHAR(16) NOT NULL,
    repo_url          VARCHAR(512) NOT NULL,
    base_ref          VARCHAR(256) NOT NULL,
    head_branch       VARCHAR(256),
    head_sha          VARCHAR(64),
    pr_number         INTEGER,
    pr_url            VARCHAR(1024),
    title             VARCHAR(512),
    body              TEXT,
    draft             BOOLEAN NOT NULL DEFAULT FALSE,
    labels            JSONB,
    reviewers         JSONB,
    merge_strategy    VARCHAR(16),
    merged_sha        VARCHAR(64),
    error_code        VARCHAR(64),
    error_message     TEXT,
    duration_ms       BIGINT,
    webhook_url       VARCHAR(1024),
    webhook_sent      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    opened_at         TIMESTAMPTZ,
    merged_at         TIMESTAMPTZ
);

CREATE INDEX idx_pull_requests_coding_run_id ON pull_requests(coding_run_id);
CREATE INDEX idx_pull_requests_status        ON pull_requests(status);
CREATE INDEX idx_pull_requests_created_at    ON pull_requests(created_at DESC);
CREATE INDEX idx_pull_requests_pr_number     ON pull_requests(repo_url, pr_number);

ALTER TABLE coding_runs
    ADD COLUMN auto_open_pr BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN pr_title     TEXT,
    ADD COLUMN pr_body      TEXT;
