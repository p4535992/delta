-- Juhend skriptide kasutamiseks asub doc/Delta tundlike andmete asendamise skript.docx
-- 1) Update text properties to dummy values
update alf_node_properties as props
set string_value='x', serializable_value=null, persisted_type_n=6
from
alf_node as node, alf_qname as type_qname,
alf_qname as prop_qname, 
alf_namespace as type_namespace, alf_namespace as prop_namespace
where 
	-- property type is text
	props.string_value is not null and string_value not like '' and props.actual_type_n=6
	and (
			( -- doccom:document type nodes
				type_namespace.uri='http://alfresco.webmedia.ee/model/document/common/1.0'
				and type_qname.local_name='document'
				and (
					prop_namespace.uri='http://alfresco.webmedia.ee/model/document/dynamic/1.0'
					OR
					(prop_namespace.uri='http://alfresco.webmedia.ee/model/document/common/1.0'
						and prop_qname.local_name in ('fileNames', 'recipient', 'recipientRegNr', 'resolution')
					)
				)
			)
			OR 
			( -- doccom:sendInfo type nodes
				type_namespace.uri='http://alfresco.webmedia.ee/model/document/common/1.0'
				and type_qname.local_name='sendInfo'
				and prop_namespace.uri='http://alfresco.webmedia.ee/model/document/common/1.0'
				and prop_qname.local_name in ('recipient', 'recipientRegNr', 'resolution')
			)
			OR 
			( -- doccom:documentLog type nodes
				type_namespace.uri='http://alfresco.webmedia.ee/model/document/common/1.0'
				and type_qname.local_name='documentLog'
				and prop_namespace.uri='http://alfresco.webmedia.ee/model/document/common/1.0'
				and prop_qname.local_name in ('eventDescription')
			)
			OR 
			( -- docchild namespace nodes (contractParty, applicantAbroad, errandAbroad, applicantDomestic, errandDomestic)
				type_namespace.uri='http://alfresco.webmedia.ee/model/document/child/1.0'
				and prop_namespace.uri='http://alfresco.webmedia.ee/model/document/dynamic/1.0'
			)
			OR
			(prop_namespace.uri='http://alfresco.webmedia.ee/model/workflow/common/1.0'
				and prop_qname.local_name in ('name')
			)
			OR
			(prop_namespace.uri='http://alfresco.webmedia.ee/model/workflow/specific/1.0'
				and prop_qname.local_name in ('description', 'resolution', 'comment', 'workflowResolution')
			)
		)
	and not 
		(prop_namespace.uri='http://www.alfresco.org/model/content/1.0'
			and prop_qname.local_name='content'
		)
	and props.node_id=node.id
	and prop_qname.id=props.qname_id
	and type_qname.id=node.type_qname_id
	and type_namespace.id=type_qname.ns_id
	and prop_namespace.id=prop_qname.ns_id;
	
-- 2) update delt_log to dummy values
update delta_log set computer_ip='0.0.0.0'where computer_ip is not null;
update delta_log set computer_name='x' where computer_name is not null;
update delta_log set description='x' where description is not null;

-- 3) update delta_task to dummy values
update delta_task set wfs_workflow_resolution='x' where wfs_resolution is not null;
update delta_task set wfs_comment='x' where wfs_comment is not null;
update delta_task set wfs_resolution='x' where wfs_resolution is not null;
		
