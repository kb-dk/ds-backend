CREATE TABLE IF NOT EXISTS auditlog (
    id            BIGINT PRIMARY KEY,
    objectid      BIGINT         NOT NULL,
    modifiedtime  BIGINT         NOT NULL,
    username      VARCHAR(256)   NOT NULL,
    changetype    VARCHAR(256)   NOT NULL,
    changename    VARCHAR(256)   NOT NULL,
    identifier    VARCHAR(1024)  NOT NULL,
    changecomment TEXT           NULL,
    textbefore    VARCHAR(65535) NULL,
    textafter     VARCHAR(65535) NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS auditlog_id_in ON auditlog(id);
CREATE INDEX IF NOT EXISTS auditlog_objectid_in ON auditlog(objectid);
CREATE INDEX IF NOT EXISTS auditlog_identifier_in ON auditlog(identifier);
