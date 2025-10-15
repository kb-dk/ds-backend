CREATE TABLE IF NOT EXISTS RESTRICTED_IDS (
    id       BIGINT PRIMARY KEY,
    id_value VARCHAR(256) NOT NULL,
    id_type  VARCHAR(32)  NOT NULL,
    platform VARCHAR(32),
    comment  VARCHAR(16384)
);

CREATE UNIQUE INDEX IF NOT EXISTS unique_restricted_id ON RESTRICTED_IDS (id_value, id_type, platform);
CREATE UNIQUE INDEX IF NOT EXISTS RESTRICTED_IDS_ID_IN ON RESTRICTED_IDS(ID);
CREATE INDEX IF NOT EXISTS RESTRICTED_IDS_ID_VALUE_PLATFORM_IN ON RESTRICTED_IDS(ID_VALUE, PLATFORM);

CREATE TABLE IF NOT EXISTS dr_holdback_rules (
    id                BIGINT PRIMARY KEY,
    dr_holdback_value VARCHAR(256) UNIQUE,
    name              VARCHAR(256),
    days              int
);

CREATE UNIQUE INDEX IF NOT EXISTS dr_holdback_rules_id_in ON dr_holdback_rules(id);
CREATE INDEX IF NOT EXISTS dr_holdback_rules_dr_holdback_value_in ON dr_holdback_rules(dr_holdback_value);
CREATE INDEX IF NOT EXISTS dr_holdback_rules_name_in ON dr_holdback_rules(name);

/*
 Table to map content and/or form to holdback
 */
CREATE TABLE IF NOT EXISTS dr_holdback_ranges (
    id                 BIGINT PRIMARY KEY,
    content_range_from INTEGER NOT NULL,
    content_range_to   INTEGER NOT NULL,
    form_range_from    INTEGER NOT NULL,
    form_range_to      INTEGER NOT NULL,
    dr_holdback_value  VARCHAR(256) references dr_holdback_rules (dr_holdback_value)
);

CREATE UNIQUE INDEX IF NOT EXISTS dr_holdback_ranges_id_in ON dr_holdback_ranges(id);
CREATE INDEX IF NOT EXISTS dr_holdback_ranges_dr_holdback_value_in ON dr_holdback_ranges(dr_holdback_value);
CREATE INDEX IF NOT EXISTS dr_holdback_ranges_content_form_in ON dr_holdback_ranges(content_range_from, content_range_to, form_range_from, form_range_to);

CREATE TABLE IF NOT EXISTS auditlog (
    id            BIGINT PRIMARY KEY,
    objectid      BIGINT       NOT NULL,
    modifiedtime  BIGINT       NOT NULL,
    username      VARCHAR(256) NOT NULL,
    changetype    VARCHAR(256) NOT NULL,
    changename    VARCHAR(256) NOT NULL,
    identifier    VARCHAR(1024),
    changecomment TEXT,
    textbefore    VARCHAR(65535),
    textafter     VARCHAR(65535)
);

CREATE UNIQUE INDEX IF NOT EXISTS auditlog_id_in ON auditlog(id);
CREATE INDEX IF NOT EXISTS auditlog_objectid_in ON auditlog(objectid);
