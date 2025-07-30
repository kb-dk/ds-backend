CREATE TABLE  RESTRICTED_IDS (
                                              id BIGINT PRIMARY KEY,
                                              id_value VARCHAR(256) NOT NULL,
                                              id_type VARCHAR(32) NOT NULL,
                                              platform VARCHAR(32),
                                              comment VARCHAR(16384)
);

CREATE UNIQUE INDEX unique_restricted_id ON RESTRICTED_IDS (id_value,id_type,platform);
CREATE UNIQUE INDEX RESTRICTED_IDS_ID_IN ON RESTRICTED_IDS(ID);
CREATE INDEX RESTRICTED_IDS_ID_VALUE_PLATFORM_IN ON RESTRICTED_IDS(ID_VALUE,PLATFORM);


CREATE TABLE DR_HOLDBACK_RULES (
    id VARCHAR(256) PRIMARY KEY,
    name VARCHAR(256),
    days int
);

/*
 Table to map content and/or form to holdback
 */
CREATE TABLE DR_HOLDBACK_MAP (
    id            BIGINT PRIMARY KEY,
    content_range_from INTEGER NOT NULL,
    content_range_to INTEGER NOT NULL,
    form_range_from INTEGER NOT NULL,
    form_range_to INTEGER NOT NULL,
    dr_holdback_id   VARCHAR(32) references DR_HOLDBACK_RULES(id)
);
