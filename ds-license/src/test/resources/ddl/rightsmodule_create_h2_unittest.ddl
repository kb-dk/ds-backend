CREATE TABLE IF NOT EXISTS RESTRICTED_IDS (
    id BIGINT PRIMARY KEY,
    id_value VARCHAR(256) NOT NULL,
    id_type VARCHAR(32) NOT NULL,
    platform VARCHAR(32),
    comment VARCHAR(16384),
    created_by VARCHAR(256),
    created_time BIGINT,
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
                              id            BIGINT PRIMARY KEY,
                              content_range_from INTEGER NOT NULL,
                              content_range_to INTEGER NOT NULL,
                              form_range_from INTEGER NOT NULL,
                              form_range_to INTEGER NOT NULL,
                              dr_holdback_id   VARCHAR(32) references DR_HOLDBACK_RULES(id)
);

CREATE TABLE IF NOT EXISTS AUDITLOG (
                                        MILLIS BIGINT PRIMARY KEY,
                                        USERNAME VARCHAR(256) NOT NULL,
                                        CHANGETYPE VARCHAR(256) NOT NULL,
                                        OBJECTNAME VARCHAR(256) NOT NULL,
                                        TEXTBEFORE VARCHAR(65535) NOT NULL,
                                        TEXTAFTER VARCHAR(65535) NOT NULL
);

