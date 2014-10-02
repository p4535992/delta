-- Skript obfuskeerib public.alf_authority tabelis kasutajagruppide (struktuuriüksuste) andmed
update public.alf_authority set 
	authority =
	concat('GROUP_', case when position(',' in authority) < 1 then 'obfuskeeritud' 
		else repeat('obfuskeeri, tud', array_length(regexp_split_to_array(authority, '(,)'), 1)) end, id ),
	crc = crc32(concat('GROUP_', case when position(',' in authority) < 1 then 'obfuskeeritud' 
		else repeat('obfuskeeri, tud', array_length(regexp_split_to_array(authority, '(,)'), 1)) end, id ))
	where authority like 'GROUP_%' and authority not in ('GROUP_ALFRESCO_ADMINISTRATORS', 'GROUP_ARCHIVISTS', 'GROUP_DOCUMENT_MANAGERS', 'GROUP_EVERYONE', 'GROUP_SUPERVISION');