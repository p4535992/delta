-- kustutada eelmise eksportimise andmed
drop table if exists tmp_constants;
drop table if exists tmp_export_nodes;
drop table if exists tmp_existing_not_to_export_nodes;
drop table if exists tmp_existing_users;
drop table if exists tmp_existing_authorities;
drop table if exists tmp_content_data;
drop table if exists tmp_content_url;
DROP SEQUENCE if exists export_sequence;

-- kõigi ülekantavate node'ide alf_node.id väärtused

CREATE TABLE tmp_constants (
	constant_name text not null unique,
	constant_value bigint not null
);

COPY tmp_constants FROM '/delta-pgsql/data/constants.csv';

CREATE SEQUENCE export_sequence
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
ALTER TABLE hibernate_sequence
  OWNER TO alfresco;
  
select setval ('export_sequence', (select max(constant_value) from tmp_constants where constant_name = 'ALF_HIBERNATE_SEQUENCE_NEXTVAL'));

CREATE TABLE tmp_export_nodes
(
  id bigint NOT NULL unique,
  uuid character varying(36) not null unique,
  new_id bigint not null,
  transaction_id bigint not null
);

CREATE TABLE tmp_existing_not_to_export_nodes
(
  id bigint NOT NULL unique
);

COPY tmp_existing_not_to_export_nodes FROM '/delta-pgsql/data/existingNotToOverwriteNodes.tsv';

CREATE TABLE tmp_existing_users
(
  id bigint NOT NULL unique,
  username text not null
);

COPY tmp_existing_users FROM '/delta-pgsql/data/existingUsers.tsv';

create table tmp_existing_authorities (
	id bigint,
	authority text
);

-- Dokumendid. NB! Oluline, et see jookseks tühjal tabelil, sest selle järgi võetakse dokumentidega seotud objektid.
-- Drafts all olevaid dokumente üle ei kanta.

