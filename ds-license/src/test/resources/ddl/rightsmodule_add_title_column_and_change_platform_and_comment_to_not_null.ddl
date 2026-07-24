ALTER TABLE restricted_ids
    ADD title VARCHAR(4096) NULL;

ALTER TABLE restricted_ids
    ALTER COLUMN comment SET NOT NULL;

ALTER TABLE restricted_ids
    ALTER COLUMN platform SET NOT NULL;