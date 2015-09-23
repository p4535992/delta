DROP INDEX IF EXISTS delta_task_compound_workflow_id;
CREATE INDEX delta_task_compound_workflow_id ON delta_task (wfs_compound_workflow_id);