insert into tmp_export_nodes (
	with drafts_root as (
		select id from alf_node where type_qname_id in
			(select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id where local_name = 'drafts' and uri ='http://alfresco.webmedia.ee/model/document/common/1.0')
	)
	select node.id, node.uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	join alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	join alf_node parent on parent.id = child_assoc.parent_node_id
		where parent.id not in (select * from drafts_root)
		and node.type_qname_id in 
			(select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'document' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0'))
		and node.node_deleted = false
);

-- rekursiivselt kõik dokumentide alamnode'id, v.a. tööülesanded ja logi node'id
WITH RECURSIVE node_hierarchy (id, uuid, new_id, transaction_id) AS ( 
                SELECT child.id, child.uuid, nextval('export_sequence'), nextval('export_sequence')  
                FROM alf_node node  
                JOIN alf_child_assoc child_assoc on node.id = child_assoc.parent_node_id  
                join alf_node child on child.id = child_assoc.child_node_id
                where 
		node.id in (select id from tmp_export_nodes)
                and child.type_qname_id not in (
					select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id where (
						(local_name = 'documentLog' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
						or (local_name = 'assignmentTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'orderAssignmentTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'informationTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'opinionTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'reviewTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'externalReviewTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'signatureTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'confirmationTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'dueDateExtensionTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
					)                
                )
                and child.node_deleted = false
            UNION ALL  
                select child_child.id, child_child.uuid, nextval('export_sequence'), nextval('export_sequence')    
                FROM node_hierarchy child  
                JOIN alf_child_assoc child_child_assoc ON child_child_assoc.parent_node_id = child.id  
                JOIN alf_node child_child ON child_child.id = child_child_assoc.child_node_id 
                where child_child.type_qname_id not in (
					select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id where (
						(local_name = 'documentLog' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
						or (local_name = 'assignmentTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'orderAssignmentTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'informationTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'opinionTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'reviewTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'externalReviewTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'signatureTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'confirmationTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
						or (local_name = 'dueDateExtensionTask' and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0')
					)                
                )
                and child_child.node_deleted = false
            )

insert into tmp_export_nodes (select * from node_hierarchy);

-- versionHistory tüüpi node'id (failide versioonide ülemnode'id)

insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'versionHistory' and uri = 'http://www.alfresco.org/model/versionstore/2.0'))
	and node_deleted = false
);

-- content tüüpi node'id, mis asuvad terviktöövoo (vastab rakenduses TÜ juures kuvatavale failile), versionHistory või mallide all (dokumendi failid valiti juba dokumendi alamnode'ide hulgas)
-- või prügikastis
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
)
insert into tmp_export_nodes (id, uuid, new_id, transaction_id) (
	select node.id, node.uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	join alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	join alf_node parent on parent.id = child_assoc.parent_node_id
	where 
	node.type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'content' and uri = 'http://www.alfresco.org/model/content/1.0'))
	and (
		parent.id in (select * from templates_root)
		or parent.type_qname_id in 
			(select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id  
			where (local_name = 'versionHistory' and uri = 'http://www.alfresco.org/model/versionstore/2.0')
				or (local_name in ('assignmentWorkflow', 'docRegistrationWorkflow', 'informationWorkflow', 'opinionWorkflow', 'reviewWorkflow', 
					'externalReviewWorkflow', 'signatureWorkflow', 'orderAssignmentWorkflow', 'confirmationWorkflow', 'dueDateExtensionWorkflow') 
					and uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0'))
		or child_assoc.qname_localname = 'archivedItem' and qname_ns_id = (select id from alf_namespace where uri = 'http://www.alfresco.org/model/system/1.0')
	)	
	and node.node_deleted = false
	and not exists (select 1 from tmp_export_nodes where node.id = tmp_export_nodes.id)
);

-- Kõik funktsioonid/sarjad/toimikud/teemad
insert into tmp_export_nodes (id, uuid, new_id, transaction_id) (
	select id, uuid, id, nextval('export_sequence') from alf_node where type_qname_id in 
		(select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'function' and uri = 'http://alfresco.webmedia.ee/model/functions/1.0')
			or (local_name = 'series' and uri = 'http://alfresco.webmedia.ee/model/series/1.0')
			or (local_name = 'volume' and uri = 'http://alfresco.webmedia.ee/model/volume/1.0')
			or (local_name = 'case' and uri = 'http://alfresco.webmedia.ee/model/case/1.0'))
		and node_deleted = false
);

-- toimiku kustutatud dokumendid
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node where type_qname_id in 
		(select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
		where (local_name = 'deletedDocument' and uri = 'http://alfresco.webmedia.ee/model/volume/1.0'))
		and node_deleted = false
);

-- klassifikaatorid
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name in ('classificator', 'classificatorValue') and uri = 'http://alfresco.webmedia.ee/model/classificator/1.0'))
	and node_deleted = false
);

-- parameetrid
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id  
			where (local_name in ('intParameter', 'stringParameter', 'doubleParameter') and uri = 'http://alfresco.webmedia.ee/model/parameters/1.0'))
	and node_deleted = false
);

-- eelseadistatud TTVd ja nende all olevad töövood (tööülesanded lähevad koos üldise delta_task tabeli ületõstmisega)
-- parameetrid
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'compoundWorkflowDefinition' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0'))
	and node_deleted = false
);

with compound_workflow_definitions as (
	select id from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'compoundWorkflowDefinition' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0'))
	and node_deleted = false
)
insert into tmp_export_nodes (
	select node.id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	join alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
		and child_assoc.parent_node_id in (select id from compound_workflow_definitions)
	and node_deleted = false
);

-- kontekstitundlik abiinfo
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'helpText' and uri = 'http://alfresco.webmedia.ee/model/helpText/1.0'))
	and node_deleted = false
);

