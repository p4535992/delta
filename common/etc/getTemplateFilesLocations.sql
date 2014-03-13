-- Juhend skriptide kasutamiseks asub doc/Delta tundlike andmete asendamise skript.docx	
-- select template content 
select content_url from alf_content_url as content_url
left join alf_content_data as content_data on content_data.content_url_id=content_url.id
where content_data.id in (
	-- alf_content_data id-s
	select long_value from alf_node_properties as props
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
