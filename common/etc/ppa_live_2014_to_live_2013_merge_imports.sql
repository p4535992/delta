drop table if exists tmp_delta_node_inheritspermissions;
drop table if exists tmp_delta_node_permission;
drop table if exists delta_node_permission;
drop table if exists delta_node_inheritspermissions;
drop table if exists tmp_nodes_deleted_on_merge;
drop table if exists tmp_alf_namespace;
drop table if exists tmp_alf_qname;
drop table if exists tmp_import_alf_node;
drop table if exists tmp_import_alf_node_errors;
drop table if exists tmp_import_overwritten_by_import;
drop table if exists tmp_import_alf_child_assoc;
drop table if exists tmp_import_alf_node_assoc;
drop table if exists tmp_import_alf_node_properties;
drop table if exists tmp_import_alf_node_aspects;
drop table if exists tmp_delta_task;
drop table if exists tmp_mimetype;
drop table if exists tmp_locale;

-- Õigused - viia olemasolevad õigused üle uue õiguste süsteemi tabelitele, kuna versioon 5.1 kasutab juba neid tabeleid
-- ja indekseerimine loeb õiguseid enne, kui õiguste süsteemi üleviimine käivitub.
-- Table: delta_node_inheritspermissions

-- DROP TABLE delta_node_inheritspermissions;

CREATE TABLE if not exists delta_node_inheritspermissions
(
  node_uuid character varying(36) NOT NULL,
  inherits boolean NOT NULL,
  acl_id bigint NOT NULL, -- temporary field for migrating permissions
  CONSTRAINT delta_node_inheritspermissions_pkey PRIMARY KEY (node_uuid)
)
WITH (
  OIDS=FALSE
);

ALTER TABLE delta_node_inheritspermissions
  OWNER TO alfresco;

CREATE TABLE tmp_delta_node_inheritspermissions
(
  node_uuid character varying(36) NOT NULL,
  inherits boolean NOT NULL,
  acl_id bigint NOT NULL);
  
-- Table: delta_node_permission

-- DROP TABLE delta_node_permission;
  
CREATE TABLE if not exists delta_node_permission
(
  node_uuid character varying(36) NOT NULL,
  authority character varying(1024) NOT NULL,
  create_document boolean DEFAULT false NOT NULL,
  view_document_meta_data boolean DEFAULT false NOT NULL,
  edit_document boolean DEFAULT false NOT NULL,
  view_document_files boolean DEFAULT false NOT NULL,
  create_case_file boolean DEFAULT false NOT NULL,
  view_case_file boolean DEFAULT false NOT NULL,
  edit_case_file boolean DEFAULT false NOT NULL,
  participate_at_forum boolean DEFAULT false NOT NULL,
  CONSTRAINT delta_node_permission_pkey PRIMARY KEY (node_uuid, authority)
)
WITH (
  OIDS=FALSE
);

ALTER TABLE delta_node_permission
  OWNER TO alfresco;

CREATE TABLE tmp_delta_node_permission
(
  node_uuid character varying(36) NOT NULL,
  authority character varying(1024) NOT NULL,
  create_document boolean DEFAULT false NOT NULL,
  view_document_meta_data boolean DEFAULT false NOT NULL,
  edit_document boolean DEFAULT false NOT NULL,
  view_document_files boolean DEFAULT false NOT NULL,
  create_case_file boolean DEFAULT false NOT NULL,
  view_case_file boolean DEFAULT false NOT NULL,
  edit_case_file boolean DEFAULT false NOT NULL,
  participate_at_forum boolean DEFAULT false NOT NULL
);

