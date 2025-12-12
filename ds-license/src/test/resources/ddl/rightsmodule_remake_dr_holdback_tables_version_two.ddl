
DROP TABLE dr_holdback_ranges;
DROP TABLE dr_holdback_rules;

CREATE TABLE IF NOT EXISTS dr_holdback_categories (
    id   BIGINT PRIMARY KEY,
    "key"  VARCHAR(256) UNIQUE,
    name VARCHAR(256),
    days int
);

CREATE UNIQUE INDEX IF NOT EXISTS dr_holdback_categories_id_in ON dr_holdback_categories (id);
CREATE INDEX IF NOT EXISTS dr_holdback_categories_key_in ON dr_holdback_categories ("key");
CREATE INDEX IF NOT EXISTS dr_holdback_categories_name_in ON dr_holdback_categories (name);

/*
 Table to map content and/or form to holdback
 */
CREATE TABLE IF NOT EXISTS dr_holdback_ranges (
    id                       BIGINT PRIMARY KEY,
    content_range_from       INTEGER NOT NULL,
    content_range_to         INTEGER NOT NULL,
    form_range_from          INTEGER NOT NULL,
    form_range_to            INTEGER NOT NULL,
    dr_holdback_category_key VARCHAR(256) references dr_holdback_categories ("key")
);

CREATE UNIQUE INDEX IF NOT EXISTS dr_holdback_ranges_id_in ON dr_holdback_ranges (id);
CREATE INDEX IF NOT EXISTS dr_holdback_ranges_dr_holdback_category_key_in ON dr_holdback_ranges (dr_holdback_category_key);
CREATE INDEX IF NOT EXISTS dr_holdback_ranges_content_form_in ON dr_holdback_ranges (content_range_from, content_range_to, form_range_from, form_range_to);
