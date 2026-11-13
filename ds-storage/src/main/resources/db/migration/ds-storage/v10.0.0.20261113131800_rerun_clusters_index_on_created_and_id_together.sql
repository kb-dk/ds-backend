DROP INDEX CONCURRENTLY rerun_clusters_created_idx;

CREATE INDEX ON rerun_clusters (created, id);