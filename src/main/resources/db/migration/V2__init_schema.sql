-- Enum types
CREATE TYPE user_role AS ENUM ('USER', 'BOOTH_MANAGER', 'FESTIVAL_ADMIN');
CREATE TYPE booth_category AS ENUM ('ACTIVITY', 'INFO', 'MARKET', 'EXPERIENCE', 'PROMOTION', 'ALCOHOL');
CREATE TYPE booth_type AS ENUM ('DAY', 'NIGHT');
CREATE TYPE waiting_status AS ENUM ('WAITING', 'CALLED', 'SEATED', 'CANCELLED');

-- Users
CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    role          user_role    NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

-- Booths
CREATE TABLE booths (
    id              UUID           PRIMARY KEY,
    manager_id      UUID           REFERENCES users(id) ON DELETE SET NULL,
    created_by      UUID           REFERENCES users(id) ON DELETE SET NULL,
    name            VARCHAR(100)   NOT NULL,
    category        booth_category NOT NULL DEFAULT 'ACTIVITY',
    type            booth_type     NOT NULL,
    description     TEXT,
    operating_hours VARCHAR(100),
    image_url       VARCHAR(500),
    is_active       BOOLEAN        NOT NULL DEFAULT TRUE,
    is_waiting_open BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL
);

-- Menu items (야간 부스 전용)
CREATE TABLE menu_items (
    id          UUID         PRIMARY KEY,
    booth_id    UUID         NOT NULL REFERENCES booths(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    price       INTEGER      NOT NULL,
    description TEXT,
    image_url   VARCHAR(500),
    is_sold_out BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

-- Waitings (야간 부스 전용)
CREATE TABLE waitings (
    id            UUID           PRIMARY KEY,
    booth_id      UUID           NOT NULL REFERENCES booths(id) ON DELETE CASCADE,
    user_id       UUID           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    party_size    SMALLINT       NOT NULL,
    status        waiting_status NOT NULL DEFAULT 'WAITING',
    call_count    SMALLINT       NOT NULL DEFAULT 0,
    registered_at TIMESTAMPTZ    NOT NULL,
    updated_at    TIMESTAMPTZ    NOT NULL
);

-- Booth locations (배치도)
CREATE TABLE booth_locations (
    id         SMALLINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booth_id   UUID        REFERENCES booths(id) ON DELETE SET NULL,
    type       booth_type  NOT NULL,
    index      SMALLINT,
    day        DATE        NOT NULL,
    zone_label VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- Booth admin assignments
CREATE TABLE booth_admin_assignments (
    id         UUID        PRIMARY KEY,
    booth_id   UUID        NOT NULL REFERENCES booths(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    granted_by UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (booth_id, user_id)
);

-- Festival
CREATE TABLE festival (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

-- Notices
CREATE TABLE notices (
    id          UUID         PRIMARY KEY,
    festival_id UUID         NOT NULL REFERENCES festival(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    created_by  UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

-- Indexes
CREATE INDEX idx_booths_type ON booths(type);
CREATE INDEX idx_booth_locations_day_type ON booth_locations(day, type);
CREATE INDEX idx_waitings_booth_id_status ON waitings(booth_id, status);
CREATE INDEX idx_waitings_user_id ON waitings(user_id);
CREATE INDEX idx_menu_items_booth_id ON menu_items(booth_id);
CREATE INDEX idx_notices_festival_id ON notices(festival_id);