-- Dok. liigid, andmeväljad ja seonduv info
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name in ('fieldDefinitions', 'fieldGroupDefinitions', 'documentType', 'caseFileType', 'documentTypeVersion', 'field', 
				'fieldDefinition', 'fieldGroup', 'separationLine', 'associationModel', 'followupAssociation', 'replyAssociation', 'fieldMapping') and uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0'))
	and node_deleted = false
);

-- salvestatud otsingud, sama id-ga üle ei kirjutata
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'filter' and uri = 'http://alfresco.webmedia.ee/model/document/search/1.0')
			or (local_name = 'logFilter' and uri = 'http://alfresco.webmedia.ee/model/log/1.0')
			or (local_name = 'filter' and uri = 'http://alfresco.webmedia.ee/model/task/search/1.0'))
	and node_deleted = false
	and not exists (select id from tmp_existing_not_to_export_nodes where tmp_existing_not_to_export_nodes.id = node.id)
);

-- kasutajagrupid, v.a. süsteemsed
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	left join alf_node_properties props on props.node_id = node.id and props.qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
		where local_name = 'authorityName' and uri = 'http://www.alfresco.org/model/content/1.0')
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'authorityContainer' and uri = 'http://www.alfresco.org/model/content/1.0'))
	and props.string_value not in ('GROUP_ALFRESCO_ADMINISTRATORS', 'GROUP_SUPERVISION', 'GROUP_ACCOUNTANTS', 'GROUP_DOCUMENT_MANAGERS', 'GROUP_ARCHIVISTS')
	and node_deleted = false
);

-- kasutajad
insert into tmp_export_nodes (
	select 
	case when existing_users.id is not null then existing_users.id else node.id end, 
	uuid, case when existing_users.id is not null then existing_users.id else node.id end, nextval('export_sequence') from 
		(select * from alf_node where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'person' and uri = 'http://www.alfresco.org/model/content/1.0'))) node
	left join alf_node_properties props on props.node_id = node.id and qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
		where local_name = 'userName' and uri = 'http://www.alfresco.org/model/content/1.0')
	left join tmp_existing_users existing_users on existing_users.username = props.string_value
	and node_deleted = false
);

-- kasutaja seadistuste ülemnode, sama id-ga üle ei kirjuta
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
			where (local_name = 'configurations' and uri = 'http://www.alfresco.org/model/application/1.0'))
	and node_deleted = false
	and not exists (select id from tmp_existing_not_to_export_nodes where tmp_existing_not_to_export_nodes.id = node.id)
);

-- eelistused (preferences) - teisest baasist kõik kustutada!
insert into tmp_export_nodes (
	select node.id, node.uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	join alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	where child_assoc.parent_node_id in (select id from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
			where (local_name = 'configurations' and uri = 'http://www.alfresco.org/model/application/1.0')))
	and node_deleted = false
);

-- lemmikute kataloogid, sama id-ga üle ei kirjutata
insert into tmp_export_nodes (
	select node.id, node.uuid, nextval('export_sequence'), nextval('export_sequence') from 
	(select * from alf_node 
		where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
			where (local_name = 'favoriteDirectory' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0'))) node
	left join tmp_existing_not_to_export_nodes not_to_export on not_to_export.id = node.id
	and node_deleted = false
	and not_to_export.id is null
);

-- asendajad, sama id-ga üle ei kirjutata
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
			where (local_name in ('substitutes', 'substitute') and uri = 'http://alfresco.webmedia.ee/model/substitute/1.0'))
	and node_deleted = false
	and not exists (select id from tmp_existing_not_to_export_nodes where tmp_existing_not_to_export_nodes.id = node.id)
);

-- kontaktid ja kontaktgrupid
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
			where (local_name in ('organization', 'orgPerson', 'privPerson', 'contactGroup', 'personBase') and uri = 'http://alfresco.webmedia.ee/model/addressbook/1.0'))
	and node_deleted = false
);

