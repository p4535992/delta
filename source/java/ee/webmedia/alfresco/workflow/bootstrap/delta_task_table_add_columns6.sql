ALTER TABLE delta_task ADD COLUMN wfc_viewed_by_owner BOOLEAN DEFAULT FALSE;
UPDATE delta_task SET wfc_viewed_by_owner=true;