CREATE INDEX ON delta_node_permission (create_document) WHERE create_document = TRUE;
CREATE INDEX ON delta_node_permission (view_document_meta_data) WHERE view_document_meta_data = FALSE;
CREATE INDEX ON delta_node_permission (edit_document) WHERE edit_document = TRUE;
CREATE INDEX ON delta_node_permission (view_document_files) WHERE view_document_files = FALSE;
CREATE INDEX ON delta_node_permission (create_case_file) WHERE create_case_file = TRUE;
CREATE INDEX ON delta_node_permission (view_case_file) WHERE view_case_file = TRUE;
CREATE INDEX ON delta_node_permission (edit_case_file) WHERE edit_case_file = TRUE;
CREATE INDEX ON delta_node_permission (participate_at_forum) WHERE participate_at_forum = TRUE;

 INSERT INTO delta_node_inheritspermissions (node_uuid, inherits, acl_id) 
 	(SELECT node.uuid, 
 		CASE WHEN ((qname.local_name = 'series' AND ns.uri = 'http://alfresco.webmedia.ee/model/series/1.0')
 			OR (qname.local_name = 'documentType' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
 			OR (qname.local_name = 'caseFileType' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')) 
 			THEN FALSE ELSE acl.inherits END, 
 		acl.id
 	FROM alf_node node
 	JOIN alf_access_control_list as acl on node.acl_id = acl.id
 	JOIN alf_qname as qname on node.type_qname_id = qname.id
 	JOIN alf_namespace as ns on qname.ns_id = ns.id
 	JOIN (SELECT acl_id FROM alf_acl_member WHERE pos = 0 GROUP BY acl_id) as grouped_member on grouped_member.acl_id = acl.id
 	WHERE node.type_qname_id in (
 		SELECT qname.id from alf_qname qname JOIN alf_namespace ns on ns.id = qname.ns_id
 		where (qname.local_name = 'document' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
 		OR (qname.local_name = 'volume' AND ns.uri = 'http://alfresco.webmedia.ee/model/volume/1.0')
 		OR (qname.local_name = 'caseFile' AND ns.uri = 'http://alfresco.webmedia.ee/model/casefile/1.0')
 		OR (qname.local_name = 'series' AND ns.uri = 'http://alfresco.webmedia.ee/model/series/1.0')
 		OR (qname.local_name = 'documentType' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
 		OR (qname.local_name = 'caseFileType' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
 		OR (qname.local_name = 'forum' AND ns.uri = 'http://www.alfresco.org/model/forum/1.0')
 	)
 	AND EXISTS (SELECT * FROM alf_access_control_entry ace
 		JOIN alf_acl_member member on ace.id = member.ace_id 
 		WHERE member.pos = 0)
 	AND node.acl_id IS NOT NULL);
 
 INSERT INTO delta_node_permission (node_uuid, authority, create_document, 
 	view_document_meta_data, edit_document, view_document_files, create_case_file, view_case_file, edit_case_file, participate_at_forum)
 	(SELECT node_permission.node_uuid, authority.authority, 
 	CASE WHEN privilegeCount.createDocumentCount > 0
 		THEN TRUE ELSE FALSE
 	END,
	CASE WHEN privilegeCount.viewDocumentMetaDataCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.editDocumentCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.viewDocumentFilesCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.createCaseFileCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.viewCaseFileCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.editCaseFileCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.participateAtForumCount > 0
		THEN TRUE ELSE FALSE
	END
	FROM delta_node_inheritspermissions node_permission
	JOIN (SELECT member.acl_id, entry.authority_id
		FROM alf_acl_member as member 
		JOIN alf_access_control_entry entry on member.ace_id = entry.id
		WHERE (entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'createDocument')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewDocumentMetaData')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocument')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocumentMetaData')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocumentFiles')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewDocumentFiles')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'createCaseFile')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewCaseFile')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editCaseFile')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'participateAtForum')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'DocumentFileRead'))
		AND member.pos = 0
		GROUP BY member.acl_id, entry.authority_id) as grouped_ace on grouped_ace.acl_id = node_permission.acl_id
	JOIN alf_authority authority on authority.id = grouped_ace.authority_id
	LEFT JOIN (SELECT member.acl_id, entry.authority_id, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'createDocument') THEN 1 ELSE 0 END) as createDocumentCount,
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewDocumentMetaData')
						OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'DocumentFileRead') THEN 1 ELSE 0 END) as viewDocumentMetaDataCount, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocument')
						OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocumentMetaData')
						OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocumentFiles') THEN 1 ELSE 0 END) as editDocumentCount, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewDocumentFiles')
						OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'DocumentFileRead') THEN 1 ELSE 0 END) as viewDocumentFilesCount, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'createCaseFile') THEN 1 ELSE 0 END) as createCaseFileCount, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewCaseFile') THEN 1 ELSE 0 END) as viewCaseFileCount, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editCaseFile') THEN 1 ELSE 0 END) as editCaseFileCount,
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'participateAtForum') 
						OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'DocumentFileRead') THEN 1 ELSE 0 END) as participateAtForumCount
		FROM alf_acl_member as member 
		JOIN alf_access_control_entry entry on member.ace_id = entry.id
		GROUP BY member.acl_id, entry.authority_id) 
		AS privilegeCount on (privilegeCount.acl_id = grouped_ace.acl_id AND privilegeCount.authority_id = grouped_ace.authority_id)
	);
	
 	
 CREATE TABLE tmp_permission_node
 (
   node_uuid character varying(36) NOT NULL
 )
 WITH (
   OIDS=FALSE
 );

INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_qname qname on qname.id = node.type_qname_id
	JOIN alf_namespace ns on ns.id = qname.ns_id
	WHERE qname.local_name = 'imapFolder' AND ns.uri = 'http://alfresco.webmedia.ee/model/imap/1.0');
	
INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	WHERE child_assoc.qname_localname = 'dvkReceivedCorruptDocuments');
	
INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	WHERE child_assoc.qname_localname = 'scannedDocs');	
	
INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	WHERE child_assoc.qname_localname = 'dvkReceived');	
	
INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	WHERE child_assoc.qname_localname = 'webServiceReceived');		

INSERT INTO delta_node_inheritspermissions (node_uuid, inherits, acl_id) 
	(SELECT node.uuid, FALSE, node.acl_id
	FROM alf_node node
	JOIN tmp_permission_node tmp on tmp.node_uuid = node.uuid
	WHERE node.acl_id IS NOT NULL);
	
INSERT INTO delta_node_permission (node_uuid, authority,  
	view_document_meta_data, edit_document, view_document_files)
	(SELECT node_permission.node_uuid, 'GROUP_DOCUMENT_MANAGERS', TRUE, TRUE, TRUE
	FROM tmp_permission_node tmp
	JOIN delta_node_inheritspermissions node_permission on tmp.node_uuid = node_permission.node_uuid);
	
DELETE FROM tmp_permission_node;

INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	WHERE child_assoc.qname_localname = 'drafts'
	AND node.type_qname_id = 
		(SELECT qname.id from alf_qname qname JOIN alf_namespace ns on ns.id = qname.ns_id
		where (qname.local_name = 'drafts' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')));
		
INSERT INTO delta_node_inheritspermissions (node_uuid, inherits, acl_id) 
	(SELECT node.uuid, FALSE, node.acl_id
	FROM alf_node node
	JOIN tmp_permission_node tmp on tmp.node_uuid = node.uuid
	WHERE node.acl_id IS NOT NULL);		
	
INSERT INTO delta_node_permission (node_uuid, authority,  
	view_document_meta_data, edit_document, view_document_files)
	(SELECT node_permission.node_uuid, 'GROUP_EVERYONE', TRUE, TRUE, TRUE
	FROM tmp_permission_node tmp
	JOIN delta_node_inheritspermissions node_permission on tmp.node_uuid = node_permission.node_uuid);	

DROP TABLE tmp_permission_node;	

alter table alf_node drop constraint if exists fk_alf_node_acl;
alter table alf_attributes drop constraint if exists fk_alf_attr_acl;
alter table avm_nodes drop constraint if exists fk_avm_n_acl;
alter table avm_stores drop constraint if exists fk_avm_s_acl;

alter table alf_acl_member rename to orig_alf_acl_member;
alter table alf_access_control_entry rename to orig_alf_access_control_entry;
alter table alf_access_control_list rename to orig_alf_access_control_list;
alter table alf_ace_context rename to orig_alf_ace_context;
alter table alf_acl_change_set rename to orig_alf_acl_change_set;
alter table alf_permission rename to orig_alf_permission;

create table tmp_nodes_deleted_on_merge (
	id bigint not null
);

-- märkida kustutatuks mallid
with templates_root as (
		select node_id from alf_node child
		left join alf_node_properties props on props.node_id=child.id
		left join alf_child_assoc assoc on assoc.child_node_id=child.id
		left join alf_node parent on assoc.parent_node_id=parent.id
		left join alf_store parent_store on parent.store_id=parent_store.id
		left join alf_qname parent_type_qname on parent_type_qname.id=parent.type_qname_id
		left join alf_namespace parent_namespace on parent_namespace.id=parent_type_qname.ns_id
		left join alf_qname child_prop_type_qname on child_prop_type_qname.id=props.qname_id
		left join alf_namespace child_prop_namespace on child_prop_namespace.id=child_prop_type_qname.ns_id
		where props.string_value='templates'
			and child_prop_type_qname.local_name='name'
			and child_prop_namespace.uri='http://www.alfresco.org/model/content/1.0'
			and parent_store.protocol='workspace'
			and parent_store.identifier='SpacesStore'
			and parent_type_qname.local_name='store_root'
			and parent_namespace.uri='http://www.alfresco.org/model/system/1.0'
),
marked_deleted as (
	update alf_node node set node_deleted = true
	from alf_child_assoc child_assoc, alf_node parent
	where child_assoc.child_node_id = node.id
	and parent.id = child_assoc.parent_node_id
	and 
	node.type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'content' and uri = 'http://www.alfresco.org/model/content/1.0'))
	and parent.id in (select * from templates_root)
	and node. node_deleted = false
	returning node.id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);
	
-- märkida kustutatuks funktsioonid/sarjad/toimikud/teemad
with marked_deleted as (
	update alf_node node set node_deleted = true
	where type_qname_id in 
		(select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'function' and uri = 'http://alfresco.webmedia.ee/model/functions/1.0')
			or (local_name = 'series' and uri = 'http://alfresco.webmedia.ee/model/series/1.0')
			or (local_name = 'volume' and uri = 'http://alfresco.webmedia.ee/model/volume/1.0')
			or (local_name = 'case' and uri = 'http://alfresco.webmedia.ee/model/case/1.0'))
		and node_deleted = false
	returning id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);

-- märkida kustutatuks klassifikaatorid
with marked_deleted as (
	update alf_node node set node_deleted = true
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name in ('classificator', 'classificatorValue') and uri = 'http://alfresco.webmedia.ee/model/classificator/1.0'))
	and node_deleted = false
	returning id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);
	
-- märkida kustutatuks parameetrid 	
with marked_deleted as (
	update alf_node set node_deleted = true
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name in ('intParameter', 'stringParameter', 'doubleParameter') and uri = 'http://alfresco.webmedia.ee/model/parameters/1.0'))
	and node_deleted = false
	returning id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);
	
	
-- märkida kustutatuks eelseadistatud TTVd ja nende töövood
with marked_deleted as (
	update alf_node node set node_deleted = true
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'compoundWorkflowDefinition' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0'))
	and node_deleted = false
	returning id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);
	
with compound_workflow_definitions as (
	select id, uuid from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'compoundWorkflowDefinition' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0'))
	and node_deleted = false
),
marked_deleted as (
	update alf_node node set node_deleted = true
	from alf_child_assoc child_assoc 
	where child_assoc.child_node_id = node.id
	and child_assoc.parent_node_id in (select id from compound_workflow_definitions)
	and node_deleted = false
	returning node.id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);	

-- märkida kustutatuks kontekstitundlik abiinfo
with marked_deleted as (
	update alf_node node set node_deleted = true
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'helpText' and uri = 'http://alfresco.webmedia.ee/model/helpText/1.0'))
	and node_deleted = false
	returning id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);	
	
