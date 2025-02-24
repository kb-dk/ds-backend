CREATE TABLE IF NOT EXISTS RESTRICTED_IDS (
                         id VARCHAR(256) NOT NULL,
                         idType VARCHAR(32) NOT NULL,
                         platform VARCHAR(32),
                         comment VARCHAR(256),
                         modified_by VARCHAR(256),
                         modified_time BIGINT,
                         modified_time_human VARCHAR(256),
                         PRIMARY KEY (idType,id,platform)
);
