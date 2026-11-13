-- THIS IS NOT FOR FLYWAY!!!
-- This is for being able to unit test integration to a foreign database we don't own

CREATE TABLE summaries (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL,
    summary VARCHAR NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL,
    job_id CHARACTER VARYING NOT NULL
);

CREATE INDEX ON summaries (created, id);
CREATE INDEX ON summaries (file_id, created, id);

COMMENT ON TABLE summaries IS 'Table of summaries data';
COMMENT ON COLUMN summaries.id IS 'Unique UUID id';
COMMENT ON COLUMN summaries.file_id IS 'Filename without extension from Preservica. Is the same as presentation copy from Preservica and a field in Solr';
COMMENT ON COLUMN summaries.summary IS 'AI generated summary of the broadcast';
COMMENT ON COLUMN summaries.created IS 'Timestamp of execution of summaries matching job';
COMMENT ON COLUMN summaries.job_id IS 'Id of summaries matching job. Only useful for logging and debugging';
