CREATE TABLE auditlog (
    id            BIGINT PRIMARY KEY,
    objectid      BIGINT         NOT NULL,
    modifiedtime  BIGINT         NOT NULL,
    username      VARCHAR(256)   NOT NULL,
    changetype    VARCHAR(256)   NOT NULL,
    changename    VARCHAR(256)   NOT NULL,
    identifier    VARCHAR(1024)  NULL,
    changecomment TEXT           NULL,
    textbefore    VARCHAR(65535) NULL,
    textafter     VARCHAR(65535) NULL
);

CREATE UNIQUE INDEX auditlog_id_in ON auditlog(id);
CREATE INDEX auditlog_objectid_in ON auditlog(objectid);
CREATE INDEX auditlog_identifier_in ON auditlog(identifier);
