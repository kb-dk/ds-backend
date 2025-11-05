CREATE TABLE IF NOT EXISTS restricted_ids (
    id       BIGINT PRIMARY KEY,
    id_value VARCHAR(256)   NOT NULL,
    id_type  VARCHAR(32)    NOT NULL,
    platform VARCHAR(32)    NOT NULL,
    title    VARCHAR(4096)  NULL,
    comment  VARCHAR(16384) NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS unique_restricted_id ON restricted_ids (id_value, id_type, platform);
CREATE UNIQUE INDEX IF NOT EXISTS restricted_ids_id_in ON restricted_ids(id);
CREATE INDEX IF NOT EXISTS restricted_ids_id_value_platform_in ON restricted_ids(id_value, platform);

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

CREATE TABLE IF NOT EXISTS AUDITLOG (
    ID            BIGINT PRIMARY KEY,
    OBJECTID      BIGINT NOT NULL,
    MODIFIEDTIME  BIGINT NOT NULL,
    USERNAME      VARCHAR(256) NOT NULL,
    CHANGETYPE    VARCHAR(256) NOT NULL,
    CHANGENAME    VARCHAR(256) NOT NULL,
    CHANGECOMMENT VARCHAR(1024),
    TEXTBEFORE    VARCHAR(65535),
    TEXTAFTER     VARCHAR(65535)
);

CREATE UNIQUE INDEX IF NOT EXISTS AUDITLOG_ID_IN_IN ON AUDITLOG(ID);
CREATE INDEX IF NOT EXISTS AUDITLOG_OBJECTID_IN ON AUDITLOG(OBJECTID);

