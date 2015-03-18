create table tmp_task_nodes_to_delete (
	node_id bigint
);

insert into tmp_task_nodes_to_delete (
	select id from alf_node where type_qname_id in
		(select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
		where (uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0' and local_name in 
			('assignmentTask', 'orderAssignmentTask', 'informationTask', 'opinionTask', 'reviewTask', 
				'externalReviewTask', 'linkedReviewTask', 'signatureTask', 'confirmationTask', 'dueDateExtensionTask', 'groupAssignmentTask'))
		or (uri = 'http://alfresco.webmedia.ee/model/document/common/1.0' and local_name = 'documentLog' ))
);

CREATE OR REPLACE FUNCTION copyNotExportedNodeRefs() returns void AS 
$BODY$
DECLARE
    store_row record;
    file_name text;
begin
	FOR store_row IN select distinct store.id, protocol, identifier from alf_node node join alf_store store on store.id = node.store_id join tmp_task_nodes_to_delete exports on exports.node_id = node.id LOOP
		file_name = '/delta-pgsql/data/task_IndexInfo2Deletions_' || store_row.protocol || '_' || store_row.identifier || '.tsv';
		execute 'COPY (
			select store.protocol || ''://'' || store.identifier || ''/'' || node.uuid 
			FROM alf_node node
			join alf_store store on store.id = node.store_id and store.id = ' || store_row.id
			|| ' join tmp_task_nodes_to_delete exports on exports.node_id = node.id
		) TO ''' || file_name || '''';
	END LOOP;
end
$BODY$
LANGUAGE plpgsql;

select copyNotExportedNodeRefs();

delete from alf_child_assoc 
	using tmp_task_nodes_to_delete
	where child_node_id = tmp_task_nodes_to_delete.node_id;

delete from alf_child_assoc 
	using tmp_task_nodes_to_delete
	where parent_node_id = tmp_task_nodes_to_delete.node_id;

delete from alf_node_assoc 
	using tmp_task_nodes_to_delete
	where source_node_id = tmp_task_nodes_to_delete.node_id;

delete from alf_node_assoc 
	using tmp_task_nodes_to_delete
	where target_node_id = tmp_task_nodes_to_delete.node_id;

delete from alf_node_aspects 
	using tmp_task_nodes_to_delete
	where alf_node_aspects.node_id = tmp_task_nodes_to_delete.node_id;

delete from alf_node_properties 
	using tmp_task_nodes_to_delete
	where alf_node_properties.node_id = tmp_task_nodes_to_delete.node_id;

create table tmp_transactions_to_delete (
	txn_id bigint
);

insert into tmp_transactions_to_delete (
	select alf_transaction.id from alf_transaction 
	left join alf_node node on node.transaction_id = alf_transaction.id
	where node.id is null); 

delete from alf_node 
	using tmp_task_nodes_to_delete
	where id = tmp_task_nodes_to_delete.node_id;

delete from alf_transaction 
	using tmp_transactions_to_delete
	where id = tmp_transactions_to_delete.txn_id;

drop table tmp_transactions_to_delete;

drop table tmp_task_nodes_to_delete;