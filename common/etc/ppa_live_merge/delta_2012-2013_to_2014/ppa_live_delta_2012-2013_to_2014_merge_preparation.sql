--SELECT setval('hibernate_sequence', greatest((select max(id) + 1 from public.alf_node), (select max(id) + 1 from public.alf_transaction), (select max(id) + 1 from public.alf_child_assoc), (select max(id) + 1 from public.alf_node_assoc), (select max(id) + 1 from public.alf_content_data), (select max(id) + 1 from public.alf_content_url)));
-- constants for calculating new node ids for imported nodes
COPY (
	select 'ALF_HIBERNATE_SEQUENCE_NEXTVAL', nextval('hibernate_sequence')
) TO '/delta-pgsql/data/constants2012-2013to2014.csv';

-- funktsioonid, sarjad, toimikud, teemad, mis on olemas; neid teisest Deltast ei ekspordi (kui teises Deltas on arhiveeritud, siis võib olla olukord, et ei ole kõiki olemas)
COPY (
	select node.id from alf_node node 
	join alf_qname qname on qname.id = node.type_qname_id
	join alf_namespace ns on ns.id = qname.ns_id
	where (local_name = 'function' and uri = 'http://alfresco.webmedia.ee/model/functions/1.0')
	or (local_name = 'series' and uri = 'http://alfresco.webmedia.ee/model/series/1.0')
	or (local_name = 'volume' and uri = 'http://alfresco.webmedia.ee/model/volume/1.0')
	or (local_name = 'case' and uri = 'http://alfresco.webmedia.ee/model/case/1.0')
	or (local_name = 'filter' and uri = 'http://alfresco.webmedia.ee/model/document/search/1.0')
	or (local_name = 'logFilter' and uri = 'http://alfresco.webmedia.ee/model/log/1.0')
	or (local_name = 'filter' and uri = 'http://alfresco.webmedia.ee/model/task/search/1.0')
	or (local_name in ('substitutes', 'substitute') and uri = 'http://alfresco.webmedia.ee/model/substitute/1.0')
	or (local_name = 'generalNotification' and uri = 'http://alfresco.webmedia.ee/model/notification/1.0')
) TO '/delta-pgsql/data/existingNotToOverwriteNodes2012-2013to2014.csv';