-- 4) update file:displayName property on content nodes, excluding templates
update alf_node_properties as props
set string_value='x', serializable_value=null, persisted_type_n=6
from
alf_node as node, alf_qname as type_qname,
alf_qname as prop_qname, 
alf_namespace as type_namespace, alf_namespace as prop_namespace
where 
	-- property type is text
	props.string_value is not null and string_value not like '' and props.actual_type_n=6

	and type_namespace.uri='http://www.alfresco.org/model/content/1.0'
	and type_qname.local_name='content'
	and prop_namespace.uri='http://alfresco.webmedia.ee/model/file/1.0'
	and prop_qname.local_name in ('displayName')

	and props.node_id=node.id
	and prop_qname.id=props.qname_id
	and type_qname.id=node.type_qname_id
	and type_namespace.id=type_qname.ns_id
	and prop_namespace.id=prop_qname.ns_id

	and props.node_id not in (
		-- exclude templates
		select distinct node.id from alf_node_properties as props
		left join alf_node as node on node.id=props.node_id
		left join alf_child_assoc as assoc on assoc.child_node_id=node.id
		left join alf_node as parent on parent.id=assoc.parent_node_id
		left join alf_qname as prop_qname on prop_qname.id=props.qname_id
		left join alf_namespace as prop_namespace on prop_namespace.id=prop_qname.ns_id
		where 
		prop_qname.local_name='content'
		and prop_namespace.uri='http://www.alfresco.org/model/content/1.0'
		and parent.id=(
			-- templates root node
			select node_id from alf_node child
			left join alf_node_properties props on props.node_id=child.id
			left join alf_child_assoc assoc on assoc.child_node_id=child.id
			left join alf_node parent on assoc.parent_node_id=parent.id
			left join alf_store parent_store on parent.store_id=parent_store.id
			left join alf_qname parent_type_qname on parent_type_qname.id=parent.type_qname_id
			left join alf_namespace parent_namespace on parent_namespace.id=parent_type_qname.ns_id
			left join alf_qname child_prop_type_qname on child_prop_type_qname.id=props.qname_id
			left join alf_namespace child_prop_namespace on child_prop_namespace.id=child_prop_type_qname.ns_id
			where props.string_value='templates'
				and child_prop_type_qname.local_name='name'
				and child_prop_namespace.uri='http://www.alfresco.org/model/content/1.0'
				and parent_store.protocol='workspace'
				and parent_store.identifier='SpacesStore'
				and parent_type_qname.local_name='store_root'
				and parent_namespace.uri='http://www.alfresco.org/model/system/1.0'
			)
		);

-- 5) create sequence for dummy filenames
DROP SEQUENCE IF EXISTS tmp_non_sensitive_filename_sequence;
CREATE SEQUENCE tmp_non_sensitive_filename_sequence;

-- 6) update cm:name property on content nodes, excluding templates
update alf_node_properties as props
set string_value='x' || nextval('tmp_non_sensitive_filename_sequence'), serializable_value=null, persisted_type_n=6
from
alf_node as node, alf_qname as type_qname,
alf_qname as prop_qname, 
alf_namespace as type_namespace, alf_namespace as prop_namespace
where 
	-- property type is text
	props.string_value is not null and string_value not like '' and props.actual_type_n=6

	and type_namespace.uri='http://www.alfresco.org/model/content/1.0'
	and type_qname.local_name='content'
	and prop_namespace.uri='http://www.alfresco.org/model/content/1.0'
	and prop_qname.local_name in ('name')

	and props.node_id=node.id
	and prop_qname.id=props.qname_id
	and type_qname.id=node.type_qname_id
	and type_namespace.id=type_qname.ns_id
	and prop_namespace.id=prop_qname.ns_id

	and props.node_id not in (
		-- exclude templates
		select distinct node.id from alf_node_properties as props
		left join alf_node as node on node.id=props.node_id
		left join alf_child_assoc as assoc on assoc.child_node_id=node.id
		left join alf_node as parent on parent.id=assoc.parent_node_id
		left join alf_qname as prop_qname on prop_qname.id=props.qname_id
		left join alf_namespace as prop_namespace on prop_namespace.id=prop_qname.ns_id
		where 
		prop_qname.local_name='content'
		and prop_namespace.uri='http://www.alfresco.org/model/content/1.0'
		and parent.id=(
			-- templates root node
			select node_id from alf_node child
			left join alf_node_properties props on props.node_id=child.id
			left join alf_child_assoc assoc on assoc.child_node_id=child.id
			left join alf_node parent on assoc.parent_node_id=parent.id
			left join alf_store parent_store on parent.store_id=parent_store.id
			left join alf_qname parent_type_qname on parent_type_qname.id=parent.type_qname_id
			left join alf_namespace parent_namespace on parent_namespace.id=parent_type_qname.ns_id
			left join alf_qname child_prop_type_qname on child_prop_type_qname.id=props.qname_id
			left join alf_namespace child_prop_namespace on child_prop_namespace.id=child_prop_type_qname.ns_id
			where props.string_value='templates'
				and child_prop_type_qname.local_name='name'
				and child_prop_namespace.uri='http://www.alfresco.org/model/content/1.0'
				and parent_store.protocol='workspace'
				and parent_store.identifier='SpacesStore'
				and parent_type_qname.local_name='store_root'
				and parent_namespace.uri='http://www.alfresco.org/model/system/1.0'
			)
		);
	
