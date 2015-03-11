drop table if exists tmp_delta_node_inheritspermissions;
drop table if exists tmp_delta_node_permission;
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

alter table alf_node drop constraint if exists fk_alf_node_acl;
alter table alf_attributes drop constraint if exists fk_alf_attr_acl;
alter table avm_nodes drop constraint if exists fk_avm_n_acl;
alter table avm_stores drop constraint if exists fk_avm_s_acl;

create table tmp_nodes_deleted_on_merge (
	id bigint not null
);

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

CREATE TABLE tmp_server (
  id bigint NOT NULL,
  version bigint NOT NULL,
  ip_address character varying(15) NOT NULL
)
WITH (
  OIDS=FALSE
);
ALTER TABLE alf_server
  OWNER TO alfresco;

copy tmp_server from '/delta-pgsql/data/alf_server.tsv';
  
insert into alf_server (
	select * from tmp_server where not exists (select id from alf_server where alf_server.ip_address = tmp_server.ip_address)
);

CREATE TABLE tmp_transaction (
  id bigint NOT NULL,
  version bigint NOT NULL,
  server_id bigint,
  change_txn_id character varying(56) NOT NULL,
  commit_time_ms bigint
)
WITH (
  OIDS=FALSE
);
ALTER TABLE alf_transaction
  OWNER TO alfresco;

COPY tmp_transaction from '/delta-pgsql/data/alf_transaction.tsv';

insert into alf_transaction (
	select tmp_transaction.id, tmp_transaction.version, alf_server.id, change_txn_id, commit_time_ms from tmp_transaction 
	join tmp_server on tmp_server.id = tmp_transaction.server_id
	join alf_server on tmp_server.ip_address = alf_server.ip_address
);

drop table tmp_transaction;
	
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

COPY tmp_import_alf_node from '/delta-pgsql/data/alf_node.tsv';

alter table alf_child_assoc drop constraint fk_alf_cass_cnode;
alter table alf_child_assoc drop constraint fk_alf_cass_pnode;

alter table alf_node_assoc drop constraint fk_alf_nass_snode;
alter table alf_node_assoc drop constraint fk_alf_nass_tnode;

create table tmp_import_alf_node_errors (
	id bigint
);

-- imporditavad node'id, mille andmekoosseis ei vasta alf_node tabeli piirangutele, on viga, lähevad tmp_import_alf_node_errors tabelisse
with import_errors as (
	delete from tmp_import_alf_node 
	where id is null or version is null or store_id is null or uuid is null 
	or transaction_id is null or node_deleted is null or type_qname_id is null
	returning id
)
insert into tmp_import_alf_node_errors (select * from import_errors);

insert into alf_node (select import.id, import.version, import.store_id, import.uuid, import.transaction_id, import.node_deleted,
	tmp_alf_qname.local_id,
	import.acl_id, import.audit_creator, import.audit_created, import.audit_modifier, import.audit_modified from tmp_import_alf_node import
	left join alf_node node on node.uuid = import.uuid
	left join tmp_alf_qname on import.type_qname_id = tmp_alf_qname.imported_id
	where node.id is null);
	
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
      
