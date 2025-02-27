CREATE TYPE restrictedIDType AS ENUM ('dr_productionId','ds_id','ownproduction_code','strict_title');

CREATE TABLE RESTRICTED_IDS (
    id VARCHAR(256),
    idType restrictedIDType,
    system VARCHAR(32),
    comment VARCHAR(256),
    modified_by VARCHAR(256),
    modified_time BIGINT,
    modified_time_human VARCHAR(256),
    PRIMARY KEY (idType,id)
);