-- 7) recreate sequence for dummy filenames
DROP SEQUENCE IF EXISTS tmp_non_sensitive_filename_sequence;
CREATE SEQUENCE tmp_non_sensitive_filename_sequence;

-- 8) rename file assocs
update alf_child_assoc
set child_node_name='x' || nextval('tmp_non_sensitive_filename_sequence'), qname_localname='x' || currval('tmp_non_sensitive_filename_sequence')
where child_node_id in (
	select node_id 
	from
	alf_node_properties as props,
	alf_node as node, alf_qname as type_qname,
	alf_qname as prop_qname, 
	alf_namespace as type_namespace, alf_namespace as prop_namespace
	where 
		-- property type is text
		props.string_value is not null and string_value not like '' and props.actual_type_n=6

		and type_namespace.uri='http://www.alfresco.org/model/content/1.0'
		and type_qname.local_name='content'
		and prop_namespace.uri='http://www.alfresco.org/model/content/1.0'
		and prop_qname.local_name in ('name')

		and props.node_id=node.id
		and prop_qname.id=props.qname_id
		and type_qname.id=node.type_qname_id
		and type_namespace.id=type_qname.ns_id
		and prop_namespace.id=prop_qname.ns_id

		and props.node_id not in (
			-- exclude templates
			select distinct node.id from alf_node_properties as props
			left join alf_node as node on node.id=props.node_id
			left join alf_child_assoc as assoc on assoc.child_node_id=node.id
			left join alf_node as parent on parent.id=assoc.parent_node_id
			left join alf_qname as prop_qname on prop_qname.id=props.qname_id
			left join alf_namespace as prop_namespace on prop_namespace.id=prop_qname.ns_id
			where 
			prop_qname.local_name='content'
			and prop_namespace.uri='http://www.alfresco.org/model/content/1.0'
			and parent.id=(
				-- templates root node
				select node_id from alf_node child
				left join alf_node_properties props on props.node_id=child.id
				left join alf_child_assoc assoc on assoc.child_node_id=child.id
				left join alf_node parent on assoc.parent_node_id=parent.id
				left join alf_store parent_store on parent.store_id=parent_store.id
				left join alf_qname parent_type_qname on parent_type_qname.id=parent.type_qname_id
				left join alf_namespace parent_namespace on parent_namespace.id=parent_type_qname.ns_id
				left join alf_qname child_prop_type_qname on child_prop_type_qname.id=props.qname_id
				left join alf_namespace child_prop_namespace on child_prop_namespace.id=child_prop_type_qname.ns_id
				where props.string_value='templates'
					and child_prop_type_qname.local_name='name'
					and child_prop_namespace.uri='http://www.alfresco.org/model/content/1.0'
					and parent_store.protocol='workspace'
					and parent_store.identifier='SpacesStore'
					and parent_type_qname.local_name='store_root'
					and parent_namespace.uri='http://www.alfresco.org/model/system/1.0'
				)
		)
		)