-- tähtsad teated, sama id-ga üle ei kirjutata
insert into tmp_export_nodes (
	select id, uuid, nextval('export_sequence'), nextval('export_sequence') from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
			where (local_name = 'generalNotification' and uri = 'http://alfresco.webmedia.ee/model/notification/1.0'))
	and node_deleted = false
	and not exists (select id from tmp_existing_not_to_export_nodes where tmp_existing_not_to_export_nodes.id = node.id)
);


-- alf_node kõik read, kus id väärtus on olemas tabelis tmp_export_nodes

COPY (
	select exports.new_id, 
	version, store_id, node.uuid, exports.transaction_id, 
	node_deleted, type_qname_id, null, audit_creator, audit_created, audit_modifier, audit_modified, audit_accessed 
	FROM alf_node node
	join tmp_export_nodes exports on exports.id = node.id 
) TO '/delta-pgsql/data/alf_node.tsv';

-- alf_namespace kõik read.
 COPY (
	SELECT * FROM alf_namespace
) TO '/delta-pgsql/data/alf_namespace.tsv';

-- alf_qname kõik read. 
 COPY (
	SELECT * FROM alf_qname
) TO '/delta-pgsql/data/alf_qname.tsv';

-- delta_task ja seotud delta_task_xxx tabelid täies mahus.
COPY (
	SELECT * FROM delta_task
) TO '/delta-pgsql/data/delta_task.tsv';

-- id-de ei ekspordi, seda kusagil ei refereerita
COPY (
	SELECT task_id, extension_task_id FROM delta_task_due_date_extension_assoc
) TO '/delta-pgsql/data/delta_task_due_date_extension_assoc.tsv';

COPY (
	SELECT task_id, previous_date, change_reason FROM delta_task_due_date_history
) TO '/delta-pgsql/data/delta_task_due_date_history.tsv';

COPY (
	SELECT task_id, file_id FROM delta_task_file
) TO '/delta-pgsql/data/delta_task_file.tsv';

-- delta_log täies mahus

COPY (
	SELECT concat(log_entry_id, '_2'), created_date_time, level, creator_id, creator_name, computer_ip, computer_name, object_id, object_name, description FROM delta_log
) TO '/delta-pgsql/data/delta_log.tsv';

-- delta_register täies mahus.

COPY (
	SELECT * FROM delta_register
) TO '/delta-pgsql/data/delta_register.tsv';

-- õigused kõigile objektidele, mis üle kantakse. 
COPY (
	SELECT * FROM delta_node_permission where node_uuid in (select uuid from tmp_export_nodes)
) TO '/delta-pgsql/data/delta_node_permission.tsv';

COPY (
	SELECT * FROM delta_node_inheritspermissions where node_uuid in (select uuid from tmp_export_nodes)
) TO '/delta-pgsql/data/delta_node_permission_list.tsv';

