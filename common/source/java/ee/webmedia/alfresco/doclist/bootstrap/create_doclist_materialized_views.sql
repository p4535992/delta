create materialized view delta_function as
select node.id, concat(protocol, '://', identifier, '/', uuid)  as node_ref_str, concat(mark, ' ', title) as label from alf_node node
join alf_qname qname on node.type_qname_id = qname.id
join alf_namespace ns on qname.ns_id = ns.id
join alf_store store on node.store_id = store.id
left join 
	( select node_id, string_value as title from alf_node_properties props
	  join alf_qname qname on props.qname_id = qname.id and qname.local_name = 'title'
	  join alf_namespace ns on qname.ns_id = ns.id and ns.uri = 'http://alfresco.webmedia.ee/model/functions/1.0'
	) as name_prop on name_prop.node_id = node.id
left join (select node_id, string_value as mark from alf_node_properties props
	  join alf_qname qname on props.qname_id = qname.id and qname.local_name = 'mark'
	  join alf_namespace ns on qname.ns_id = ns.id and ns.uri = 'http://alfresco.webmedia.ee/model/functions/1.0'
	) as mark_prop on mark_prop.node_id = node.id
where qname.local_name = 'function'
and ns.uri = 'http://alfresco.webmedia.ee/model/functions/1.0'
and node.node_deleted=false;

CREATE UNIQUE INDEX delta_function_node_ref_str
  ON delta_function (node_ref_str);
  
create materialized view delta_series as
select node.id, concat(protocol, '://', identifier, '/', uuid)  as node_ref_str, concat(mark, ' ', title) as label from alf_node node
join alf_qname qname on node.type_qname_id = qname.id
join alf_namespace ns on qname.ns_id = ns.id
join alf_store store on node.store_id = store.id
left join 
	( select node_id, string_value as title from alf_node_properties props
	  join alf_qname qname on props.qname_id = qname.id and qname.local_name = 'title'
	  join alf_namespace ns on qname.ns_id = ns.id and ns.uri = 'http://alfresco.webmedia.ee/model/series/1.0'
	) as name_prop on name_prop.node_id = node.id
left join (select node_id, string_value as mark from alf_node_properties props
	  join alf_qname qname on props.qname_id = qname.id and qname.local_name = 'seriesIdentifier'
	  join alf_namespace ns on qname.ns_id = ns.id and ns.uri = 'http://alfresco.webmedia.ee/model/series/1.0'
	) as mark_prop on mark_prop.node_id = node.id
where qname.local_name = 'series'
and ns.uri = 'http://alfresco.webmedia.ee/model/series/1.0'
and node.node_deleted=false;

CREATE UNIQUE INDEX delta_series_node_ref_str
  ON delta_series (node_ref_str);
  
create materialized view delta_volume_casefile as
select node.id, concat(protocol, '://', identifier, '/', uuid)  as node_ref_str, concat(mark, ' ', title) as label from alf_node node
join alf_qname qname on node.type_qname_id = qname.id
join alf_namespace ns on qname.ns_id = ns.id
join alf_store store on node.store_id = store.id
left join 
	( select node_id, string_value as title from alf_node_properties props
	  join alf_qname qname on props.qname_id = qname.id and qname.local_name = 'title'
	  join alf_namespace ns on qname.ns_id = ns.id and ns.uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0'
	) as name_prop on name_prop.node_id = node.id
left join (select node_id, string_value as mark from alf_node_properties props
	  join alf_qname qname on props.qname_id = qname.id and qname.local_name = 'volumeMark'
	  join alf_namespace ns on qname.ns_id = ns.id and ns.uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0'
	) as mark_prop on mark_prop.node_id = node.id
where (qname.local_name = 'volume'
	and ns.uri = 'http://alfresco.webmedia.ee/model/volume/1.0')
or (qname.local_name = 'caseFile'
	and ns.uri = 'http://alfresco.webmedia.ee/model/casefile/1.0')
and node.node_deleted=false;

CREATE UNIQUE INDEX delta_volume_casefile_node_ref_str
  ON delta_volume_casefile (node_ref_str);
  
create materialized view delta_case as
select node.id, concat(protocol, '://', identifier, '/', uuid)  as node_ref_str, title as label from alf_node node
join alf_qname qname on node.type_qname_id = qname.id
join alf_namespace ns on qname.ns_id = ns.id
join alf_store store on node.store_id = store.id
left join 
	( select node_id, string_value as title from alf_node_properties props
	  join alf_qname qname on props.qname_id = qname.id and qname.local_name = 'title'
	  join alf_namespace ns on qname.ns_id = ns.id and ns.uri = 'http://alfresco.webmedia.ee/model/case/1.0'
	) as name_prop on name_prop.node_id = node.id
where (qname.local_name = 'case'
	and ns.uri = 'http://alfresco.webmedia.ee/model/case/1.0')
and node.node_deleted=false;

CREATE UNIQUE INDEX delta_case_node_ref_str
  ON delta_case (node_ref_str);  
  
