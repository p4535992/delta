--SELECT setval('hibernate_sequence', greatest((select max(id) + 1 from public.alf_node), (select max(id) + 1 from public.alf_transaction), (select max(id) + 1 from public.alf_child_assoc), (select max(id) + 1 from public.alf_node_assoc), (select max(id) + 1 from public.alf_content_data), (select max(id) + 1 from public.alf_content_url)));
-- constants for calculating new node ids for imported nodes
COPY (
	select 'ALF_HIBERNATE_SEQUENCE_NEXTVAL', nextval('hibernate_sequence')
) TO '/delta-pgsql/data/constants.csv';

-- existing users (must be imported with same node id if users exist in this database)
COPY (
	select node.id, props.string_value from 
	(select * from alf_node where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'person' and uri = 'http://www.alfresco.org/model/content/1.0'))) node
	join alf_node_properties props on props.node_id = node.id and props.qname_id in 
		(select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id where local_name = 'userName' and uri = 'http://www.alfresco.org/model/content/1.0')
	
	and node_deleted = false
	and props.string_value is not null
) TO '/delta-pgsql/data/existingUsers.tsv';

-- export existing user favorites, shortcuts, substitutions, saved filters and also general notifications, as existing data must not be overwritten
COPY (
	select id from alf_node node
	where type_qname_id in (select qname.id from alf_qname qname join alf_namespace ns on ns.id = qname.ns_id 
			where (local_name = 'favoriteDirectory' and uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
			or (local_name in ('substitutes', 'substitute') and uri = 'http://alfresco.webmedia.ee/model/substitute/1.0')
			or (local_name = 'generalNotification' and uri = 'http://alfresco.webmedia.ee/model/notification/1.0')
			or (local_name = 'configurations' and uri = 'http://www.alfresco.org/model/application/1.0')
			or (local_name = 'filter' and uri = 'http://alfresco.webmedia.ee/model/document/search/1.0')
			or (local_name = 'logFilter' and uri = 'http://alfresco.webmedia.ee/model/log/1.0')
			or (local_name = 'filter' and uri = 'http://alfresco.webmedia.ee/model/task/search/1.0'))
	and node_deleted = false
) TO '/delta-pgsql/data/existingNotToOverwriteNodes.tsv';
