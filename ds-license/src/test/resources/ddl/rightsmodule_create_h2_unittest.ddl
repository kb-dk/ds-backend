CREATE TABLE IF NOT EXISTS RESTRICTED_IDS (
                         id BIGINT PRIMARY KEY,
                         id_value VARCHAR(256) NOT NULL,
                         id_type VARCHAR(32) NOT NULL,
                         platform VARCHAR(32),
                         comment VARCHAR(16384)
);

CREATE UNIQUE INDEX IF NOT EXISTS unique_restricted_id ON RESTRICTED_IDS (id_value,id_type,platform);
CREATE UNIQUE INDEX IF NOT EXISTS RESTRICTED_IDS_ID_IN ON RESTRICTED_IDS(ID);
CREATE INDEX IF NOT EXISTS RESTRICTED_IDS_ID_VALUE_PLATFORM_IN ON RESTRICTED_IDS(ID_VALUE,PLATFORM);

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
       ID BIGINT PRIMARY KEY,
       OBJECTID BIGINT NOT NULL,
       MODIFIEDTIME BIGINT NOT NULL,
       USERNAME VARCHAR(256) NOT NULL,
       CHANGETYPE VARCHAR(256) NOT NULL,
       CHANGENAME VARCHAR(256) NOT NULL,
       CHANGECOMMENT VARCHAR(1024),
       TEXTBEFORE VARCHAR(65535),
       TEXTAFTER VARCHAR(65535) 
);       

CREATE UNIQUE INDEX IF NOT EXISTS AUDITLOG_ID_IN_IN ON AUDITLOG(ID);
CREATE INDEX IF NOT EXISTS AUDITLOG_OBJECTID_IN ON AUDITLOG(OBJECTID);

