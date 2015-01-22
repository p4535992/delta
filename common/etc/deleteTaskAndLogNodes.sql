create table tmp_nodes_to_delete (
	node_id bigint
);

insert into tmp_nodes_to_delete (
	select id from alf_node where type_qname_id in
		(select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id
		where (uri = 'http://alfresco.webmedia.ee/model/workflow/specific/1.0' and local_name in 
			('assignmentTask', 'orderAssignmentTask', 'informationTask', 'opinionTask', 'reviewTask', 
				'externalReviewTask', 'linkedReviewTask', 'signatureTask', 'confirmationTask', 'dueDateExtensionTask', 'groupAssignmentTask'))
		or (uri = 'http://alfresco.webmedia.ee/model/document/common/1.0' and local_name = 'documentLog' ))
);

delete from alf_child_assoc 
	using tmp_nodes_to_delete
	where child_node_id = tmp_nodes_to_delete.node_id;

delete from alf_child_assoc 
	using tmp_nodes_to_delete
	where parent_node_id = tmp_nodes_to_delete.node_id;

delete from alf_node_assoc 
	using tmp_nodes_to_delete
	where source_node_id = tmp_nodes_to_delete.node_id;

delete from alf_node_assoc 
	using tmp_nodes_to_delete
	where target_node_id = tmp_nodes_to_delete.node_id;

delete from alf_node_aspects 
	using tmp_nodes_to_delete
	where alf_node_aspects.node_id = tmp_nodes_to_delete.node_id;

delete from alf_node_properties 
	using tmp_nodes_to_delete
	where alf_node_properties.node_id = tmp_nodes_to_delete.node_id;

create table tmp_transactions_to_delete (
	txn_id bigint
);

insert into tmp_transactions_to_delete (
	select alf_transaction.id from alf_transaction 
	left join alf_node node on node.transaction_id = alf_transaction.id
	where node.id is null); 

delete from alf_node 
	using tmp_nodes_to_delete
	where id = tmp_nodes_to_delete.node_id;

delete from alf_transaction 
	using tmp_transactions_to_delete
	where id = tmp_transactions_to_delete.txn_id;

begin;

delete from alf_acl_member 
	where not exists (select acl_id from alf_node where acl_id = alf_acl_member.acl_id);

delete from alf_access_control_list 
 where not exists (select acl_id from alf_node where acl_id = alf_access_control_list.id)
 and id not in (select acl_id from avm_stores);

delete from alf_access_control_entry 
 where not exists (select ace_id from alf_acl_member where ace_id = alf_access_control_entry.id);

commit;

drop table tmp_transactions_to_delete;

drop table tmp_nodes_to_delete;