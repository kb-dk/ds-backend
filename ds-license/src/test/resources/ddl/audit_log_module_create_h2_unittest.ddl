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