-- märkida kustutatuks dok. liigid, andmeväljad ja seonduv info
with marked_deleted as (
	update alf_node node set node_deleted = true
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name in ('fieldDefinitions', 'fieldGroupDefinitions', 'documentType', 'caseFileType', 'documentTypeVersion', 'field', 
				'fieldDefinition', 'fieldGroup', 'separationLine', 'associationModel', 'followupAssociation', 'replyAssociation', 'fieldMapping') and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'))
	and node_deleted = false
	returning id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);	

-- märkida kustutatuks kasutajagrupid, v.a. süsteemsed
with marked_deleted as (
	update alf_node node set node_deleted = true
	from alf_node_properties props 
	where 
	props.node_id = node.id and props.qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
		where local_name = 'authorityName' and uri = 'http://www.alfresco.org/model/content/1.0')
	and type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'authorityContainer' and uri = 'http://www.alfresco.org/model/content/1.0'))
	and props.string_value not in ('GROUP_ALFRESCO_ADMINISTRATORS', 'GROUP_SUPERVISION', 'GROUP_ACCOUNTANTS', 'GROUP_DOCUMENT_MANAGERS', 'GROUP_ARCHIVISTS')
	and node_deleted = false
	returning node.id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);	

-- märkida kustutatuks kasutajad
with marked_deleted as (
	update alf_node node set node_deleted = true
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
		where local_name = 'person' and uri = 'http://www.alfresco.org/model/content/1.0')
	and node_deleted = false
	returning node.id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);	

with marked_deleted as (
	update alf_node node set node_deleted = true
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
			where (local_name in ('organization', 'orgPerson', 'privPerson', 'contactGroup', 'personBase') and uri = 'http://alfresco.webmedia.ee/model/addressbook/1.0'))
	and node_deleted = false
	returning node.id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);

with marked_deleted as (
	update alf_node node set node_deleted = true
	where type_qname_id in 
		(select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
			where (local_name = 'configurations' and uri = 'http://www.alfresco.org/model/application/1.0'))
	and node_deleted = false
	returning node.id
)
insert into tmp_nodes_deleted_on_merge (select * from marked_deleted);
	
-- importida andmed.

-- alf_namespace
CREATE TABLE tmp_alf_namespace(
  imported_id bigint NOT NULL,
  version bigint NOT NULL,
  uri character varying(100) NOT NULL,
  local_id bigint
);

copy tmp_alf_namespace (imported_id, version, uri) from '/delta-pgsql/data/alf_namespace.tsv';

update tmp_alf_namespace tmp_ns set local_id = ns.id
from alf_namespace ns
where tmp_ns.uri = ns.uri;

insert into alf_namespace  
(select (select max(id) from alf_namespace) + row_number() over (),tmp_ns.version, tmp_ns.uri from tmp_alf_namespace tmp_ns
where not exists (select 1 from alf_namespace where uri = tmp_ns.uri));

update tmp_alf_namespace tmp_ns set local_id = ns.id 
from alf_namespace ns
where tmp_ns.uri = ns.uri
and tmp_ns.local_id is null;

-- alf_qname.
CREATE TABLE tmp_alf_qname (
  imported_id bigint NOT NULL,
  version bigint NOT NULL,
  ns_id bigint NOT NULL,
  local_name character varying(200) NOT NULL,
  local_id bigint
);

copy tmp_alf_qname (imported_id, version, ns_id, local_name) from '/delta-pgsql/data/alf_qname.tsv';

update tmp_alf_qname tmp_qname set local_id = qname.id
from alf_qname qname, tmp_alf_namespace tmp_ns
where tmp_qname.local_name = qname.local_name and tmp_qname.ns_id = tmp_ns.imported_id and qname.ns_id = tmp_ns.local_id;

insert into alf_qname  
(select (select max(id) from alf_qname) + row_number() over (),tmp_qname.version, tmp_ns.local_id, tmp_qname.local_name from tmp_alf_qname tmp_qname
left join alf_qname qname on tmp_qname.local_name = qname.local_name
left join tmp_alf_namespace tmp_ns on tmp_qname.ns_id = tmp_ns.imported_id
left join alf_namespace ns on ns.id = tmp_ns.local_id
where qname.id is null or ns.id is null);

update tmp_alf_qname tmp_qname set local_id = qname.id 
from alf_qname qname, tmp_alf_namespace tmp_ns, alf_namespace ns
where tmp_qname.local_name = qname.local_name and tmp_qname.ns_id = tmp_ns.imported_id and ns.id = tmp_ns.local_id
and tmp_qname.local_id is null;

create table tmp_locale (
  id bigint NOT NULL,
  version bigint NOT NULL,
  locale_str character varying(20) NOT NULL
);

copy tmp_locale from '/delta-pgsql/data/alf_locale.tsv';

COPY alf_transaction from '/delta-pgsql/data/alf_transaction.tsv';
	
-- alf_node import esialgu ajutisse tabelisse, et saaks teha lisakontrolle vastu alf_node tabelit
create table tmp_import_alf_node (
  id bigint,
  version bigint,
  store_id bigint,
  uuid character varying,
  transaction_id bigint,
  node_deleted boolean,
  type_qname_id integer,
  acl_id bigint,
  audit_creator text,
  audit_created text,
  audit_modifier text,
  audit_modified text,
  audit_accessed text
);

