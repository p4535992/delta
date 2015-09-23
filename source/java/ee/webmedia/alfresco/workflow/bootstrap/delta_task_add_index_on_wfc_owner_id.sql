DROP INDEX IF EXISTS delta_task_wfc_owner_id;
CREATE INDEX delta_task_wfc_owner_id ON delta_task (wfc_owner_id);
