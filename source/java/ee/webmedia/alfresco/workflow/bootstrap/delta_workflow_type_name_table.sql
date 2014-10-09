DROP TABLE IF EXISTS delta_workflow_type_name;

CREATE TABLE delta_workflow_type_name (
	type_qname_id bigint NOT NULL,
	name character varying(255) NOT NULL
);