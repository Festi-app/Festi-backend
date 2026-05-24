CREATE TYPE waiting_notification_event_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'RETRY_WAIT',
    'FAILED'
);

ALTER TABLE waiting_notification_events
    ADD COLUMN status                waiting_notification_event_status NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN attempt_count         INTEGER                           NOT NULL DEFAULT 0,
    ADD COLUMN available_at          TIMESTAMPTZ                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN processing_started_at TIMESTAMPTZ,
    ADD COLUMN processed_at          TIMESTAMPTZ,
    ADD COLUMN failure_reason        TEXT;

ALTER TABLE waiting_notification_events
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE push_notification_deliveries
    ADD COLUMN retryable BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_notification_events_dispatch
    ON waiting_notification_events(status, available_at, created_at);
