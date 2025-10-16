ALTER TABLE auditlog
    RENAME COLUMN changecomment TO identifier;

ALTER TABLE auditlog
    ADD changecomment TEXT NULL;