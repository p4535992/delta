<<<<<<< HEAD
CREATE INDEX delta_task_searchable_compound_workflow_type ON delta_task USING gin(to_tsvector('simple', wfs_searchable_compound_workflow_type));
CREATE INDEX delta_task_searchable_compound_workflow_owner_name ON delta_task USING gin(to_tsvector('simple', wfs_searchable_compound_workflow_owner_name));
CREATE INDEX delta_task_searchable_compound_workflow_owner_organization_name ON delta_task USING gin(to_tsvector('simple', wfs_searchable_compound_workflow_owner_organization_name));
CREATE INDEX delta_task_searchable_compound_workflow_owner_job_title ON delta_task USING gin(to_tsvector('simple', wfs_searchable_compound_workflow_owner_job_title));
=======
CREATE INDEX delta_task_searchable_compound_workflow_type ON delta_task USING gin(to_tsvector('simple', wfs_searchable_compound_workflow_type));
CREATE INDEX delta_task_searchable_compound_workflow_owner_name ON delta_task USING gin(to_tsvector('simple', wfs_searchable_compound_workflow_owner_name));
CREATE INDEX delta_task_searchable_compound_workflow_owner_organization_name ON delta_task USING gin(to_tsvector('simple', wfs_searchable_compound_workflow_owner_organization_name));
CREATE INDEX delta_task_searchable_compound_workflow_owner_job_title ON delta_task USING gin(to_tsvector('simple', wfs_searchable_compound_workflow_owner_job_title));
>>>>>>> develop-5.1
CREATE INDEX delta_task_searchable_compound_workflow_status ON delta_task USING gin(to_tsvector('simple', wfs_searchable_compound_workflow_status));