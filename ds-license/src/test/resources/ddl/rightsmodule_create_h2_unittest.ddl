CREATE TABLE IF NOT EXISTS RESTRICTED_IDS (
                         id BIGINT PRIMARY KEY,
                         id_value VARCHAR(256) NOT NULL,
                         id_type VARCHAR(32) NOT NULL,
                         platform VARCHAR(32),
                         comment VARCHAR(256),
                         modified_by VARCHAR(256),
                         modified_time BIGINT,
                         modified_time_human VARCHAR(256)
);

CREATE UNIQUE INDEX IF NOT EXISTS unique_restricted_id ON RESTRICTED_IDS (id_value,id_type,platform)
