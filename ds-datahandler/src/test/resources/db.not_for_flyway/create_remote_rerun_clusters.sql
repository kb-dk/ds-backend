-- THIS IS NOT FOR FLYWAY!!!
-- This is for being able to unit test integration to a foreign database we don't own

CREATE TABLE clusters (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL,
    rerun_cluster_id UUID NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL,
    job_id CHARACTER VARYING NOT NULL
);

CREATE INDEX clusters_file_id_idx ON clusters(file_id);
CREATE INDEX clusters_rerun_cluster_id_idx ON clusters(rerun_cluster_id);
CREATE INDEX clusters_created_idx ON clusters(created);

COMMENT ON TABLE clusters IS 'Table of rerun clusters data';
COMMENT ON COLUMN clusters.id IS 'Unique UUID id';
COMMENT ON COLUMN clusters.file_id IS 'Filename without extension from Preservica. Is the same as presentation copy from Preservica and a field in Solr';
COMMENT ON COLUMN clusters.rerun_cluster_id IS 'UUID id of a rerun cluster. Multiple file_id can share the same rerun_cluster_id';
COMMENT ON COLUMN clusters.created IS 'Timestamp of execution of rerun clusters matching job';
COMMENT ON COLUMN clusters.job_id IS 'Id of rerun clusters matching job. Only useful for logging and debugging';

