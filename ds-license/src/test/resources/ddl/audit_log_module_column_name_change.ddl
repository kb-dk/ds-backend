ALTER TABLE auditlog
    RENAME COLUMN changecomment to identifier;

ALTER TABLE auditlog
    ADD changecomment TEXT;