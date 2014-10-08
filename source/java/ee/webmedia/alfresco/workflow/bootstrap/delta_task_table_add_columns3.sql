<<<<<<< HEAD
ALTER TABLE delta_task DROP COLUMN wfs_searchable_compound_workflow_owner_organization_name;
ALTER TABLE delta_task ADD COLUMN wfs_searchable_compound_workflow_owner_organization_name text[];
=======
ALTER TABLE delta_task DROP COLUMN wfs_searchable_compound_workflow_owner_organization_name;
ALTER TABLE delta_task ADD COLUMN wfs_searchable_compound_workflow_owner_organization_name text[];
>>>>>>> develop-5.1
CREATE INDEX delta_task_searchable_compound_workflow_owner_organization_names ON delta_task USING GIN(wfs_searchable_compound_workflow_owner_organization_name);