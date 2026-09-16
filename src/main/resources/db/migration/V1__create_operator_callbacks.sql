CREATE TABLE operator_callbacks (
    id UUID PRIMARY KEY,
    operator_code VARCHAR(50) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(512) NOT NULL,
    content_type VARCHAR(255),
    source_ip VARCHAR(64),
    request_headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_payload TEXT NOT NULL,
    payload_sha256 CHAR(64) NOT NULL,
    processing_status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    processing_error TEXT,
    processed_at TIMESTAMPTZ,
    CONSTRAINT chk_operator_callbacks_processing_status
        CHECK (processing_status IN ('RECEIVED', 'PROCESSED', 'REJECTED', 'FAILED'))
);

CREATE INDEX idx_operator_callbacks_received_at
    ON operator_callbacks (received_at DESC);

CREATE INDEX idx_operator_callbacks_payload_sha256
    ON operator_callbacks (payload_sha256);
