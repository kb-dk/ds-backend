INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.01.01', 'Almene Nyheder', 30);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.01.02', 'Sportsnyheder', 30);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.02', 'Aktualitet og Debat', 2190);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.03', 'Oplysning og Kultur', 2190);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.04', 'Undervisning', 2190);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.05', 'Dramatik & Fiktion', 3650);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.06', 'Musik', 2190);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.07', 'Underholdning', 2190);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.08', 'Sport', 2190);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.09', 'Præsentation', 2190);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.09*', 'Øvrigt (TTV + Pauser)', 2190);
INSERT INTO DR_HOLDBACK_RULES (id, name, days) VALUES ('2.20', 'Sponsorskilte', 2190);

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (1, INT4RANGE(1000, 1900), INT4RANGE(1000, 1000), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (2, INT4RANGE(1000, 1900), INT4RANGE(1100, 1100), '2.01.01');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (3, INT4RANGE(1000, 1900), INT4RANGE(1200, 1500), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (4, INT4RANGE(1000, 1900), INT4RANGE(1600, 1600), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (5, INT4RANGE(1000, 1900), INT4RANGE(1700, 1700), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (6, INT4RANGE(1000, 1900), INT4RANGE(1800, 1800), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (7, INT4RANGE(1000, 1900), INT4RANGE(1900, 1900), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (8, INT4RANGE(1000, 1900), INT4RANGE(2000, 2300), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (9, INT4RANGE(1000, 1900), INT4RANGE(3000, 3900), '2.02');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (10, INT4RANGE(2000, 2900), INT4RANGE(1000, 1000), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (11, INT4RANGE(2000, 2900), INT4RANGE(1200, 1500), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (12, INT4RANGE(2000, 2900), INT4RANGE(1600, 1600), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (13, INT4RANGE(2000, 2900), INT4RANGE(1700, 1700), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (14, INT4RANGE(2000, 2900), INT4RANGE(1800, 1800), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (15, INT4RANGE(2000, 2900), INT4RANGE(1900, 1900), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (16, INT4RANGE(2000, 2900), INT4RANGE(2000, 2300), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (17, INT4RANGE(2000, 2900), INT4RANGE(4000, 4900), '2.03');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (18, INT4RANGE(3000, 3190), INT4RANGE(1000, 1000), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (19, INT4RANGE(3000, 3190), INT4RANGE(1200, 1500), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (20, INT4RANGE(3000, 3190), INT4RANGE(1600, 1600), '2.05');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (21, INT4RANGE(3000, 3190), INT4RANGE(1700, 1700), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (22, INT4RANGE(3000, 3190), INT4RANGE(1800, 1800), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (23, INT4RANGE(3000, 3190), INT4RANGE(1900, 1900), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (24, INT4RANGE(3000, 3190), INT4RANGE(2000, 2300), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (25, INT4RANGE(3000, 3190), INT4RANGE(3000, 3900), '2.05');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (26, INT4RANGE(3200, 3390), INT4RANGE(1000, 1000), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (27, INT4RANGE(3200, 3390), INT4RANGE(1200, 1500), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (28, INT4RANGE(3200, 3390), INT4RANGE(1600, 1600), '2.06');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (29, INT4RANGE(3200, 3390), INT4RANGE(1700, 1700), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (30, INT4RANGE(3200, 3390), INT4RANGE(1800, 1800), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (31, INT4RANGE(3200, 3390), INT4RANGE(1900, 1900), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (32, INT4RANGE(3200, 3390), INT4RANGE(2000, 2300), '2.06');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (33, INT4RANGE(3200, 3390), INT4RANGE(3000, 3900), '2.06');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (34, INT4RANGE(4000, 4900), INT4RANGE(1000, 1000), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (35, INT4RANGE(4000, 4900), INT4RANGE(1200, 1500), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (36, INT4RANGE(4000, 4900), INT4RANGE(1600, 1600), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (37, INT4RANGE(4000, 4900), INT4RANGE(1700, 1700), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (38, INT4RANGE(4000, 4900), INT4RANGE(1800, 1800), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (39, INT4RANGE(4000, 4900), INT4RANGE(1900, 1900), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (40, INT4RANGE(4000, 4900), INT4RANGE(2000, 2300), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (41, INT4RANGE(4000, 4900), INT4RANGE(3000, 3900), '2.06');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (42, INT4RANGE(5000, 5990), INT4RANGE(1600, 1600), '2.08');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (43, INT4RANGE(5000, 5990), INT4RANGE(2000, 2300), '2.08');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (44, INT4RANGE(5000, 5990), INT4RANGE(3000, 3900), '2.08');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (45, INT4RANGE(5000, 5990), INT4RANGE(4000, 4900), '2.08');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (46, INT4RANGE(5000, 5990), INT4RANGE(5000, 5900), '2.08');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (47, INT4RANGE(5000, 5990), INT4RANGE(6000, 6000), '2.08');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (48, INT4RANGE(5000, 5990), INT4RANGE(6100, 6100), '2.01.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (49, INT4RANGE(5000, 5990), INT4RANGE(6200, 6900), '2.08');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (50, INT4RANGE(6000, 6300), INT4RANGE(1000, 1000), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (51, INT4RANGE(6000, 6300), INT4RANGE(1200, 1500), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (52, INT4RANGE(6000, 6300), INT4RANGE(1600, 1600), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (53, INT4RANGE(6000, 6300), INT4RANGE(1700, 1700), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (54, INT4RANGE(6000, 6300), INT4RANGE(1800, 1800), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (55, INT4RANGE(6000, 6300), INT4RANGE(1900, 1900), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (56, INT4RANGE(6000, 6300), INT4RANGE(2000, 2300), '2.07');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (57, INT4RANGE(6400, 6400), INT4RANGE(1000, 1000), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (58, INT4RANGE(6400, 6400), INT4RANGE(1200, 1500), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (59, INT4RANGE(6400, 6400), INT4RANGE(1600, 1600), '2.05');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (60, INT4RANGE(6400, 6400), INT4RANGE(1700, 1700), '2.05');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (61, INT4RANGE(6400, 6400), INT4RANGE(1800, 1800), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (62, INT4RANGE(6400, 6400), INT4RANGE(1900, 1900), '2.07');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (63, INT4RANGE(6500, 6600), INT4RANGE(1000, 1000), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (64, INT4RANGE(6500, 6600), INT4RANGE(1200, 1500), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (65, INT4RANGE(6500, 6600), INT4RANGE(1600, 1600), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (66, INT4RANGE(6500, 6600), INT4RANGE(1700, 1700), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (67, INT4RANGE(6500, 6600), INT4RANGE(1800, 1800), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (68, INT4RANGE(6500, 6600), INT4RANGE(1900, 1900), '2.07');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (69, INT4RANGE(6700, 6700), INT4RANGE(1000, 1000), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (70, INT4RANGE(6700, 6700), INT4RANGE(1200, 1500), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (71, INT4RANGE(6700, 6700), INT4RANGE(1600, 1600), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (72, INT4RANGE(6700, 6700), INT4RANGE(1700, 1700), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (73, INT4RANGE(6700, 6700), INT4RANGE(1800, 1800), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (74, INT4RANGE(6700, 6700), INT4RANGE(1900, 1900), '2.07');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (75, INT4RANGE(6800, 6900), INT4RANGE(1000, 1000), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (76, INT4RANGE(6800, 6900), INT4RANGE(1200, 1500), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (77, INT4RANGE(6800, 6900), INT4RANGE(1600, 1600), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (78, INT4RANGE(6800, 6900), INT4RANGE(1700, 1700), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (79, INT4RANGE(6800, 6900), INT4RANGE(1800, 1800), '2.07');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (80, INT4RANGE(6800, 6900), INT4RANGE(1900, 1900), '2.07');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (81, INT4RANGE(7100, 7100), INT4RANGE(7000, 7000), '2.09');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (82, INT4RANGE(7120, 7120), INT4RANGE(1000, 1000), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (83, INT4RANGE(7120, 7120), INT4RANGE(1100, 1100), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (84, INT4RANGE(7120, 7120), INT4RANGE(1200, 1500), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (85, INT4RANGE(7120, 7120), INT4RANGE(1600, 1600), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (86, INT4RANGE(7120, 7120), INT4RANGE(1700, 1700), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (87, INT4RANGE(7120, 7120), INT4RANGE(1800, 1800), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (88, INT4RANGE(7120, 7120), INT4RANGE(1900, 1900), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (89, INT4RANGE(7120, 7120), INT4RANGE(2000, 2300), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (90, INT4RANGE(7120, 7120), INT4RANGE(3000, 3900), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (91, INT4RANGE(7120, 7120), INT4RANGE(4000, 4900), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (92, INT4RANGE(7120, 7120), INT4RANGE(5000, 5900), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (93, INT4RANGE(7120, 7120), INT4RANGE(6000, 6000), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (94, INT4RANGE(7120, 7120), INT4RANGE(6100, 6100), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (95, INT4RANGE(7120, 7120), INT4RANGE(6200, 6900), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (96, INT4RANGE(7120, 7120), INT4RANGE(7000, 7000), '2.02');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (97, INT4RANGE(7130, 7130), INT4RANGE(1000, 1000), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (98, INT4RANGE(7130, 7130), INT4RANGE(1100, 1100), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (99, INT4RANGE(7130, 7130), INT4RANGE(1200, 1500), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (100, INT4RANGE(7130, 7130), INT4RANGE(1600, 1600), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (101, INT4RANGE(7130, 7130), INT4RANGE(1700, 1700), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (102, INT4RANGE(7130, 7130), INT4RANGE(1800, 1800), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (103, INT4RANGE(7130, 7130), INT4RANGE(1900, 1900), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (104, INT4RANGE(7130, 7130), INT4RANGE(2000, 2300), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (105, INT4RANGE(7130, 7130), INT4RANGE(3000, 3900), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (106, INT4RANGE(7130, 7130), INT4RANGE(4000, 4900), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (107, INT4RANGE(7130, 7130), INT4RANGE(5000, 5900), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (108, INT4RANGE(7130, 7130), INT4RANGE(6000, 6000), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (109, INT4RANGE(7130, 7130), INT4RANGE(6100, 6100), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (110, INT4RANGE(7130, 7130), INT4RANGE(6200, 6900), '2.03');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (111, INT4RANGE(7130, 7130), INT4RANGE(7000, 7000), '2.03');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (112, INT4RANGE(7140, 7150), INT4RANGE(1000, 1000), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (113, INT4RANGE(7140, 7150), INT4RANGE(1100, 1100), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (114, INT4RANGE(7140, 7150), INT4RANGE(1200, 1500), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (115, INT4RANGE(7140, 7150), INT4RANGE(1600, 1600), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (116, INT4RANGE(7140, 7150), INT4RANGE(1700, 1700), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (117, INT4RANGE(7140, 7150), INT4RANGE(1800, 1800), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (118, INT4RANGE(7140, 7150), INT4RANGE(1900, 1900), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (119, INT4RANGE(7140, 7150), INT4RANGE(2000, 2300), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (120, INT4RANGE(7140, 7150), INT4RANGE(3000, 3900), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (121, INT4RANGE(7140, 7150), INT4RANGE(4000, 4900), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (122, INT4RANGE(7140, 7150), INT4RANGE(5000, 5900), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (123, INT4RANGE(7140, 7150), INT4RANGE(6000, 6000), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (124, INT4RANGE(7140, 7150), INT4RANGE(6100, 6100), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (125, INT4RANGE(7140, 7150), INT4RANGE(6200, 6900), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (126, INT4RANGE(7140, 7150), INT4RANGE(7000, 7000), '2.09*');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (127, INT4RANGE(7160, 7212), INT4RANGE(1100, 1100), '2.09');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (128, INT4RANGE(7160, 7212), INT4RANGE(1800, 1800), '2.09');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (129, INT4RANGE(7220, 7220), INT4RANGE(1000, 1000), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (130, INT4RANGE(7220, 7220), INT4RANGE(1100, 1100), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (131, INT4RANGE(7220, 7220), INT4RANGE(1200, 1500), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (132, INT4RANGE(7220, 7220), INT4RANGE(1600, 1600), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (133, INT4RANGE(7220, 7220), INT4RANGE(1700, 1700), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (134, INT4RANGE(7220, 7220), INT4RANGE(1800, 1800), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (135, INT4RANGE(7220, 7220), INT4RANGE(1900, 1900), '2.02');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (136, INT4RANGE(7220, 7220), INT4RANGE(2000, 2300), '2.02');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (137, INT4RANGE(7230, 7252), INT4RANGE(3000, 3900), '2.09');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (138, INT4RANGE(7260, 7260), INT4RANGE(1000, 1000), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (139, INT4RANGE(7260, 7260), INT4RANGE(1100, 1100), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (140, INT4RANGE(7260, 7260), INT4RANGE(1200, 1500), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (141, INT4RANGE(7260, 7260), INT4RANGE(1600, 1600), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (142, INT4RANGE(7260, 7260), INT4RANGE(1700, 1700), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (143, INT4RANGE(7260, 7260), INT4RANGE(1800, 1800), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (144, INT4RANGE(7260, 7260), INT4RANGE(1900, 1900), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (145, INT4RANGE(7260, 7260), INT4RANGE(2000, 2300), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (146, INT4RANGE(7260, 7260), INT4RANGE(3000, 3900), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (147, INT4RANGE(7260, 7260), INT4RANGE(4000, 4900), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (148, INT4RANGE(7260, 7260), INT4RANGE(5000, 5900), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (149, INT4RANGE(7260, 7260), INT4RANGE(6000, 6000), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (150, INT4RANGE(7260, 7260), INT4RANGE(6100, 6100), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (151, INT4RANGE(7260, 7260), INT4RANGE(6200, 6900), '2.09*');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (152, INT4RANGE(7260, 7260), INT4RANGE(7000, 7000), '2.09*');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (153, INT4RANGE(7270, 7270), INT4RANGE(1000, 1000), '2.20');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (154, INT4RANGE(7270, 7270), INT4RANGE(1100, 1100), '2.20');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (155, INT4RANGE(7270, 7270), INT4RANGE(1200, 1500), '2.20');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (156, INT4RANGE(7270, 7270), INT4RANGE(1600, 1600), '2.20');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (157, INT4RANGE(7270, 7270), INT4RANGE(1700, 1700), '2.20');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (158, INT4RANGE(7270, 7270), INT4RANGE(1800, 1800), '2.20');
INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (159, INT4RANGE(7270, 7270), INT4RANGE(1900, 1900), '2.20');

INSERT INTO DR_HOLDBACK_MAP(id, content_range, form_range, dr_holdback_id) VALUES (160, INT4RANGE(7280, 7290), INT4RANGE(3000, 3900), '2.09');
