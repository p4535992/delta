-- TODO foreign key to delta_task.task_id?
CREATE TABLE temp_delta_task_to_compound_workflow AS ( 
	with cte as (
		select store.id as store_id, store_text from
		(select distinct store_id as store_text from delta_task) as delta_task_store
		join alf_store store ON store.protocol || '://' || store.identifier = delta_task_store.store_text
	)
	
	select task.task_id as task_id, compound_workflow.uuid, compound_workflow.store_id 
	from delta_task task
	join cte on task.store_id = cte.store_text 
	join alf_node workflow on (workflow.store_id = cte.store_id  and workflow.uuid = task.workflow_id)
	join alf_child_assoc workflow_assoc on workflow_assoc.child_node_id = workflow.id
	join alf_node compound_workflow on workflow_assoc.parent_node_id = compound_workflow.id
	join alf_qname qname on qname.id = compound_workflow.type_qname_id
	join alf_namespace ns on ns.id = qname.ns_id
	where (local_name = 'compoundWorkflow' or local_name = 'compoundWorkflowDefinition') and uri = 'http://alfresco.webmedia.ee/model/workflow/common/1.0'
	and (wfs_compound_workflow_id is null or wfs_compound_workflow_store_id is null)
);

-- add indexes to temp table
CREATE INDEX tmp_task_id_idx ON temp_delta_task_to_compound_workflow (task_id);

-- do the actual update
UPDATE delta_task
SET wfs_compound_workflow_id = uuid, wfs_compound_workflow_store_id = temp_delta_task_to_compound_workflow.store_id
from temp_delta_task_to_compound_workflow where temp_delta_task_to_compound_workflow.task_id = delta_task.task_id;

-- this table is no longer needed
DROP TABLE temp_delta_task_to_compound_workflow;