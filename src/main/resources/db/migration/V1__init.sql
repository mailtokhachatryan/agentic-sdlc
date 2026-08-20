CREATE TABLE plans (
    id             UUID PRIMARY KEY,
    status         VARCHAR(16)  NOT NULL,
    provider       VARCHAR(16)  NOT NULL,
    model          VARCHAR(128) NOT NULL,
    input_type     VARCHAR(16)  NOT NULL,
    prompt         TEXT,
    jira_key       VARCHAR(64),
    jira_snapshot  JSONB,
    plan_json      JSONB,
    error_code     VARCHAR(64),
    error_message  TEXT,
    duration_ms    BIGINT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_plans_created_at ON plans(created_at DESC);
CREATE INDEX idx_plans_status     ON plans(status);
CREATE INDEX idx_plans_jira_key   ON plans(jira_key) WHERE jira_key IS NOT NULL;