create table tmp_import_alf_node_errors (
	id bigint
);

COPY tmp_import_alf_node from '/delta-pgsql/data/alf_node.tsv';

create table tmp_import_overwritten_by_import (
	id bigint,
	uuid character varying
);
-- kustutada kõik node'id, mis impordiga üle kirjutatakse (enne märgiti ainult node_deleted = true)
insert into tmp_import_overwritten_by_import (select id, uuid from alf_node where id in (select id from  tmp_import_alf_node) and node_deleted = true);

delete from alf_node_properties where node_id in (select id from tmp_import_overwritten_by_import);
delete from alf_node_aspects where node_id in (select id from tmp_import_overwritten_by_import);
delete from delta_node_permission where node_uuid in (select uuid from tmp_import_overwritten_by_import);
delete from delta_node_inheritspermissions where node_uuid in (select uuid from tmp_import_overwritten_by_import);

alter table alf_child_assoc drop constraint fk_alf_cass_cnode;
alter table alf_child_assoc drop constraint fk_alf_cass_pnode;

alter table alf_node_assoc drop constraint fk_alf_nass_snode;
alter table alf_node_assoc drop constraint fk_alf_nass_tnode;

delete from alf_node where id in (select id from  tmp_import_overwritten_by_import);

-- Kui pärast eelmiste päringute jooksutamist alf_node sisaldab sama id-d, kui imporditavad node'id, katkesta skriptide töö - midagi on päris valesti läinud.
-- Kontrollida, kas ekspordil kasutati õiget maksimaalset id-d uute id-de määramiseks.
copy (select 'Remaining nodes with same id (erroneous):') to stdout;
copy (select count(*) from alf_node join tmp_import_alf_node on tmp_import_alf_node.id = alf_node.id) to stdout;
insert into tmp_import_alf_node_errors (select alf_node.id from alf_node join tmp_import_alf_node on tmp_import_alf_node.id = alf_node.id);

-- imporditavad node'id, mille andmekoosseis ei vasta alf_node tabeli piirangutele, on viga, lähevad tmp_import_alf_node_errors tabelisse
with import_errors as (
	delete from tmp_import_alf_node 
	where id is null or version is null or store_id is null or uuid is null 
	or transaction_id is null or node_deleted is null or type_qname_id is null
	returning id
)
insert into tmp_import_alf_node_errors (select * from import_errors);

delete from alf_node_properties using tmp_import_alf_node import, alf_node where node_id = alf_node.id and import.uuid = alf_node.uuid;
delete from alf_node_aspects using tmp_import_alf_node import, alf_node where node_id = alf_node.id and import.uuid = alf_node.uuid;

delete from alf_node_aspects where node_id in (select id from alf_node where exists (select uuid from tmp_import_alf_node import where import.uuid = alf_node.uuid));

-- delete assocs where both nodes are imported
--delete from alf_node_assoc using tmp_import_alf_node target_import, tmp_import_alf_node source_import, alf_node source, alf_node target 
--	where target_node_id = target.id and target.uuid = target_import.uuid and source_node_id = source.id and source.uuid = source_import.uuid;

--delete from alf_child_assoc using tmp_import_alf_node child_import, alf_node child, tmp_import_alf_node parent_import, alf_node parent 
--	where child_node_id = child.id and child.uuid = child_import.uuid and parent_node_id = parent.id and parent.uuid = parent_import.uuid;

delete from delta_node_permission using tmp_import_alf_node, alf_node
where node_uuid = tmp_import_alf_node.uuid and alf_node.uuid = tmp_import_alf_node.uuid;

delete from delta_node_permission using tmp_import_alf_node, alf_node
where node_uuid = tmp_import_alf_node.uuid and alf_node.id = tmp_import_alf_node.id;

delete from delta_node_inheritspermissions using tmp_import_alf_node, alf_node
where node_uuid = tmp_import_alf_node.uuid and alf_node.uuid = tmp_import_alf_node.uuid;

delete from delta_node_inheritspermissions using tmp_import_alf_node, alf_node
where node_uuid = tmp_import_alf_node.uuid and alf_node.id = tmp_import_alf_node.id;

update alf_node set id = import.id, version = import.version, store_id = import.store_id, uuid = import.uuid, 
	transaction_id = import.transaction_id, node_deleted = import.node_deleted,
	type_qname_id = (select local_id from tmp_alf_qname where imported_id = import.type_qname_id),
	acl_id = import.acl_id, audit_creator = import.audit_creator, audit_created = import.audit_created, audit_modifier = import.audit_modifier, 
	audit_modified = import.audit_modified 
	from tmp_import_alf_node import where import.uuid = alf_node.uuid and import.uuid is not null;

-- SIIT!

insert into alf_node (select import.id, import.version, import.store_id, import.uuid, import.transaction_id, import.node_deleted,
	tmp_alf_qname.local_id,
	import.acl_id, import.audit_creator, import.audit_created, import.audit_modifier, import.audit_modified from tmp_import_alf_node import
	left join alf_node node on node.uuid = import.uuid
	left join tmp_alf_qname on import.type_qname_id = tmp_alf_qname.imported_id
	where node.id is null);

