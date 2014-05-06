-- Table: delta_node_inheritspermissions

-- DROP TABLE delta_node_inheritspermissions;

CREATE TABLE delta_node_inheritspermissions
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
  
-- Table: delta_node_permission

-- DROP TABLE delta_node_permission;
  
CREATE TABLE delta_node_permission
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

CREATE INDEX ON delta_node_permission (create_document) WHERE create_document = TRUE;
CREATE INDEX ON delta_node_permission (view_document_meta_data) WHERE view_document_meta_data = FALSE;
CREATE INDEX ON delta_node_permission (edit_document) WHERE edit_document = TRUE;
CREATE INDEX ON delta_node_permission (view_document_files) WHERE view_document_files = FALSE;
CREATE INDEX ON delta_node_permission (create_case_file) WHERE create_case_file = TRUE;
CREATE INDEX ON delta_node_permission (view_case_file) WHERE view_case_file = TRUE;
CREATE INDEX ON delta_node_permission (edit_case_file) WHERE edit_case_file = TRUE;
CREATE INDEX ON delta_node_permission (participate_at_forum) WHERE participate_at_forum = TRUE;


  