insert into alf_child_assoc (
	select tmp_import_alf_child_assoc.id, tmp_import_alf_child_assoc.version, tmp_import_alf_child_assoc.parent_node_id, 
	tmp_alf_qname.local_id,
	tmp_import_alf_child_assoc.child_node_name_crc, tmp_import_alf_child_assoc.child_node_name, tmp_import_alf_child_assoc.child_node_id, tmp_import_alf_child_assoc.qname_ns_id, 
	tmp_import_alf_child_assoc.qname_localname, tmp_import_alf_child_assoc.is_primary, tmp_import_alf_child_assoc.assoc_index
	from tmp_import_alf_child_assoc 
	left join tmp_alf_qname on tmp_alf_qname.imported_id = tmp_import_alf_child_assoc.type_qname_id
	join alf_node parent_import on parent_import.id = tmp_import_alf_child_assoc.parent_node_id
	join alf_node child_import on child_import.id = tmp_import_alf_child_assoc.child_node_id
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

insert into alf_node_assoc (
	select tmp_import_alf_node_assoc.id, tmp_import_alf_node_assoc.version, source_node_id, target_node_id,
	tmp_alf_qname.local_id
	from tmp_import_alf_node_assoc
	left join tmp_alf_qname on tmp_alf_qname.imported_id = tmp_import_alf_node_assoc.type_qname_id
	join alf_node source_import on source_import.id = tmp_import_alf_node_assoc.source_node_id
	join alf_node target_import on target_import.id = tmp_import_alf_node_assoc.target_node_id
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
	join alf_node on alf_node.id = tmp_import_alf_node_aspects.node_id
);

-- ülejäänud tabelid ilma lisakontrollideta importida otse
-- delta_task ja seotud delta_task_xxx tabelid täies mahus.
CREATE TABLE tmp_delta_task
(
  task_id text NOT NULL,
  workflow_id text,
  task_type text NOT NULL,
  index_in_workflow integer,
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
  store_id text NOT NULL,
  wfs_creator_institution_name text,
  wfs_compound_workflow_title text,
  wfs_compound_workflow_comment text,
  wfs_original_noderef_id text,
  wfs_original_task_object_url text,
  wfs_searchable_compound_workflow_type text,
  wfs_searchable_compound_workflow_owner_name text,
  wfs_searchable_compound_workflow_owner_job_title text,
  wfs_searchable_compound_workflow_created_date_time timestamp without time zone,
  wfs_searchable_compound_workflow_started_date_time timestamp without time zone,
  wfs_searchable_compound_workflow_stopped_date_time timestamp without time zone,
  wfs_searchable_compound_workflow_finished_date_time timestamp without time zone,
  wfs_searchable_compound_workflow_status text,
  wfs_compound_workflow_id text,
  wfs_compound_workflow_store_id bigint,
  wfs_searchable_compound_workflow_owner_organization_name text[],
  wfc_owner_substitute_name text,
  wfc_viewed_by_owner boolean DEFAULT false,
  wfs_received_date_time timestamp without time zone
)
WITH (
  OIDS=FALSE
);
ALTER TABLE delta_task
  OWNER TO alfresco;

COPY tmp_delta_task from '/delta-pgsql/data/delta_task.tsv';

insert into delta_task (select * from tmp_delta_task);

-- id-d ei ekspordi, seda kusagil ei refereerita
COPY delta_task_due_date_extension_assoc (task_id, extension_task_id) from '/delta-pgsql/data/delta_task_due_date_extension_assoc.tsv';

COPY delta_task_due_date_history (task_id, previous_date, change_reason) FROM '/delta-pgsql/data/delta_task_due_date_history.tsv';

COPY delta_task_file (task_id, file_id) FROM '/delta-pgsql/data/delta_task_file.tsv';

COPY delta_log from '/delta-pgsql/data/delta_log.tsv';

-- permissions
CREATE TABLE tmp_delta_node_inheritspermissions
(
  node_uuid character varying(36) NOT NULL,
  inherits boolean NOT NULL,
  acl_id bigint NOT NULL);
  
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

COPY tmp_delta_node_permission from '/delta-pgsql/data/delta_node_permission.tsv';

insert into delta_node_permission (select * from tmp_delta_node_permission);

COPY tmp_delta_node_inheritspermissions from '/delta-pgsql/data/delta_node_permission_list.tsv';

insert into delta_node_inheritspermissions (select * from tmp_delta_node_inheritspermissions);

-- content
drop table if exists tmp_alf_content_url_imported;
create table if not exists tmp_alf_content_url_imported(like alf_content_url);
COPY tmp_alf_content_url_imported from '/delta-pgsql/data/alf_content_url.tsv';

select * into alf_content_url from tmp_alf_content_url_imported

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

-- remove duplicate content data and content urls
drop table if exists tmp_duplicate_content_url_id;
create table tmp_duplicate_content_url_id (
  old_id bigint not null,
  new_id bigint not null
);

insert into tmp_duplicate_content_url_id (
  select min(id) as old_id, max(id) as new_id
  from alf_content_url 
  group by content_url
  having count(1) > 1
  and max(id) in (select id from tmp_alf_content_url_imported)
);

create table if not exists tmp_alf_content_url_deleted (like alf_content_url);
create table if not exists tmp_alf_content_data_deleted (like alf_content_data);

insert into tmp_alf_content_url_deleted (
  select * from alf_content_url url
  where url.id in (select duplicate.old_id from tmp_duplicate_content_url_id duplicate)
);

insert into tmp_alf_content_data_deleted (
  select * from alf_content_data
  where content_url_id in (select old_id from tmp_duplicate_content_url_id)
);

drop table if exists tmp_old_content_data_id_to_new;
create table tmp_old_content_data_id_to_new (
  id bigint not null,
  new_id bigint not null
);

COPY tmp_old_content_data_id_to_new from '/delta-pgsql/data/content_data_old_id_to_new_id.tsv';

-- alf_content_data.id is used in alf_node_properties.long_value, update references
with props_qname_ids as (
   select alf_qname.id from alf_qname
   join alf_namespace ns on ns.id = ns_id
   where (local_name = 'content' and uri = 'http://www.alfresco.org/model/content/1.0')
   or (local_name = 'fileContents' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
)
update alf_node_properties props
set long_value = tmp.new_id
from tmp_old_content_data_id_to_new AS tmp
where actual_type_n = 3 -- long
and qname_id in (select id from props_qname_ids)
and props.long_value = tmp.id;

delete from alf_content_url
where id in (select old_id from tmp_duplicate_content_url_id);

delete from alf_content_data
where content_url_id in (select old_id from tmp_duplicate_content_url_id);

-- resettida sequence'id
select setval('delta_task_due_date_extension_task_due_date_extension_assoc_seq', (select max(task_due_date_extension_assoc_id) from delta_task_due_date_extension_assoc));
select setval('delta_task_file_task_file_id_seq', (select max(task_file_id) from delta_task_file));
select setval('delta_task_due_date_history_task_due_date_history_id_seq', (select max(task_due_date_history_id) from delta_task_due_date_history));
select setval('alf_content_data_seq', (select max(id) from alf_content_data));
select setval('alf_content_url_seq', (select max(id) from alf_content_url));
SELECT setval('hibernate_sequence', greatest((select max(id) + 1 from public.alf_node), (select max(id) + 1 from public.alf_transaction), (select max(id) + 1 from public.alf_child_assoc), (select max(id) + 1 from public.alf_node_assoc), (select max(id) + 1 from public.alf_content_data), (select max(id) + 1 from public.alf_content_url)));
