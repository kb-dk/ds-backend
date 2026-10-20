CREATE TABLE summaries (
    id UUID PRIMARY KEY,
    file_id UUID UNIQUE NOT NULL,
    summary TEXT NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL,
    job_id CHARACTER VARYING NOT NULL,
    inserted TIMESTAMP WITH TIME ZONE NOT NULL,
    updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX summaries_summary_idx ON summaries(summary);
CREATE INDEX summaries_created_idx ON summaries(created);

COMMENT ON TABLE summaries IS 'Table of summaries data';
COMMENT ON COLUMN summaries.id IS 'Unique UUID id';
COMMENT ON COLUMN summaries.file_id IS 'Filename without extension from Preservica. Is the same as presentation copy from Preservica and a field in Solr';
COMMENT ON COLUMN summaries.summary IS 'AI generated summary of the broadcast';
COMMENT ON COLUMN summaries.created IS 'Timestamp of execution of summaries matching job';
COMMENT ON COLUMN summaries.job_id IS 'Id of summaries matching job. Only useful for logging and debugging';
COMMENT ON COLUMN summaries.inserted IS 'When did team Arkivblik insert the row from the foreign data wrapper table';
COMMENT ON COLUMN summaries.updated IS 'When did team Arkivblik update the row from the foreign data wrapper table';
