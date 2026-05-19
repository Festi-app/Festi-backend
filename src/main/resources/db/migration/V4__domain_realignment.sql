-- Truncate all data (dev environment only — migrate data separately in production)
TRUNCATE TABLE booth_admin_assignments, waitings, booth_locations, menu_items, notices, booths, users RESTART IDENTITY CASCADE;

-- Drop old enum types and add FOOD_TRUCK to booth_type
ALTER TYPE booth_type ADD VALUE IF NOT EXISTS 'FOOD_TRUCK';

-- Rebuild users table with composite PK (festival_id + id)
ALTER TABLE users DROP CONSTRAINT users_pkey;
ALTER TABLE users DROP COLUMN id;
ALTER TABLE users DROP COLUMN email;
ALTER TABLE users ADD COLUMN id VARCHAR(30) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN festival_id UUID NOT NULL REFERENCES festival(id) ON DELETE CASCADE;
ALTER TABLE users ADD PRIMARY KEY (festival_id, id);
ALTER TABLE users ALTER COLUMN id DROP DEFAULT;

-- Rebuild booths: remove is_active, change FK columns to plain strings
ALTER TABLE booths DROP COLUMN is_active;
ALTER TABLE booths DROP COLUMN manager_id;
ALTER TABLE booths DROP COLUMN created_by;
ALTER TABLE booths ADD COLUMN manager_id VARCHAR(30);
ALTER TABLE booths ADD COLUMN created_by_id VARCHAR(30) NOT NULL DEFAULT '';
ALTER TABLE booths ALTER COLUMN created_by_id DROP DEFAULT;

-- Rebuild waitings: change user FK to composite PK reference
ALTER TABLE waitings DROP CONSTRAINT waitings_user_id_fkey;
ALTER TABLE waitings DROP COLUMN user_id;
ALTER TABLE waitings ADD COLUMN user_festival_id UUID NOT NULL REFERENCES festival(id) ON DELETE CASCADE;
ALTER TABLE waitings ADD COLUMN user_id VARCHAR(30) NOT NULL DEFAULT '';
ALTER TABLE waitings ALTER COLUMN user_id DROP DEFAULT;
ALTER TABLE waitings ADD CONSTRAINT waitings_user_fk
    FOREIGN KEY (user_festival_id, user_id) REFERENCES users(festival_id, id) ON DELETE CASCADE;

-- Rebuild booth_locations: add festival_id, add unique constraint
ALTER TABLE booth_locations ADD COLUMN festival_id UUID REFERENCES festival(id) ON DELETE CASCADE;
ALTER TABLE booth_locations ALTER COLUMN festival_id SET NOT NULL;
ALTER TABLE booth_locations ADD CONSTRAINT uq_booth_locations_festival_zone_index_day
    UNIQUE (festival_id, zone_label, index, day);

-- Rebuild booth_admin_assignments: change user FKs to plain strings
ALTER TABLE booth_admin_assignments DROP CONSTRAINT booth_admin_assignments_user_id_fkey;
ALTER TABLE booth_admin_assignments DROP CONSTRAINT booth_admin_assignments_granted_by_fkey;
ALTER TABLE booth_admin_assignments DROP COLUMN user_id;
ALTER TABLE booth_admin_assignments DROP COLUMN granted_by;
ALTER TABLE booth_admin_assignments ADD COLUMN user_id VARCHAR(30) NOT NULL DEFAULT '';
ALTER TABLE booth_admin_assignments ADD COLUMN granted_by_id VARCHAR(30) NOT NULL DEFAULT '';
ALTER TABLE booth_admin_assignments ALTER COLUMN user_id DROP DEFAULT;
ALTER TABLE booth_admin_assignments ALTER COLUMN granted_by_id DROP DEFAULT;

-- Add pinned to notices
ALTER TABLE notices ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;
-- Change notices.created_by to plain string
ALTER TABLE notices DROP CONSTRAINT IF EXISTS notices_created_by_fkey;
ALTER TABLE notices DROP COLUMN created_by;
ALTER TABLE notices ADD COLUMN created_by_id VARCHAR(30);

-- New tables

-- FestivalDay
CREATE TABLE festival_days (
    id         UUID        PRIMARY KEY,
    festival_id UUID       NOT NULL REFERENCES festival(id) ON DELETE CASCADE,
    day        DATE        NOT NULL,
    day_start  TIME,
    day_end    TIME,
    night_start TIME,
    night_end   TIME,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (festival_id, day)
);

-- Timeline
CREATE TABLE timelines (
    id          UUID         PRIMARY KEY,
    festival_id UUID         NOT NULL REFERENCES festival(id) ON DELETE CASCADE,
    day         DATE         NOT NULL,
    title       VARCHAR(200) NOT NULL,
    artist      VARCHAR(200) NOT NULL,
    start_time  TIME         NOT NULL,
    end_time    TIME         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

-- BoothApplicationStatus enum
CREATE TYPE booth_application_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- BoothApplication
CREATE TABLE booth_applications (
    id             UUID                     PRIMARY KEY,
    festival_id    UUID                     NOT NULL REFERENCES festival(id) ON DELETE CASCADE,
    applicant_id   VARCHAR(30)              NOT NULL,
    booth_name     VARCHAR(100)             NOT NULL,
    booth_type     booth_type               NOT NULL,
    booth_category booth_category           NOT NULL DEFAULT 'ACTIVITY',
    image_url      VARCHAR(500),
    description    TEXT,
    status         booth_application_status NOT NULL DEFAULT 'PENDING',
    review_memo    TEXT,
    created_at     TIMESTAMPTZ              NOT NULL,
    updated_at     TIMESTAMPTZ              NOT NULL,
    CONSTRAINT booth_applications_applicant_fk
        FOREIGN KEY (festival_id, applicant_id) REFERENCES users(festival_id, id) ON DELETE CASCADE
);

-- Favorites
CREATE TABLE favorites (
    id          UUID        PRIMARY KEY,
    festival_id UUID        NOT NULL REFERENCES festival(id) ON DELETE CASCADE,
    user_id     VARCHAR(30) NOT NULL,
    booth_id    UUID        NOT NULL REFERENCES booths(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT favorites_user_fk
        FOREIGN KEY (festival_id, user_id) REFERENCES users(festival_id, id) ON DELETE CASCADE,
    UNIQUE (festival_id, user_id, booth_id)
);

-- Indexes
CREATE INDEX idx_festival_days_festival_id ON festival_days(festival_id);
CREATE INDEX idx_timelines_festival_id_day ON timelines(festival_id, day);
CREATE INDEX idx_booth_applications_festival_id ON booth_applications(festival_id);
CREATE INDEX idx_favorites_user ON favorites(festival_id, user_id);
CREATE INDEX idx_booth_locations_festival_day_type ON booth_locations(festival_id, day, type);
CREATE INDEX idx_waitings_user ON waitings(user_festival_id, user_id);
