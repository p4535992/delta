CREATE TABLE delta_task (
  task_id text PRIMARY KEY,
  workflow_id text NOT NULL,
  task_type text NOT NULL,
  index_in_workflow integer NOT NULL,
  
  wfc_status text,
  wfc_creator_name text,
  wfc_started_date_time timestamp,
  wfc_stopped_date_time timestamp,
  wfc_owner_id text,
  wfc_owner_name text,
  wfc_previous_owner_id text,
  wfc_owner_email text,
  wfc_owner_group text,
  wfc_outcome text,
  wfc_document_type text,
  wfc_completed_date_time timestamp,
  wfc_owner_organization_name text,
  wfc_owner_job_title text,
  wfc_parallel_tasks boolean,

  wfs_creator_id text,
  wfs_creator_email text,
  wfs_workflow_resolution text,
  wfs_completed_overdue boolean,
  wfs_due_date timestamp,
  wfs_due_date_days integer,
  wfs_is_due_date_days_working_days boolean,
  wfs_comment text,
  wfs_file_versions text,
  
  -- START: external review task fields
  wfs_institution_name text,
  wfs_institution_code text,
  wfs_creator_institution_code text,
  wfs_original_dvk_id text,
  wfs_sent_dvk_id text,
  wfs_recieved_dvk_id text,
  wfs_send_status text,
  wfs_send_date_time timestamp,
  -- END: external review task fields
  
  wfs_resolution text,
  wfs_temp_outcome text,
  wfs_active boolean,
  wfs_send_order_assignment_completed_email boolean,
  
  -- START: due date extension task fields
  wfs_proposed_due_date timestamp,
  wfs_confirmed_due_date timestamp,
  -- END: due date extension task fields
  
  has_due_date_history boolean DEFAULT false NOT NULL,
  has_files boolean DEFAULT false NOT NULL,
  is_searchable boolean NOT NULL,
  UNIQUE(workflow_id, index_in_workflow) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_delta_task_index_in_workflow ON delta_task (workflow_id, index_in_workflow);