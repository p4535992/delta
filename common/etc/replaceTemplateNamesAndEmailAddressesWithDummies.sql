-- Skripti jooksutada eelnevalt replaceSensitiveDataWithDummies.sql skripti abil obfuskeeritud andmebaasil.
-- Skript obfuskeerib täiendavalt mallide nimed ja meiliaadressid 

-- Mallide nimede täiendav obfuskeerimine alf_child_assoc tabelis:
UPDATE alf_child_assoc
set qname_localname = concat('mall_', child_node_id, '.bin'),
	child_node_name = concat('mall_', child_node_id, '.bin'),
	child_node_name_crc = crc32(concat('mall_', child_node_id, '.bin')) WHERE child_node_id in (select id from tmp_content_node_ids) and qname_localname not like 'fail_%'

-- Mallide nimede täiendav obfuskeerimine alf_node_properties tabelis:
UPDATE alf_node_properties props
set string_value = concat('fail_', props.node_id, (case when position('.' in string_value) > 0 
							then substring(string_value from (length(string_value) - position('.' in reverse(string_value)) + 1) for (length(string_value) - position('.' in reverse(string_value)) + 6)) 
							else '' end) )
from
alf_qname qname, alf_namespace ns
where props.qname_id = qname.id
and ns.id = qname.ns_id
and node_id in (select id from tmp_content_node_ids where nimi_obfuskeerida = false)
	and ((local_name = 'name' and uri = 'http://alfresco.webmedia.ee/model/document-template/1.0')
		or (local_name = 'displayName' and uri = 'http://alfresco.webmedia.ee/model/file/1.0')
		or (local_name = 'name' and uri = 'http://www.alfresco.org/model/content/1.0')
		or (local_name = 'fileNameBase' and uri = 'temp'))

-- @politsei.ee meiliaadresse sisaldavate kirjete täiendav obfuskeerimine tabelis alf_child_assoc
UPDATE alf_child_assoc set qname_localname = rpad('obfuskeeritud', length(qname_localname), ' obfusk') where lower(qname_localname) like '%@olitsei.ee%';

UPDATE alf_child_assoc set child_node_name = rpad('obfuskeeritud', length(child_node_name), ' obfusk'),
	child_node_name_crc = crc32(rpad('obfuskeeritud', length(child_node_name), ' obfusk')) where lower(child_node_name) like '%@olitsei.ee%';

-- @politsei.ee meiliaadresse sisaldavate kirjete täiendav obfuskeerimine tabelis alf_node_properties

UPDATE alf_node_properties set string_value = rpad('obf@obf.obf', length(string_value), ' obfusk') where string_value like '%@politsei.ee%';

-- Dokumentide juurde salvestatud mallide nimed, tabel alf_node_properties tabel:
UPDATE alf_node_properties set string_value = 
	concat('fail_', props.node_id, (case when position('.' in string_value) > 0 
		then substring(string_value from (length(string_value) - position('.' in reverse(string_value)) + 1) for (length(string_value) - position('.' in reverse(string_value)) + 6)) 
		else '' end) )
from alf_qname qname, alf_namespace ns
where alf_node_properties.qname_id = qname.id and ns.id = qname.ns_id and (local_name = 'templateName' and uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0')

--Aruannete täiendav obfuskeerimine (isikukoodid ja mallide nimed), tabel alf_node_properties:
UPDATE alf_node_properties set string_value = 
	 rpad('obfuskeeritud', length(string_value), ' obfusk') from alf_qname qname, alf_namespace ns where alf_node_properties.qname_id = qname.id and ns.id = qname.ns_id and ((local_name = 'reportName' and uri = 'http://alfresco.webmedia.ee/model/report/1.0,reportResult')
or (local_name = 'userName' and uri = 'http://alfresco.webmedia.ee/model/report/1.0,reportResult'))

update alf_child_assoc set qname_localname = 'obf@obf.obf'
where qname_localname like '%@%';

update alf_child_assoc set child_node_name = 'obf@obf.obf'
where child_node_name like '%@%';

update alf_child_assoc set qname_localname = '10000000001'
where qname_localname SIMILAR TO '\d\d\d\d\d\d\d\d\d\d\d';

update alf_child_assoc set child_node_name = '10000000001'
where child_node_name SIMILAR TO '\d\d\d\d\d\d\d\d\d\d\d';
