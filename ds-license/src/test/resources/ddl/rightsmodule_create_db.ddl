CREATE TABLE  RESTRICTED_IDS (
                                              id BIGINT PRIMARY KEY,
                                              id_value VARCHAR(256) NOT NULL,
                                              id_type VARCHAR(32) NOT NULL,
                                              platform VARCHAR(32),
                                              comment VARCHAR(256),
                                              modified_by VARCHAR(256),
                                              modified_time BIGINT,
                                              modified_time_human VARCHAR(256)
);

CREATE UNIQUE INDEX unique_restricted_id ON RESTRICTED_IDS (id_value,id_type,platform);

/*
 Table to map content and/or form to holdback
 */
CREATE TABLE HOLDBACK_MAP (
    id            VARCHAR(256) PRIMARY KEY,
    content_range INT4RANGE NOT NULL,
    form_range    INT4RANGE NOT NULL,
    holdback_id   VARCHAR(32)
);

CREATE TABLE HOLDBACK_RULES (
    id VARCHAR(256) PRIMARY KEY,
    name VARCHAR(256),
    days int
);
