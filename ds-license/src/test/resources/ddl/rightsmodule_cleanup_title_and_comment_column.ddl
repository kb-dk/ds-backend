WITH kb_comment AS (
	SELECT
		ri.id,
		split_part(comment, ', KB Kommentar:', 1) AS title
	FROM
		restricted_ids ri
), dr_klausulering1 AS (
	SELECT
		ri.id,
		split_part(kb_comment.title, ', , DR Klausulering:', 1) AS title
	FROM
		restricted_ids ri
	JOIN
		kb_comment ON ri.id = kb_comment.id
), dr_klausulering2 AS (
	SELECT
		ri.id,
		split_part(dr_klausulering1.title, ', DR Klausulering:', 1) AS title
	FROM
		restricted_ids ri
	JOIN
		dr_klausulering1 ON ri.id = dr_klausulering1.id
), dr_restriction1 AS (
	SELECT
		ri.id,
		split_part(dr_klausulering2.title, ', DR Restriction:', 1) AS title
	FROM
		restricted_ids ri
	JOIN
		dr_klausulering2 ON ri.id = dr_klausulering2.id
), dr_restriction2 AS (
	SELECT
		ri.id,
		split_part(dr_restriction1.title, '. DR Restriction:', 1) AS title
	FROM
		restricted_ids ri
	JOIN
		dr_restriction1 ON ri.id = dr_restriction1.id
), remove_titel AS (
	SELECT
		ri.id,
		regexp_replace(dr_restriction2.title, '.*(Titel: )|(Title: )|(Titel klausulering på titlen )', '') AS title
	FROM
		restricted_ids ri
	JOIN
		dr_restriction2 ON ri.id = dr_restriction2.id
), remove_strict_titel AS (
	SELECT
		ri.id,
		split_part(remove_titel.title, ', Ukendt årsag til klausulering, skal undersøges', 1) AS title
	FROM
		restricted_ids ri
	JOIN
		remove_titel ON ri.id = remove_titel.id
), remove_text_titel AS (
	SELECT
		ri.id,
		regexp_replace(remove_strict_titel.title, '(FEJL MÅ IKKE SENDES)|(\(MÅ IKKE SENDES\) - )|(\(\(MÅ IKKE SENDES\))|(\(MÅ IKKE SENDES\))|(MÅ IKKE SENDES:)|(MÅ IKKE SENDES - )|( - MÅ IKKE SENDES)|(. MÅ IKKE SENDES)|(MÅ IKKE SENDES!)|(MÅ IKKE SENDES)|(\(MÅ IKKE UDSENDES\))|(\(MÅ IKKE UDSENDES:\))|(\(MÅ IKKE UDSENDES\) : )|(\(MÅ IKKE UDSENDES\).)|( - MÅ IKKE UDSENDES)|(. \(MÅ ikke udsendes\))|( - MÅ IKKE GENUDSENDES)|(MÅ IKKE GENUDSENDES - )|(FEJL MÅ IKKE GENUDSENDES !!!!!!!)|(\(MÅ IKKE GENUDSENDES\))|(\(MÅ KKE GENUDSENDES\))|(MÅ IKKE GENUDSENDES)|(\(MÅ IKKE BRUGES\))|(MÅ IKKE GENUDSENDES ELLER GENANVENDES - )|(MÅ IKKE GENUDSENDES ELLER GENANVENDES)|(MÅ IKKE BRUGES)|(MÅ IKKE GENBRUGES - )|(DENNE VERSION MÅ IKKE GENBRUGES)', '') AS title
	FROM
		restricted_ids ri
	JOIN
		remove_strict_titel ON ri.id = remove_strict_titel.id
), remove_comma_titel AS (
	SELECT
		ri.id,
		regexp_replace(remove_text_titel.title, ', , ', '') AS title
	FROM
		restricted_ids ri
	JOIN
		remove_text_titel ON ri.id = remove_text_titel.id
), btrim_titel AS (
	SELECT
		ri.id,
		btrim(remove_comma_titel.title, E' \t\n') AS title
	FROM
		restricted_ids ri
	JOIN
		remove_comma_titel ON ri.id = remove_comma_titel.id
)
UPDATE restricted_ids
SET
	title = NULLIF(btrim_titel.title, '')
FROM
	btrim_titel
WHERE
	restricted_ids.id = btrim_titel.id
	AND
	(
		restricted_ids.id_type = 'DR_PRODUCTION_ID'
		OR
		restricted_ids.id_type = 'DS_ID'
		OR
		restricted_ids.id_type = 'STRICT_TITLE'
	);


WITH kb_comment AS (
	SELECT
		ri.id,
		regexp_replace(comment, '.*, KB Kommentar:', 'KB Kommentar:') AS new_comment
	FROM
		restricted_ids ri
), dr_klausulering1 AS (
	SELECT
		ri.id,
		regexp_replace(kb_comment.new_comment, '(Titel:|Title:).*, , DR Klausulering:', 'DR Klausulering:') AS new_comment
	FROM
		restricted_ids ri
	JOIN
		kb_comment ON ri.id = kb_comment.id
), dr_klausulering2 AS (
	SELECT
		ri.id,
		regexp_replace(dr_klausulering1.new_comment, '(Titel:|Title:).*, DR Klausulering:', 'DR Klausulering:') AS new_comment
	FROM
		restricted_ids ri
	JOIN
		dr_klausulering1 ON ri.id = dr_klausulering1.id
), dr_restriction1 AS (
	SELECT
		ri.id,
		regexp_replace(dr_klausulering2.new_comment, '(Titel:|Title:).*, DR Restriction:', 'DR Restriction:') AS new_comment
	FROM
		restricted_ids ri
	JOIN
		dr_klausulering2 ON ri.id = dr_klausulering2.id
), dr_restriction2 AS (
	SELECT
		ri.id,
		regexp_replace(dr_restriction1.new_comment, '(Titel:|Title:).*. DR Restriction:', 'DR Restriction:') AS new_comment
	FROM
		restricted_ids ri
	JOIN
		dr_restriction1 ON ri.id = dr_restriction1.id
), remove_bold_comment AS (
	SELECT
		ri.id,
		regexp_replace(dr_restriction2.new_comment , '(<[bB]>\s+)|(<[bB]>)|(\s+</[bB]>)|(</[bB]>)', '', 'g') AS new_comment
	FROM
		restricted_ids ri
	JOIN
		dr_restriction2 ON ri.id = dr_restriction2.id
), remove_comma_comment AS (
	SELECT
		ri.id,
		regexp_replace(remove_bold_comment.new_comment , ', , ', '') AS new_comment
	FROM
		restricted_ids ri
	JOIN
		remove_bold_comment ON ri.id = remove_bold_comment.id
), btrim_comment AS (
	SELECT
		ri.id,
		btrim(remove_comma_comment.new_comment, E' \t\n') AS new_comment
	FROM
		restricted_ids ri
	JOIN
		remove_comma_comment ON ri.id = remove_comma_comment.id
)
UPDATE restricted_ids
SET
	comment = NULLIF(btrim_comment.new_comment, '')
FROM
	btrim_comment
WHERE
	restricted_ids.id = btrim_comment.id