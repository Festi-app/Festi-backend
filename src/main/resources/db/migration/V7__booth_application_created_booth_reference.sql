ALTER TABLE booth_applications
    ADD COLUMN booth_id UUID;

ALTER TABLE booth_applications
    ADD CONSTRAINT booth_applications_booth_fk
        FOREIGN KEY (booth_id) REFERENCES booths(id) ON DELETE SET NULL,
    ADD CONSTRAINT booth_applications_booth_unique UNIQUE (booth_id);
