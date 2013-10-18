COPY compound_function (mark, title) TO '/home/user/delta/common/etc/testdata/functions.csv' WITH DELIMITER ';' CSV;
COPY compound_series (mark, title) TO '/home/user/delta/common/etc/testdata/series.csv' WITH DELIMITER ';' CSV;
COPY volume (mark, title) TO '/home/user/delta/common/etc/testdata/volumes.csv' WITH DELIMITER ';' CSV;

COPY (
SELECT DISTINCT substring(trim(from substring(compilator from '^[^(]*')) from '^(.*) [^ ]+$') AS foo
FROM document WHERE compilator != '' ORDER BY foo
) TO '/home/user/delta/common/etc/testdata/users-firstnames.csv' WITH DELIMITER ';' CSV;

COPY (
SELECT DISTINCT substring(trim(from substring(compilator from '^[^(]*')) from '^.* ([^ ]+)$') AS foo
FROM document WHERE compilator != '' ORDER BY foo
) TO '/home/user/delta/common/etc/testdata/users-lastnames.csv' WITH DELIMITER ';' CSV;

-- Annab warningu aga teeb ära
COPY (
SELECT DISTINCT trim(from substring(compilator from '\\((.*)\\)$')) AS foo
FROM document WHERE compilator != '' ORDER BY foo
) TO '/home/user/delta/common/etc/testdata/orgunits.csv' WITH DELIMITER ';' CSV;

COPY (
SELECT DISTINCT party AS foo
FROM document WHERE party != '' ORDER BY foo
) TO '/home/user/delta/common/etc/testdata/contacts.csv' WITH DELIMITER ';' CSV;

COPY (
SELECT DISTINCT title AS foo
FROM document WHERE title != '' ORDER BY foo
) TO '/home/user/delta/common/etc/testdata/doctitles.csv' WITH DELIMITER ';' CSV;

COPY (
SELECT DISTINCT reg_number AS foo
FROM document WHERE reg_number != '' ORDER BY foo
) TO '/home/user/delta/common/etc/testdata/regnumbers.csv' WITH DELIMITER ';' CSV;

COPY (
SELECT DISTINCT sender_reg_number AS foo
FROM document WHERE sender_reg_number != '' ORDER BY foo
) TO '/home/user/delta/common/etc/testdata/senderregnumbers.csv' WITH DELIMITER ';' CSV;

COPY (
SELECT DISTINCT transmittal_mode AS foo
FROM document WHERE transmittal_mode != '' ORDER BY foo
) TO '/home/user/delta/common/etc/testdata/transmittalmodes.csv' WITH DELIMITER ';' CSV;

COPY (
SELECT DISTINCT access_restriction_reason AS foo
FROM document WHERE access_restriction_reason != '' ORDER BY foo
) TO '/home/user/delta/common/etc/testdata/accessrestrictionreasons.csv' WITH DELIMITER ';' CSV;

COPY (
SELECT DISTINCT access_restriction_end_desc AS foo
FROM document WHERE access_restriction_end_desc != '' ORDER BY foo
) TO '/home/user/delta/common/etc/testdata/accessrestrictionenddescs.csv' WITH DELIMITER ';' CSV;

COPY (
SELECT '' || document.organization_id || '/' || file.id, file.mime_type, file.encoding, file.size, file.name, file.title FROM file
JOIN document ON file.document_id = document.id
ORDER BY file.id
) TO '/home/user/delta/common/etc/testdata/files.csv' WITH DELIMITER ';' CSV;
