CREATE TYPE push_notification_delivery_status AS ENUM ('PENDING', 'SENT', 'FAILED');

CREATE TABLE push_subscriptions (
    id          UUID         PRIMARY KEY,
    festival_id UUID         NOT NULL,
    user_id     VARCHAR(30)  NOT NULL,
    endpoint    TEXT         NOT NULL UNIQUE,
    p256dh_key  VARCHAR(255) NOT NULL,
    auth_key    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT push_subscriptions_user_fk
        FOREIGN KEY (festival_id, user_id) REFERENCES users(festival_id, id) ON DELETE CASCADE
);

CREATE TABLE waiting_notification_events (
    id         UUID         PRIMARY KEY,
    waiting_id UUID         NOT NULL REFERENCES waitings(id) ON DELETE CASCADE,
    event_type VARCHAR(50)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       TEXT         NOT NULL,
    icon       VARCHAR(500),
    url        VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE push_notification_deliveries (
    id              UUID                              PRIMARY KEY,
    event_id        UUID                              NOT NULL REFERENCES waiting_notification_events(id) ON DELETE CASCADE,
    subscription_id UUID                              REFERENCES push_subscriptions(id) ON DELETE SET NULL,
    endpoint        TEXT                              NOT NULL,
    status          push_notification_delivery_status NOT NULL DEFAULT 'PENDING',
    response_status INTEGER,
    failure_reason  TEXT,
    attempted_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ                       NOT NULL
);

CREATE INDEX idx_push_subscriptions_user ON push_subscriptions(festival_id, user_id);
CREATE INDEX idx_notification_events_waiting ON waiting_notification_events(waiting_id, created_at);
CREATE INDEX idx_notification_deliveries_event ON push_notification_deliveries(event_id, created_at);
