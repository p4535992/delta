-- Skripti jooksutada eelnevalt replaceSensitiveDataWithDummies.sql skripti abil obfuskeeritud andmebaasil.
-- Skript obfuskeerib täiendaalt kasutajagruppide (struktuuriüksuste) andmed
update public.alf_authority set 
	authority =
	concat('GROUP_', case when position(',' in authority) < 1 then 'obfuskeeritud' 
		else repeat('obfuskeeri, tud', array_length(regexp_split_to_array(authority, '(,)'), 1)) end, id ),
	crc = crc32(concat('GROUP_', case when position(',' in authority) < 1 then 'obfuskeeritud' 
		else repeat('obfuskeeri, tud', array_length(regexp_split_to_array(authority, '(,)'), 1)) end, id ))
	where authority like 'GROUP_%' and authority not in ('GROUP_ALFRESCO_ADMINISTRATORS', 'GROUP_ARCHIVISTS', 'GROUP_DOCUMENT_MANAGERS', 'GROUP_EVERYONE', 'GROUP_SUPERVISION');

UPDATE alf_child_assoc SET qname_localname = 
	concat('GROUP_', case when position(',' in qname_localname) < 1 then 'obfuskeeritud' 
		else repeat('obfuskeeri, tud', array_length(regexp_split_to_array(qname_localname, '(,)'), 1)) end, id ) where qname_localname like 'GROUP_%' 
	and qname_localname not in ('GROUP_ALFRESCO_ADMINISTRATORS', 'GROUP_ARCHIVISTS', 'GROUP_DOCUMENT_MANAGERS', 'GROUP_EVERYONE', 'GROUP_SUPERVISION', 'GROUP_ACCOUNTANTS');

UPDATE alf_node_properties props
SET string_value = 
	case when string_value like 'GROUP_%' then
		concat('GROUP_', case when position(',' in string_value) < 1 then 'obfuskeeritud' 
		else repeat('obfuskeeri, tud', array_length(regexp_split_to_array(string_value, '(,)'), 1)) end, props.node_id )
	else 
		concat(case when position(',' in string_value) < 1 then 'obfuskeeritud' 
		else repeat('obfuskeeri, tud', array_length(regexp_split_to_array(string_value, '(,)'), 1)) end, props.node_id )
	end
from alf_qname qname, alf_namespace ns
where
 qname.id = props.qname_id
 and ns.id = qname.ns_id
 and
 ((local_name = 'authorityDisplayName' and uri = 'http://www.alfresco.org/model/content/1.0')
	or
	(local_name = 'authorityName' and uri = 'http://www.alfresco.org/model/content/1.0')
	or
	(local_name = 'name' and uri = 'http://alfresco.webmedia.ee/model/orgstructure/1.0')
	or
	(local_name = 'organizationPath' and uri = 'http://alfresco.webmedia.ee/model/orgstructure/1.0'));