-- alf_child_assoc kõik seosed, kus vähemalt ühes otsas on tmp_export_nodes sisalduv id.
-- Sealjuures mitte üle kanda tööülesannete ja logide assoceid.
COPY (
	select  nextval('export_sequence'), version,
	case when parent_export.id is not null
		then parent_export.new_id
		else parent_node_id end,
	type_qname_id, child_node_name_crc, child_node_name, 
	case when child_export.id is not null
		then child_export.new_id
		else child_node_id end, 
	qname_ns_id, qname_localname, is_primary, assoc_index
	FROM alf_child_assoc 
	left join tmp_export_nodes parent_export on parent_export.id = alf_child_assoc.parent_node_id
	left join tmp_export_nodes child_export on child_export.id = alf_child_assoc.child_node_id
	where alf_child_assoc.type_qname_id not in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
		where (local_name = 'task' and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0')
		or (local_name = 'documentLog' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		or (local_name = 'seriesLog' and uri = 'http://alfresco.webmedia.ee/model/series/1.0'))
	and (parent_export.id is not null or child_export.id is not null)
) TO '/delta-pgsql/data/alf_child_assoc.tsv';


-- alf_node_assoc, sama mis eelmine.
COPY (
	with const (max_val) as (
		select constant_value from tmp_constants where constant_name = 'ALF_NODE_ID_MAX_VALUE' limit 1
	)
	SELECT nextval('export_sequence'), version, 
	case when source_export.id is not null
		then source_export.new_id
		else source_node_id end, 
	case when target_export.id is not null
		then target_export.new_id
		else target_node_id end, 
	type_qname_id FROM alf_node_assoc 
	left join tmp_export_nodes target_export on target_export.id = alf_node_assoc.target_node_id
	left join tmp_export_nodes source_export on source_export.id = alf_node_assoc.source_node_id
	where (target_export.id is not null or source_export.id is not null)
) TO '/delta-pgsql/data/alf_node_assoc.tsv';


-- alf_node_aspects kõigi tmp_export_nodes jaoks 
COPY (
	SELECT export.new_id, 
	qname_id FROM alf_node_aspects aspects 
	join tmp_export_nodes export on export.id = aspects.node_id
) TO '/delta-pgsql/data/alf_node_aspects.tsv';

create table tmp_content_data (
   id bigint,
   new_id bigint
);

insert into tmp_content_data (select id, nextval('export_sequence') from alf_content_data);

create table tmp_content_url (
   id bigint,
   new_id bigint
);

insert into tmp_content_url (select id, nextval('export_sequence') from alf_content_url);

-- alf_node_properties kõigi tmp_export_nodes jaoks
COPY (
	SELECT export.new_id, 
	actual_type_n, persisted_type_n, boolean_value, 
	case when qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id where local_name = 'content' and uri = 'http://www.alfresco.org/model/content/1.0') 
		then tmp_content_data.new_id 
		else long_value end, 
	float_value, double_value, string_value, serializable_value, qname_id, list_index, locale_id FROM alf_node_properties props 
	join tmp_export_nodes export on export.id = props.node_id
	left join tmp_content_data on long_value = tmp_content_data.id
) TO '/delta-pgsql/data/alf_node_properties.tsv';

-- alf_content_data kõik andmed
COPY (
	with const_content_data (max_val) as (
		select constant_value from tmp_constants where constant_name = 'ALF_CONTENT_DATA_ID_MAX_VALUE' limit 1
	),
	const_content_url (max_val) as (
		select constant_value from tmp_constants where constant_name = 'ALF_CONTENT_URL_ID_MAX_VALUE' limit 1
	)
	SELECT tmp_content_data.new_id, version, tmp_content_url.new_id,
		content_mimetype_id, content_encoding_id, content_locale_id from alf_content_data
	join tmp_content_data on alf_content_data.id = tmp_content_data.id
	join tmp_content_url on alf_content_data.content_url_id = tmp_content_url.id
)  TO '/delta-pgsql/data/alf_content_data.tsv';

-- alf_content_url kõik andmed
COPY (
	SELECT tmp_content_url.new_id, version, content_url, content_url_short, content_url_crc, content_size from alf_content_url
	join tmp_content_url on tmp_content_url.id = alf_content_url.id
)  TO '/delta-pgsql/data/alf_content_url.tsv';

-- alf_transaction. Iga 10 ülekantava node'i jaoks üks uus transaktsioon
COPY (
	with const_server as (select min(id) as id from alf_server)
	select tmp_export_nodes.transaction_id, 1, (select id from const_server), (row_number() over ())::bigint::text, 
	(EXTRACT(EPOCH from now())::bigint * 1000) + (row_number() over ())
	from tmp_export_nodes
)  TO '/delta-pgsql/data/alf_transaction.tsv';

COPY (
	select * from alf_locale
) to '/delta-pgsql/data/alf_locale.tsv';

COPY (
	select * from alf_mimetype
) to '/delta-pgsql/data/alf_mimetype.tsv';