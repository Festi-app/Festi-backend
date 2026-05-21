-- BoothType enum 확장
ALTER TYPE booth_type ADD VALUE 'FOOD_TRUCK';

-- Timelines (공연 타임라인)
CREATE TABLE timelines (
    id          UUID         PRIMARY KEY,
    festival_id UUID         NOT NULL REFERENCES festival(id) ON DELETE CASCADE,
    day         DATE         NOT NULL,
    title       VARCHAR(200) NOT NULL,
    artist      VARCHAR(20)  NOT NULL,
    start_time  TIME         NOT NULL,
    end_time    TIME         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

-- Favorites (부스 즐겨찾기)
CREATE TABLE favorites (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    booth_id   UUID NOT NULL REFERENCES booths(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, booth_id)
);

-- Indexes
CREATE INDEX idx_timelines_festival_id_day ON timelines(festival_id, day);
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
