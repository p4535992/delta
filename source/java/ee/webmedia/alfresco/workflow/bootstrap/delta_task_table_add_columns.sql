ALTER TABLE delta_task ADD COLUMN wfs_creator_institution_name text;
ALTER TABLE delta_task ADD COLUMN wfs_compound_workflow_title text;
ALTER TABLE delta_task ADD COLUMN wfs_compound_workflow_comment text;
ALTER TABLE delta_task ADD COLUMN wfs_original_noderef_id text;
ALTER TABLE delta_task ADD COLUMN wfs_original_task_object_url text;
ALTER TABLE delta_task ALTER COLUMN workflow_id DROP NOT NULL;
ALTER TABLE delta_task ALTER COLUMN index_in_workflow DROP NOT NULL;
ALTER TABLE delta_task ADD CONSTRAINT workflow_index_and_id_both_filled_or_empty CHECK ((workflow_id IS NOT NULL AND index_in_workflow IS NOT NULL) OR (workflow_id IS NULL AND index_in_workflow IS NULL));