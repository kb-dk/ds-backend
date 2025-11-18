ALTER TABLE auditlog
    RENAME COLUMN changecomment TO identifier;

ALTER TABLE auditlog
    ALTER COLUMN identifier SET NOT NULL;

CREATE INDEX auditlog_identifier_in ON auditlog(identifier);

ALTER TABLE auditlog
    ADD changecomment TEXT NULL;
