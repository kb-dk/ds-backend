ALTER TABLE auditlog
    RENAME COLUMN changecomment TO identifier;

CREATE INDEX auditlog_identifier_in ON auditlog(identifier);

ALTER TABLE auditlog
    ADD changecomment TEXT NULL;
