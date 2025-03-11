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

CREATE UNIQUE INDEX IF NOT EXISTS unique_restricted_id ON RESTRICTED_IDS (id_value,id_type,platform);

CREATE TABLE IF NOT EXISTS DR_HOLDBACK_RULES (
                                                 id VARCHAR(256) PRIMARY KEY,
                                                 name VARCHAR(256),
                                                 days int
);

CREATE TABLE IF NOT EXISTS DR_HOLDBACK_MAP (
                              id            VARCHAR(256) PRIMARY KEY,
                              content_range_from INTEGER NOT NULL,
                              content_range_to INTEGER NOT NULL,
                              form_range_from INTEGER NOT NULL,
                              form_range_to INTEGER NOT NULL,
                              holdback_id   VARCHAR(32) references DR_HOLDBACK_RULES(id)
);

