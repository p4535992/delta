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

create table tmp_acl_to_delete (
	acl_id bigint
);

insert into tmp_acl_to_delete (
	select acl_id from alf_node 
	join tmp_nodes_to_delete on tmp_nodes_to_delete.node_id = alf_node.id);

create table tmp_ace_to_delete (
	ace_id bigint
);

insert into tmp_ace_to_delete (
	select ace_id from alf_acl_member 
	join tmp_acl_to_delete on tmp_acl_to_delete.acl_id = alf_acl_member.acl_id);

delete from alf_node 
	using tmp_nodes_to_delete
	where id = tmp_nodes_to_delete.node_id;

delete from alf_transaction 
	using tmp_transactions_to_delete
	where id = tmp_transactions_to_delete.txn_id;

delete from alf_acl_member 
	using tmp_acl_to_delete
	where alf_acl_member.acl_id = tmp_acl_to_delete.acl_id;

delete from alf_access_control_list 
	using tmp_acl_to_delete
	where id = tmp_acl_to_delete.acl_id;

delete from alf_access_control_entry 
	using tmp_ace_to_delete
	where id = tmp_ace_to_delete.ace_id;

drop table tmp_transactions_to_delete;

drop table tmp_nodes_to_delete;

drop table tmp_acl_to_delete;

drop table tmp_ace_to_delete;