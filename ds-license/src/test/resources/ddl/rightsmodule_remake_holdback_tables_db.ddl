
DROP TABLE dr_holdback_map;
DROP TABLE dr_holdback_rules;

CREATE TABLE dr_holdback_rules (
    id BIGINT PRIMARY KEY,
    dr_holdback_value VARCHAR(256) UNIQUE,
    name VARCHAR(256),
    days int
);

CREATE UNIQUE INDEX dr_holdback_rules_id_in ON dr_holdback_rules(id); -- Is this needed, since we dont really use the id in searches?
CREATE INDEX dr_holdback_rules_dr_holdback_value_in ON dr_holdback_rules(dr_holdback_value); -- Is this needed?
CREATE INDEX dr_holdback_rules_name_in ON dr_holdback_rules(name);

/*
 Table to map content and/or form to holdback
 */
CREATE TABLE dr_holdback_ranges (
    id BIGINT PRIMARY KEY,
    content_range_from INTEGER NOT NULL,
    content_range_to INTEGER NOT NULL,
    form_range_from INTEGER NOT NULL,
    form_range_to INTEGER NOT NULL,
    dr_holdback_value VARCHAR(256) references dr_holdback_rules(dr_holdback_value)
);

CREATE UNIQUE INDEX dr_holdback_ranges_id_in ON dr_holdback_ranges(id); -- Is this needed, since we dont really use the id in searches?
CREATE INDEX dr_holdback_ranges_dr_holdback_value_in ON dr_holdback_ranges(dr_holdback_value);
CREATE INDEX dr_holdback_ranges_content_form_in ON dr_holdback_ranges(content_range_from, content_range_to, form_range_from, form_range_to);