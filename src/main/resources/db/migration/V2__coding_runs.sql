CREATE TABLE coding_runs (
    id              UUID PRIMARY KEY,
    plan_id         UUID NOT NULL REFERENCES plans(id),
    status          VARCHAR(16) NOT NULL,
    provider        VARCHAR(16) NOT NULL,
    model           VARCHAR(128) NOT NULL,
    repo_url        VARCHAR(512),
    base_ref        VARCHAR(256),
    diff            TEXT,
    files_changed   INTEGER,
    iterations_used INTEGER,
    tokens_used     BIGINT,
    tests_passed    BOOLEAN,
    error_code      VARCHAR(64),
    error_message   TEXT,
    duration_ms     BIGINT,
    webhook_url     VARCHAR(1024),
    webhook_sent    BOOLEAN NOT NULL DEFAULT FALSE,
    timeout_reason  VARCHAR(32),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_coding_runs_plan_id    ON coding_runs(plan_id);
CREATE INDEX idx_coding_runs_status     ON coding_runs(status);
CREATE INDEX idx_coding_runs_created_at ON coding_runs(created_at DESC);
