CREATE INDEX delta_task_owner_name ON delta_task USING gin(to_tsvector('simple', wfc_owner_name));
CREATE INDEX delta_task_creator_name ON delta_task USING gin(to_tsvector('simple', wfc_creator_name));
CREATE INDEX delta_task_owner_job_title ON delta_task USING gin(to_tsvector('simple', wfc_owner_job_title));
CREATE INDEX delta_task_comment ON delta_task USING gin(to_tsvector('simple', wfs_comment));
CREATE INDEX delta_task_resolution ON delta_task USING gin(to_tsvector('simple', coalesce(wfs_resolution, '') || ' ' || coalesce(wfs_workflow_resolution, '')));