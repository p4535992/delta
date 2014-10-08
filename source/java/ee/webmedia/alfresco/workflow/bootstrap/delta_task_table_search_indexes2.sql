<<<<<<< HEAD
-- no index is created for is_searchable field because in query conditions only TRUE value is searched
-- and most of table rows have is_searchable=true

-- Most of tasks are in status 'teostatud', so threre is no point in including those values to index
CREATE INDEX delta_task_status ON delta_task (wfc_status) WHERE wfc_status != 'teostatud';
-- Only search for wfs_sent_dvk_id field is currently performed with IS NOT NULL condition and most of the values are NULL in this column
CREATE INDEX delta_task_sent_dvk_id ON delta_task (wfs_sent_dvk_id) WHERE wfs_sent_dvk_id IS NOT NULL;
-- Only search for wfs_active field is currently performed with IS NOT NULL condition and most of the values are NULL in this column
CREATE INDEX delta_task_active ON delta_task (wfs_active) WHERE wfs_active IS NOT NULL;
-- Only search for wfs_completed_overdue field is currently performed with IS TRUE condition and most of the values are assumed to be FALSE in this column.
-- If it should occur that TRUE and FALSE values are roughly evenly represented in this column, this index could be dropped 
CREATE INDEX delta_task_completed_overdue ON delta_task (wfs_completed_overdue) WHERE wfs_completed_overdue IS TRUE;

-- Regular indexes
CREATE INDEX delta_task_started_date_time ON delta_task (wfc_started_date_time);
CREATE INDEX delta_task_task_type ON delta_task (task_type);
CREATE INDEX delta_task_due_date ON delta_task (wfs_due_date);
CREATE INDEX delta_task_completed_date_time ON delta_task (wfc_completed_date_time);
CREATE INDEX delta_task_outcome ON delta_task (wfc_outcome);
CREATE INDEX delta_task_stopped_date_time ON delta_task (wfc_stopped_date_time);
CREATE INDEX delta_task_document_type ON delta_task (wfc_document_type);
-- If no additional archival stores (from archivals-additional conf propert) are added, 
-- this index is probably used only when searching for 'archive://SpacesStore' 
CREATE INDEX delta_task_store_id ON delta_task (store_id);

=======
-- no index is created for is_searchable field because in query conditions only TRUE value is searched
-- and most of table rows have is_searchable=true

-- Most of tasks are in status 'teostatud', so threre is no point in including those values to index
CREATE INDEX delta_task_status ON delta_task (wfc_status) WHERE wfc_status != 'teostatud';
-- Only search for wfs_sent_dvk_id field is currently performed with IS NOT NULL condition and most of the values are NULL in this column
CREATE INDEX delta_task_sent_dvk_id ON delta_task (wfs_sent_dvk_id) WHERE wfs_sent_dvk_id IS NOT NULL;
-- Only search for wfs_active field is currently performed with IS NOT NULL condition and most of the values are NULL in this column
CREATE INDEX delta_task_active ON delta_task (wfs_active) WHERE wfs_active IS NOT NULL;
-- Only search for wfs_completed_overdue field is currently performed with IS TRUE condition and most of the values are assumed to be FALSE in this column.
-- If it should occur that TRUE and FALSE values are roughly evenly represented in this column, this index could be dropped 
CREATE INDEX delta_task_completed_overdue ON delta_task (wfs_completed_overdue) WHERE wfs_completed_overdue IS TRUE;

-- Regular indexes
CREATE INDEX delta_task_started_date_time ON delta_task (wfc_started_date_time);
CREATE INDEX delta_task_task_type ON delta_task (task_type);
CREATE INDEX delta_task_due_date ON delta_task (wfs_due_date);
CREATE INDEX delta_task_completed_date_time ON delta_task (wfc_completed_date_time);
CREATE INDEX delta_task_outcome ON delta_task (wfc_outcome);
CREATE INDEX delta_task_stopped_date_time ON delta_task (wfc_stopped_date_time);
CREATE INDEX delta_task_document_type ON delta_task (wfc_document_type);
-- If no additional archival stores (from archivals-additional conf propert) are added, 
-- this index is probably used only when searching for 'archive://SpacesStore' 
CREATE INDEX delta_task_store_id ON delta_task (store_id);

>>>>>>> develop-5.1
CREATE INDEX delta_task_due_date_history_task_id ON delta_task_due_date_history(task_id);