-- remove assocs that refer to not existing nodes
with not_existing_parent as (
	select parent_node_id from alf_child_assoc assoc
	left join alf_node node on node.id = assoc.parent_node_id and node_deleted = false
	where node.id is null
),
not_existing_child as (
	select child_node_id from alf_child_assoc assoc
	left join alf_node node on node.id = assoc.child_node_id and node_deleted = false
	where node.id is null
)
delete from alf_child_assoc where child_node_id in (select * from not_existing_child) or parent_node_id in (select * from not_existing_parent);

with not_existing_source as (
	select source_node_id from alf_node_assoc assoc
	left join alf_node node on node.id = assoc.source_node_id and node_deleted = false
	where node.id is null
),
not_existing_target as (
	select target_node_id from alf_node_assoc assoc
	left join alf_node node on node.id = assoc.target_node_id and node_deleted = false
	where node.id is null
)
delete from alf_node_assoc where target_node_id in (select * from not_existing_target) or source_node_id in (select * from not_existing_source);

alter table alf_child_assoc add CONSTRAINT fk_alf_cass_cnode FOREIGN KEY (child_node_id)
      REFERENCES alf_node (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

alter table alf_child_assoc add CONSTRAINT fk_alf_cass_pnode FOREIGN KEY (parent_node_id)
      REFERENCES alf_node (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;
      
alter table alf_node_assoc add CONSTRAINT fk_alf_nass_snode FOREIGN KEY (source_node_id)
      REFERENCES alf_node (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;
      
alter table alf_node_assoc add CONSTRAINT fk_alf_nass_tnode FOREIGN KEY (target_node_id)
      REFERENCES alf_node (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

-- alf_child_assoc import
CREATE TABLE tmp_import_alf_child_assoc (
  id bigint,
  version bigint,
  parent_node_id bigint,
  type_qname_id bigint,
  child_node_name_crc bigint,
  child_node_name character varying(50),
  child_node_id bigint,
  qname_ns_id bigint,
  qname_localname character varying(255),
  is_primary boolean,
  assoc_index integer
);

copy tmp_import_alf_child_assoc from '/delta-pgsql/data/alf_child_assoc.tsv';

delete from tmp_import_alf_child_assoc tmp_assoc
where exists (select 1 from alf_child_assoc assoc where tmp_assoc.parent_node_id = assoc.parent_node_id 
	and tmp_assoc.type_qname_id = assoc.type_qname_id and tmp_assoc.child_node_name_crc = assoc.child_node_name_crc and tmp_assoc.child_node_name = assoc.child_node_name);

delete from tmp_import_alf_child_assoc tmp_assoc
where exists (select 1 from alf_child_assoc assoc where tmp_assoc.parent_node_id = assoc.parent_node_id 
	and tmp_assoc.type_qname_id = assoc.type_qname_id and tmp_assoc.child_node_id = assoc.child_node_id and tmp_assoc.child_node_name = assoc.child_node_name);

with not_existing_parent as (
	select parent_node_id from tmp_import_alf_child_assoc assoc
	left join alf_node node on node.id = assoc.parent_node_id and node_deleted = false
	where node.id is null
),
not_existing_child as (
	select child_node_id from tmp_import_alf_child_assoc assoc
	left join alf_node node on node.id = assoc.child_node_id and node_deleted = false
	where node.id is null
)
delete from tmp_import_alf_child_assoc where child_node_id in (select * from not_existing_child) or parent_node_id in (select * from not_existing_parent);

insert into alf_child_assoc (
	select tmp_import_alf_child_assoc.id, tmp_import_alf_child_assoc.version, tmp_import_alf_child_assoc.parent_node_id, 
	tmp_alf_qname.local_id,
	tmp_import_alf_child_assoc.child_node_name_crc, tmp_import_alf_child_assoc.child_node_name, tmp_import_alf_child_assoc.child_node_id, tmp_import_alf_child_assoc.qname_ns_id, 
	tmp_import_alf_child_assoc.qname_localname, tmp_import_alf_child_assoc.is_primary, tmp_import_alf_child_assoc.assoc_index
	from tmp_import_alf_child_assoc 
	left join tmp_alf_qname on tmp_alf_qname.imported_id = tmp_import_alf_child_assoc.type_qname_id
	left join tmp_import_alf_node parent_import on parent_import.id = tmp_import_alf_child_assoc.parent_node_id
	left join tmp_import_alf_node child_import on child_import.id = tmp_import_alf_child_assoc.child_node_id
	where parent_import.id is not null or child_import.id is not null
);

-- alf_node_assoc import
create table tmp_import_alf_node_assoc (
  id bigint,
  version bigint,
  source_node_id bigint,
  target_node_id bigint,
  type_qname_id bigint
);

copy tmp_import_alf_node_assoc from '/delta-pgsql/data/alf_node_assoc.tsv';

with not_existing_source as (
	select source_node_id from tmp_import_alf_node_assoc assoc
	left join alf_node node on node.id = assoc.source_node_id and node_deleted = false
	where node.id is null
),
not_existing_target as (
	select target_node_id from tmp_import_alf_node_assoc assoc
	left join alf_node node on node.id = assoc.target_node_id and node_deleted = false
	where node.id is null
)
delete from tmp_import_alf_node_assoc where target_node_id in (select * from not_existing_target) or source_node_id in (select * from not_existing_source);

insert into alf_node_assoc (
	select tmp_import_alf_node_assoc.id, tmp_import_alf_node_assoc.version, source_node_id, target_node_id,
	tmp_alf_qname.local_id
	from tmp_import_alf_node_assoc
	left join tmp_alf_qname on tmp_alf_qname.imported_id = tmp_import_alf_node_assoc.type_qname_id
	left join tmp_import_alf_node source_import on source_import.id = tmp_import_alf_node_assoc.source_node_id
	left join tmp_import_alf_node target_import on target_import.id = tmp_import_alf_node_assoc.target_node_id
	where source_import.id is not null or target_import.id is not null
);

-- alf_node_properties import
CREATE TABLE tmp_import_alf_node_properties (
  node_id bigint,
  actual_type_n integer,
  persisted_type_n integer,
  boolean_value boolean,
  long_value bigint,
  float_value real,
  double_value double precision,
  string_value character varying(1024),
  serializable_value bytea,
  qname_id bigint,
  list_index integer,
  locale_id bigint
);

copy tmp_import_alf_node_properties from '/delta-pgsql/data/alf_node_properties.tsv';

insert into alf_node_properties (
	select tmp_import_alf_node_properties.node_id, actual_type_n, persisted_type_n, boolean_value, long_value, float_value,
	double_value, string_value, serializable_value, 
	tmp_alf_qname.local_id,
	list_index,locale_id from tmp_import_alf_node_properties 
	left join tmp_alf_qname on tmp_alf_qname.imported_id = tmp_import_alf_node_properties.qname_id
	left join tmp_import_alf_node on tmp_import_alf_node_properties.node_id = tmp_import_alf_node.id
	left join alf_node on tmp_import_alf_node_properties.node_id = alf_node.id
	where tmp_import_alf_node.id is not null and alf_node.id is not null
);

-- alf_node_aspects import
create table tmp_import_alf_node_aspects (
  node_id bigint,
  qname_id bigint
);

copy tmp_import_alf_node_aspects from '/delta-pgsql/data/alf_node_aspects.tsv';

insert into alf_node_aspects (
	select node_id, tmp_alf_qname.local_id from tmp_import_alf_node_aspects
	left join tmp_alf_qname on tmp_alf_qname.imported_id = tmp_import_alf_node_aspects.qname_id
	left join tmp_import_alf_node on tmp_import_alf_node.id = tmp_import_alf_node_aspects.node_id
	where tmp_import_alf_node.id is not null
);

-- ülejäänud tabelid ilma lisakontrollideta importida otse
-- delta_task ja seotud delta_task_xxx tabelid täies mahus.
CREATE TABLE tmp_delta_task
(
  task_id text NOT NULL,
  workflow_id text NOT NULL,
  task_type text NOT NULL,
  index_in_workflow integer NOT NULL,
  wfc_status text,
  wfc_creator_name text,
  wfc_started_date_time timestamp without time zone,
  wfc_stopped_date_time timestamp without time zone,
  wfc_owner_id text,
  wfc_owner_name text,
  wfc_previous_owner_id text,
  wfc_owner_email text,
  wfc_owner_group text,
  wfc_outcome text,
  wfc_document_type text,
  wfc_completed_date_time timestamp without time zone,
  wfc_owner_job_title text,
  wfc_parallel_tasks boolean,
  wfs_creator_id text,
  wfs_creator_email text,
  wfs_workflow_resolution text,
  wfs_completed_overdue boolean,
  wfs_due_date timestamp without time zone,
  wfs_due_date_days integer,
  wfs_is_due_date_days_working_days boolean,
  wfs_comment text,
  wfs_file_versions text,
  wfs_institution_name text,
  wfs_institution_code text,
  wfs_creator_institution_code text,
  wfs_original_dvk_id text,
  wfs_sent_dvk_id text,
  wfs_recieved_dvk_id text,
  wfs_send_status text,
  wfs_send_date_time timestamp without time zone,
  wfs_resolution text,
  wfs_temp_outcome text,
  wfs_active boolean,
  wfs_send_order_assignment_completed_email boolean,
  wfs_proposed_due_date timestamp without time zone,
  wfs_confirmed_due_date timestamp without time zone,
  has_due_date_history boolean NOT NULL DEFAULT false,
  is_searchable boolean NOT NULL,
  wfc_owner_organization_name text[],
  store_id text NOT NULL
);
COPY tmp_delta_task from '/delta-pgsql/data/delta_task.tsv';

delete from delta_task where task_id in (select task_id from tmp_delta_task);
delete from delta_task where workflow_id in (select workflow_id from tmp_delta_task);
insert into delta_task (select * from tmp_delta_task);

-- id-d ei ekspordi, seda kusagil ei refereerita
COPY delta_task_due_date_extension_assoc (task_id, extension_task_id) from '/delta-pgsql/data/delta_task_due_date_extension_assoc.tsv';

COPY delta_task_due_date_history (task_id, previous_date, change_reason) FROM '/delta-pgsql/data/delta_task_due_date_history.tsv';

COPY delta_task_file (task_id, file_id) FROM '/delta-pgsql/data/delta_task_file.tsv';

COPY delta_log from '/delta-pgsql/data/delta_log.tsv';

delete from delta_register;
COPY delta_register from '/delta-pgsql/data/delta_register.tsv';

delete from delta_node_permission where node_uuid in (select uuid from tmp_import_alf_node);
COPY tmp_delta_node_permission from '/delta-pgsql/data/delta_node_permission.tsv';

delete from tmp_delta_node_permission 
using (select tmp_delta_node_permission.node_uuid as uuid, tmp_import_alf_node.uuid as import_uuid from tmp_delta_node_permission
 left join tmp_import_alf_node on tmp_import_alf_node.uuid = tmp_delta_node_permission.node_uuid) as tmp_node
where tmp_delta_node_permission.node_uuid = tmp_node.uuid and tmp_node.import_uuid is null;

insert into delta_node_permission (select * from tmp_delta_node_permission);

delete from delta_node_inheritspermissions where node_uuid in (select uuid from tmp_import_alf_node);
COPY tmp_delta_node_inheritspermissions from '/delta-pgsql/data/delta_node_permission_list.tsv';
delete from tmp_delta_node_permission 
	using (select tmp_delta_node_permission.node_uuid as uuid, tmp_import_alf_node.uuid as import_uuid from tmp_delta_node_permission
 	left join tmp_import_alf_node on tmp_import_alf_node.uuid = tmp_delta_node_permission.node_uuid) as tmp_node
	where tmp_delta_node_permission.node_uuid = tmp_node.uuid and tmp_node.import_uuid is null;
insert into delta_node_inheritspermissions (select * from tmp_delta_node_inheritspermissions);

-- content
COPY alf_content_url from '/delta-pgsql/data/alf_content_url.tsv';

create table tmp_content_data (
  id bigint NOT NULL,
  version bigint NOT NULL,
  content_url_id bigint,
  content_mimetype_id bigint,
  content_encoding_id bigint,
  content_locale_id bigint
);

COPY tmp_content_data from '/delta-pgsql/data/alf_content_data.tsv';

CREATE TABLE tmp_mimetype (
  id bigint NOT NULL,
  version bigint NOT NULL,
  mimetype_str character varying(100) NOT NULL
);

copy tmp_mimetype from '/delta-pgsql/data/alf_mimetype.tsv';

insert into alf_mimetype (select ((select max(id) as max_id from alf_mimetype) + (row_number() over())), tmp_mimetype.version, tmp_mimetype.mimetype_str from tmp_mimetype
	left join alf_mimetype on alf_mimetype.mimetype_str = tmp_mimetype.mimetype_str
	where alf_mimetype.id is null);

insert into alf_content_data (
	select tmp_content_data.id, tmp_content_data.version, tmp_content_data.content_url_id, tmp_content_data.content_mimetype_id, tmp_content_data.content_encoding_id, 
	alf_locale.id
	from tmp_content_data
	join tmp_locale on tmp_locale.id = tmp_content_data.content_locale_id
	join alf_locale on alf_locale.locale_str = tmp_locale.locale_str
	join tmp_mimetype on tmp_mimetype.id = tmp_content_data.content_mimetype_id
	join alf_mimetype on alf_mimetype.mimetype_str = tmp_mimetype.mimetype_str
);

-- kustutada kõiki alf_node_properties, alf_node_aspects, alf_child_assoc ja alf_node_assoc read, kus viidatakse kustutatud node'idele, mis on toodud tabelis tmp_nodes_deleted_on_merge
-- NB! tabel sisaldab ka node, mis impordi käigus uuesti sama id-ga loodi, tuleb kontollida, et neid ei kustutataks!
delete from alf_node_properties where node_id in (select id from alf_node where id in (select id from tmp_nodes_deleted_on_merge) and node_deleted = true);
delete from alf_node_aspects where node_id in (select id from alf_node where id in (select id from tmp_nodes_deleted_on_merge) and node_deleted = true);
delete from alf_child_assoc where parent_node_id in (select id from alf_node where id in (select id from tmp_nodes_deleted_on_merge) and node_deleted = true)
	or child_node_id in (select id from alf_node where id in (select id from tmp_nodes_deleted_on_merge) and node_deleted = true);
delete from alf_node_assoc where source_node_id in (select id from alf_node where id in (select id from tmp_nodes_deleted_on_merge) and node_deleted = true)
	or target_node_id in (select id from alf_node where id in (select id from tmp_nodes_deleted_on_merge) and node_deleted = true);
	
-- resettida sequence'id
select setval('delta_task_due_date_extension_task_due_date_extension_assoc_seq', (select max(task_due_date_extension_assoc_id) from delta_task_due_date_extension_assoc));
select setval('delta_task_file_task_file_id_seq', (select max(task_file_id) from delta_task_file));
select setval('delta_task_due_date_history_task_due_date_history_id_seq', (select max(task_due_date_history_id) from delta_task_due_date_history));
select setval('alf_content_data_seq', (select max(id) from alf_content_data));
select setval('alf_content_url_seq', (select max(id) from alf_content_url));
SELECT setval('hibernate_sequence', greatest((select max(id) + 1 from public.alf_node), (select max(id) + 1 from public.alf_transaction), (select max(id) + 1 from public.alf_child_assoc), (select max(id) + 1 from public.alf_node_assoc), (select max(id) + 1 from public.alf_content_data), (select max(id) + 1 from public.alf_content_url)));
