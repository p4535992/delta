\o delta-stats2.txt
\pset format unaligned

SELECT CURRENT_TIMESTAMP;

\timing on

\echo 'Database stats'
\qecho 'Database stats'
SELECT * FROM pg_stat_database;
\echo 'Bgwriter stats'
\qecho 'Bgwriter stats'
SELECT * FROM pg_stat_bgwriter;
\echo 'Activity stats'
\qecho 'Activity stats'
SELECT * FROM pg_stat_activity;

\echo 'Table stats'
\qecho 'Table stats'
SELECT * FROM pg_stat_user_tables;
\echo 'Index stats'
\qecho 'Index stats'
SELECT * FROM pg_stat_user_indexes;
\echo 'Table IO stats'
\qecho 'Table IO stats'
SELECT * FROM pg_statio_user_tables;
\echo 'Index IO stats'
\qecho 'Index IO stats'
SELECT * FROM pg_statio_user_indexes;
\echo 'Sequence IO stats'
\qecho 'Sequence IO stats'
SELECT * FROM pg_statio_user_sequences;


\echo 'Table and index sizes (where size >= 1 MB)'
\qecho 'Table and index sizes (where size >= 1 MB)'
SELECT nspname || '.' || relname AS "relation", pg_relation_size(C.oid) / 1024 / 1024 AS "size" FROM pg_class C LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace) WHERE nspname NOT IN ('pg_catalog', 'information_schema') AND pg_relation_size(C.oid) >= 1024 * 1024 ORDER BY size DESC;

\echo 'Sum of table and index sizes where size < 1 MB'
\qecho 'Sum of table and index sizes where size < 1 MB'
SELECT SUM(pg_relation_size(C.oid)) / 1024 / 1024 AS size FROM pg_class C LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace) WHERE nspname NOT IN ('pg_catalog', 'information_schema')  AND pg_relation_size(C.oid) < 1024 * 1024;

\echo 'Node count by parent node, grouped by deleted, store, node type'
\qecho 'Node count by parent node, grouped by deleted, store, node type'
SELECT sum(count), count(*), avg(count), stddev_pop(count), min(count), max(count), node_deleted, protocol, identifier, uri, local_name FROM (
SELECT count(*) AS count, alf_child_assoc.parent_node_id, alf_node.node_deleted, alf_store.protocol, alf_store.identifier, alf_namespace.uri, alf_qname.local_name
FROM alf_node
LEFT JOIN alf_child_assoc ON alf_node.id = alf_child_assoc.child_node_id AND alf_child_assoc.is_primary = TRUE
JOIN alf_store ON alf_node.store_id = alf_store.id
JOIN alf_qname ON alf_qname.id = alf_node.type_qname_id
JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
GROUP BY alf_node.node_deleted, alf_child_assoc.parent_node_id, alf_store.protocol, alf_store.identifier, alf_namespace.uri, alf_qname.local_name
) t
GROUP BY node_deleted, protocol, identifier, uri, local_name
ORDER BY node_deleted, protocol, identifier, uri, local_name;

\echo 'Property count by node, grouped by store, node type'
\qecho 'Property count by node, grouped by store, node type'
SELECT sum(count), count(*), avg(count), stddev_pop(count), min(count), max(count), protocol, identifier, uri, local_name FROM (
SELECT count(*) AS count, alf_node_properties.node_id, alf_store.protocol, alf_store.identifier, alf_namespace.uri, alf_qname.local_name
FROM alf_node_properties
JOIN alf_node ON alf_node_properties.node_id = alf_node.id
JOIN alf_store ON alf_node.store_id = alf_store.id
JOIN alf_qname ON alf_qname.id = alf_node.type_qname_id
JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
GROUP BY alf_node_properties.node_id, alf_store.protocol, alf_store.identifier, alf_namespace.uri, alf_qname.local_name
) t
GROUP BY protocol, identifier, uri, local_name
ORDER BY protocol, identifier, uri, local_name;

\echo 'Aspect count by node, grouped by store, node type'
\qecho 'Aspect count by node, grouped by store, node type'
SELECT sum(count), count(*), avg(count), stddev_pop(count), min(count), max(count), protocol, identifier, uri, local_name FROM (
SELECT count(*) AS count, alf_node_aspects.node_id, alf_store.protocol, alf_store.identifier, alf_namespace.uri, alf_qname.local_name
FROM alf_node_aspects
JOIN alf_node ON alf_node_aspects.node_id = alf_node.id
JOIN alf_store ON alf_node.store_id = alf_store.id
JOIN alf_qname ON alf_qname.id = alf_node.type_qname_id
JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
GROUP BY alf_node_aspects.node_id, alf_store.protocol, alf_store.identifier, alf_namespace.uri, alf_qname.local_name 
) t
GROUP BY protocol, identifier, uri, local_name
ORDER BY protocol, identifier, uri, local_name;

