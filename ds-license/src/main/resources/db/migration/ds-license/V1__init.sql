CREATE TABLE PRESENTATIONTYPES (
       ID BIGINT PRIMARY KEY,
       KEY_ID VARCHAR(256) NOT NULL,
       VALUE_DK VARCHAR(256) NOT NULL,
       VALUE_EN VARCHAR(256) NOT NULL
);
CREATE UNIQUE INDEX PRESTYPE_ID_IN ON PRESENTATIONTYPES(ID);

CREATE TABLE GROUPTYPES (
       ID BIGINT PRIMARY KEY,
       KEY_ID VARCHAR(256) NOT NULL,
       VALUE_DK VARCHAR(256) NOT NULL,
       VALUE_EN VARCHAR(256) NOT NULL,
       RESTRICTION BOOLEAN NOT NULL,
       QUERYSTRING VARCHAR(2048) NOT NULL,
       DESCRIPTION_DK VARCHAR(512) NOT NULL,
       DESCRIPTION_EN VARCHAR(512) NOT NULL
);
CREATE UNIQUE INDEX  GROUPTYPE_ID_IN ON GROUPTYPES(ID);

CREATE TABLE ATTRIBUTETYPES (
       ID BIGINT PRIMARY KEY,
       VALUE_ORG VARCHAR(256) NOT NULL
);

CREATE UNIQUE INDEX ATTTYPE_ID_IN ON ATTRIBUTETYPES(ID);

CREATE TABLE LICENSE (
       ID BIGINT PRIMARY KEY,
       NAME VARCHAR(256) NOT NULL,
       NAME_EN VARCHAR(256) NOT NULL,
       DESCRIPTION_DK VARCHAR(1024) NOT NULL,
       DESCRIPTION_EN VARCHAR(1024) NOT NULL,
       VALIDFROM VARCHAR(32) NOT NULL,
       VALIDTO VARCHAR(32) NOT NULL
);
CREATE UNIQUE INDEX LICENSE_ID_IN ON LICENSE(ID);

CREATE TABLE ATTRIBUTEGROUP (
       ID BIGINT PRIMARY KEY,
       NUMBER INT NOT NULL,
       LICENSEID BIGINT NOT NULL

);
CREATE UNIQUE INDEX  ATTRGRP_ID_IN ON ATTRIBUTEGROUP(ID);
CREATE INDEX ATTRGRP_LICENSEID_IN ON ATTRIBUTEGROUP(LICENSEID);

CREATE TABLE ATTRIBUTE (
       ID BIGINT PRIMARY KEY,
       NAME VARCHAR(256) NOT NULL,
       ATTRIBUTEGROUPID BIGINT NOT NULL

);
CREATE UNIQUE INDEX ATTRIBUTE_ID_IN ON ATTRIBUTE(ID);
CREATE INDEX  ATTR_GROUPID_IN ON ATTRIBUTE(ATTRIBUTEGROUPID);

CREATE TABLE VALUE_ORG (
       ID BIGINT PRIMARY KEY,
       VALUE_ORG VARCHAR(256) NOT NULL,
       ATTRIBUTEID BIGINT NOT NULL

);
CREATE UNIQUE INDEX  VALUE_ID_IN ON VALUE_ORG(ID);
CREATE INDEX VALUE_ATTRID_IN ON VALUE_ORG(ATTRIBUTEID);

CREATE TABLE LICENSECONTENT (
       ID BIGINT PRIMARY KEY,
       NAME VARCHAR(256) NOT NULL,
       LICENSEID BIGINT NOT NULL

);
CREATE UNIQUE INDEX  LICCONTENT_ID_IN ON LICENSECONTENT(ID);
CREATE INDEX LICCONTENT_LICID_IN ON LICENSECONTENT(LICENSEID);

CREATE TABLE PRESENTATION (
       ID BIGINT PRIMARY KEY,
       NAME VARCHAR(256) NOT NULL,
       LICENSECONTENTID BIGINT NOT NULL

);
CREATE UNIQUE INDEX  PRESENTATION_ID_IN ON PRESENTATION(ID);
CREATE INDEX PRES_LICCONTID_IN ON PRESENTATION(LICENSECONTENTID);

CREATE TABLE auditlog (
    id            BIGINT PRIMARY KEY,
    objectid      BIGINT         NOT NULL,
    modifiedtime  BIGINT         NOT NULL,
    username      VARCHAR(256)   NOT NULL,
    changetype    VARCHAR(256)   NOT NULL,
    changename    VARCHAR(256)   NOT NULL,
    identifier    VARCHAR(1024)  NOT NULL,
    changecomment TEXT           NULL,
    textbefore    VARCHAR(65535) NULL,
    textafter     VARCHAR(65535) NULL
);

CREATE UNIQUE INDEX auditlog_id_in ON auditlog(id);
CREATE INDEX auditlog_objectid_in ON auditlog(objectid);
CREATE INDEX auditlog_identifier_in ON auditlog(identifier);

CREATE TABLE restricted_ids (
    id       BIGINT PRIMARY KEY,
    id_value VARCHAR(256)   NOT NULL,
    id_type  VARCHAR(32)    NOT NULL,
    platform VARCHAR(32)    NOT NULL,
    title    VARCHAR(4096)  NULL,
    comment  VARCHAR(16384) NOT NULL
);

CREATE UNIQUE INDEX unique_restricted_id ON restricted_ids (id_value, id_type, platform);
CREATE UNIQUE INDEX restricted_ids_id_in ON restricted_ids (id);
CREATE INDEX restricted_ids_id_value_platform_in ON restricted_ids (id_value, platform);

CREATE TABLE dr_holdback_categories (
    id     BIGINT PRIMARY KEY,
    "key"  VARCHAR(256) UNIQUE,
    name   VARCHAR(256),
    days   int
);

CREATE UNIQUE INDEX dr_holdback_categories_id_in ON dr_holdback_categories (id);
CREATE INDEX dr_holdback_categories_key_in ON dr_holdback_categories ("key");
CREATE INDEX dr_holdback_categories_name_in ON dr_holdback_categories (name);

/*
 Table to map content and/or form to holdback
 */
CREATE TABLE dr_holdback_ranges (
    id                       BIGINT PRIMARY KEY,
    content_range_from       INTEGER NOT NULL,
    content_range_to         INTEGER NOT NULL,
    form_range_from          INTEGER NOT NULL,
    form_range_to            INTEGER NOT NULL,
    dr_holdback_category_key VARCHAR(256) references dr_holdback_categories ("key")
);

CREATE UNIQUE INDEX dr_holdback_ranges_id_in ON dr_holdback_ranges (id);
CREATE INDEX dr_holdback_ranges_dr_holdback_category_key_in ON dr_holdback_ranges (dr_holdback_category_key);
CREATE INDEX dr_holdback_ranges_content_form_in ON dr_holdback_ranges (content_range_from, content_range_to, form_range_from, form_range_to);