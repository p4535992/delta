-- Use array type columns (instead of separate tables), because:
-- Suitable operators are supported for searching inside array types
-- There are few values (5-10) in each array, so it is okay that whole array is returned in each query
-- Array type columns can be indexed
-- At present there is no need to update separate array elements

ALTER TABLE delta_task DROP COLUMN wfc_owner_organization_name;
ALTER TABLE delta_task ADD COLUMN wfc_owner_organization_name text[];
CREATE INDEX delta_task_owner_organization_names ON delta_task USING GIN(wfc_owner_organization_name);

ALTER TABLE delta_task ADD COLUMN store_id text;