\echo 'Asssoc count by source and target node, grouped by source node store, source node type, target node store, target node type'
\qecho 'Asssoc count by source and target node, grouped by source node store, source node type, target node store, target node type'
SELECT sum(count), count(*), avg(count), stddev_pop(count), min(count), max(count), source_protocol, source_identifier, source_uri, source_local_name, target_protocol, target_identifier, target_uri, target_local_name FROM (
SELECT count(*) AS count, a.source_node_id, s1.protocol source_protocol, s1.identifier source_identifier, ns1.uri source_uri, q1.local_name source_local_name, a.target_node_id, s2.protocol target_protocol, s2.identifier target_identifier, ns2.uri target_uri, q2.local_name target_local_name
FROM alf_node_assoc a
JOIN alf_node n1 ON a.source_node_id = n1.id
JOIN alf_store s1 ON n1.store_id = s1.id
JOIN alf_qname q1 ON q1.id = n1.type_qname_id
JOIN alf_namespace ns1 ON q1.ns_id = ns1.id
JOIN alf_node n2 ON a.target_node_id = n2.id
JOIN alf_store s2 ON n2.store_id = s2.id
JOIN alf_qname q2 ON q2.id = n2.type_qname_id
JOIN alf_namespace ns2 ON q2.ns_id = ns2.id
GROUP BY a.source_node_id, s1.protocol, s1.identifier, ns1.uri, q1.local_name, a.target_node_id, s2.protocol, s2.identifier, ns2.uri, q2.local_name
) t
GROUP BY source_protocol, source_identifier, source_uri, source_local_name, target_protocol, target_identifier, target_uri, target_local_name
ORDER BY source_protocol, source_identifier, source_uri, source_local_name, target_protocol, target_identifier, target_uri, target_local_name;

\echo 'Transaction count by hour'
\qecho 'Transaction count by hour'
SELECT sum(count), count(*), avg(count), stddev_pop(count), min(count), max(count) FROM (
SELECT count(*), commit_time_ms / 3600000 AS hour
FROM alf_transaction
GROUP BY hour
) t;

\echo 'Node count by transaction'
\qecho 'Node count by transaction'
SELECT sum(count), count(*), avg(count), stddev_pop(count), min(count), max(count) FROM (
SELECT count(*), transaction_id
FROM alf_node
GROUP BY transaction_id
) t;

\echo 'Node count by creation date, grouped by creation date, deleted, store, node type'
\qecho 'Node count by creation date, grouped by creation date, deleted, store, node type'
SELECT count(*), substring(alf_node.audit_created from 1 for 10) AS created_date, alf_node.node_deleted, alf_store.protocol, alf_store.identifier, alf_namespace.uri, alf_qname.local_name
FROM alf_node
JOIN alf_store ON alf_node.store_id = alf_store.id
JOIN alf_qname ON alf_qname.id = alf_node.type_qname_id
JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
WHERE alf_node.audit_created IS NOT NULL
GROUP BY created_date, alf_node.node_deleted, alf_store.protocol, alf_store.identifier, alf_namespace.uri, alf_qname.local_name
ORDER BY created_date, alf_node.node_deleted, alf_store.protocol, alf_store.identifier, alf_namespace.uri, alf_qname.local_name;

\echo 'AccessRestriction property value count, grouped by node store, node type, parent node type, node objectTypeId property value'
\qecho 'AccessRestriction property value count, grouped by node store, node type, parent node type, node objectTypeId property value'
SELECT count(*), alf_node_properties.string_value AS access_restriction, alf_store.protocol, alf_store.identifier, ns2.uri, q2.local_name, ns3.uri, q3.local_name, p2.string_value as object_type_id
FROM alf_node_properties
JOIN alf_qname ON alf_qname.id = alf_node_properties.qname_id
JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
JOIN alf_node ON alf_node_properties.node_id = alf_node.id
JOIN alf_qname q2 ON q2.id = alf_node.type_qname_id
JOIN alf_namespace ns2 ON q2.ns_id = ns2.id
JOIN alf_store ON alf_node.store_id = alf_store.id
LEFT JOIN alf_child_assoc ON alf_node.id = alf_child_assoc.child_node_id AND alf_child_assoc.is_primary = TRUE
JOIN alf_node n3 ON alf_child_assoc.parent_node_id = n3.id
JOIN alf_qname q3 ON q3.id = n3.type_qname_id
JOIN alf_namespace ns3 ON q3.ns_id = ns3.id
LEFT JOIN alf_node_properties p2 ON alf_node.id = p2.node_id AND p2.qname_id IN (
   SELECT alf_qname.id
   FROM alf_qname
   JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
   WHERE alf_namespace.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0' AND alf_qname.local_name = 'objectTypeId'
)
WHERE alf_namespace.uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0' AND alf_qname.local_name = 'accessRestriction'
GROUP BY access_restriction, alf_store.protocol, alf_store.identifier, ns2.uri, q2.local_name, ns3.uri, q3.local_name, object_type_id
ORDER BY access_restriction, alf_store.protocol, alf_store.identifier, ns2.uri, q2.local_name, ns3.uri, q3.local_name, object_type_id
;

\echo 'File node count, grouped by deleted, store, node type, node active property value'
\qecho 'File node count, grouped by deleted, store, node type, node active property value'
SELECT sum(count), count(*), avg(count), stddev_pop(count), min(count), max(count), node_deleted, protocol, identifier, uri, local_name, active FROM (
SELECT count(*) AS count, alf_child_assoc.parent_node_id, alf_node.node_deleted, alf_store.protocol, alf_store.identifier, ns2.uri, q2.local_name, alf_node_properties.boolean_value AS active
FROM alf_node
LEFT JOIN alf_child_assoc ON alf_node.id = alf_child_assoc.child_node_id AND alf_child_assoc.is_primary = TRUE
JOIN alf_store ON alf_node.store_id = alf_store.id
JOIN alf_qname ON alf_qname.id = alf_node.type_qname_id
JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
JOIN alf_node n2 ON alf_child_assoc.parent_node_id = n2.id
JOIN alf_qname q2 ON q2.id = n2.type_qname_id
JOIN alf_namespace ns2 ON q2.ns_id = ns2.id
LEFT JOIN alf_node_properties ON alf_node.id = alf_node_properties.node_id AND alf_node_properties.qname_id IN (
   SELECT alf_qname.id
   FROM alf_qname
   JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
   WHERE alf_namespace.uri = 'http://alfresco.webmedia.ee/model/file/1.0' AND alf_qname.local_name = 'active'
)
WHERE alf_namespace.uri = 'http://www.alfresco.org/model/content/1.0' AND alf_qname.local_name = 'content'
GROUP BY alf_child_assoc.parent_node_id, alf_node.node_deleted, alf_store.protocol, alf_store.identifier, ns2.uri, q2.local_name, alf_node_properties.boolean_value
) t
GROUP BY node_deleted, protocol, identifier, uri, local_name, active
ORDER BY node_deleted, protocol, identifier, uri, local_name, active;

\echo 'Content property size count, grouped by mimeType, encoding, store, node type, parent node type, node active property value, node accessRestriction property value'
\qecho 'Content property size count, grouped by mimeType, encoding, store, node type, parent node type, node active property value, node accessRestriction property value'
SELECT count(*), sum(content_size), avg(content_size), stddev_pop(content_size), min(content_size), max(content_size), 
alf_mimetype.mimetype_str, alf_encoding.encoding_str, alf_store.protocol, alf_store.identifier, ns2.uri, q2.local_name, ns3.uri, q3.local_name, p2.boolean_value AS active, p3.string_value AS access_restriction
FROM alf_node_properties
JOIN alf_qname ON alf_qname.id = alf_node_properties.qname_id
JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
JOIN alf_content_data ON alf_node_properties.long_value = alf_content_data.id
JOIN alf_content_url ON alf_content_data.content_url_id = alf_content_url.id
JOIN alf_node ON alf_node_properties.node_id = alf_node.id
JOIN alf_qname q2 ON q2.id = alf_node.type_qname_id
JOIN alf_namespace ns2 ON q2.ns_id = ns2.id
JOIN alf_store ON alf_node.store_id = alf_store.id
LEFT JOIN alf_child_assoc ON alf_node.id = alf_child_assoc.child_node_id AND alf_child_assoc.is_primary = TRUE
JOIN alf_node n3 ON alf_child_assoc.parent_node_id = n3.id
JOIN alf_qname q3 ON q3.id = n3.type_qname_id
JOIN alf_namespace ns3 ON q3.ns_id = ns3.id
JOIN alf_mimetype ON alf_content_data.content_mimetype_id = alf_mimetype.id
JOIN alf_encoding ON alf_content_data.content_encoding_id = alf_encoding.id
LEFT JOIN alf_node_properties p2 ON alf_node.id = p2.node_id AND p2.qname_id IN (
   SELECT alf_qname.id
   FROM alf_qname
   JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
   WHERE alf_namespace.uri = 'http://alfresco.webmedia.ee/model/file/1.0' AND alf_qname.local_name = 'active'
)
LEFT JOIN alf_node_properties p3 ON n3.id = p3.node_id AND p3.qname_id IN (
   SELECT alf_qname.id
   FROM alf_qname
   JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id
   WHERE alf_namespace.uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0' AND alf_qname.local_name = 'accessRestriction'
)
WHERE alf_namespace.uri = 'http://www.alfresco.org/model/content/1.0' AND alf_qname.local_name = 'content'
GROUP BY alf_mimetype.mimetype_str, alf_encoding.encoding_str, alf_store.protocol, alf_store.identifier, ns2.uri, q2.local_name, ns3.uri, q3.local_name, active, access_restriction
ORDER BY alf_mimetype.mimetype_str, alf_encoding.encoding_str, alf_store.protocol, alf_store.identifier, ns2.uri, q2.local_name, ns3.uri, q3.local_name, active, access_restriction;
