-- function for correcting namepairs (name -> personName, erase name (if name is filled and personName is empty)
CREATE OR REPLACE FUNCTION correctNamePairs(recipientName_qid bigint, recipientPersonName_qid bigint, upto varchar) RETURNS integer AS $$
DECLARE
    -- document record
    doc RECORD;
    prop RECORD;
    count bigint;
    
BEGIN	
	count = 0;
	-- iterate through documents 
	for doc in 
		select 
			n.*
		from alf_node n, alf_qname q where q.id = n.type_qname_id and q.local_name = 'document' and n.audit_created <= upto
	loop
		-- iterate through name and personName pairs, matched by node_id, qname_id, list_index and locale_id
		for prop in
			select 
				p1.string_value as p1_string_value, p2.string_value as p2_string_value, 
				p1.list_index as list_index, p1.locale_id as locale_id
			from alf_node_properties p1,  alf_node_properties p2
			where 
				p1.node_id = doc.id and p1.qname_id = recipientName_qid and
				p2.node_id = p1.node_id and p2.qname_id = recipientPersonName_qid and
				p1.list_index = p2.list_index and 
				p1.locale_id = p2.locale_id
		loop
		
			-- push name to personName and wipe name if name is filled and personName is empty
			IF 
				prop.p1_string_value IS NOT NULL AND prop.p1_string_value != '' and 
				(prop.p2_string_value is null or prop.p2_string_value = '')
			THEN
				-- log the targets
				-- RAISE NOTICE 'doc.id: %, prop.p1_string_value: %, prop.p2_string_value: %, prop.list_index: %', doc.id, prop.p1_string_value,prop.p2_string_value, prop.list_index;
				count = count + 1;
				
				-- update recipientPersonName to recipientName
				update 
					alf_node_properties set string_value = prop.p1_string_value 
				where 
					node_id = doc.id and qname_id = recipientPersonName_qid and list_index = prop.list_index and locale_id = prop.locale_id;

				-- erase recipientName
				update 
					alf_node_properties set string_value = ''
				where 
					node_id = doc.id and qname_id = recipientName_qid and list_index = prop.list_index and locale_id = prop.locale_id;

					
				IF mod(count, 1000) = 0 THEN
					RAISE NOTICE '% property pairs updated.', count;
				END IF;
			END IF;
		end loop;
	end loop;
	return count;
END;
$$ LANGUAGE plpgsql;

-- function to look up all necessary qnames and call namepairs correction function
CREATE OR REPLACE FUNCTION update20121() RETURNS integer AS $$
DECLARE
    -- namespace
    var_ns_id bigint;
    -- recipients
    name_qid bigint;
    personName_qid bigint;
    upto varchar;
    count bigint;
BEGIN
	count = 0;
	-- set the timestamp (varchar)
	upto = '2012-07-11T16:57:00.000+03:00';
	-- get the namespace id (for qname queries)
	select id into var_ns_id from alf_namespace where uri = 'http://alfresco.webmedia.ee/model/document/dynamic/1.0';
	
	-- get the name qname ids
	select q.id into name_qid from alf_qname q where q.ns_id = var_ns_id and q.local_name = 'recipientName';
	-- get the personName qname id
	select q.id into personName_qid from alf_qname q where q.ns_id = var_ns_id and q.local_name = 'recipientPersonName';
	-- switch the names
	count = count + correctNamePairs( name_qid, personName_qid, upto );
	
	-- switch additionalRecipientNames
	select q.id into name_qid from alf_qname q where q.ns_id = var_ns_id and q.local_name = 'additionalRecipientName';
	select q.id into personName_qid from alf_qname q where q.ns_id = var_ns_id and q.local_name = 'additionalRecipientPersonName';
	count = count + correctNamePairs( name_qid, personName_qid, upto );

	-- switch senderNames
	select q.id into name_qid from alf_qname q where q.ns_id = var_ns_id and q.local_name = 'senderName';
	select q.id into personName_qid from alf_qname q where q.ns_id = var_ns_id and q.local_name = 'senderPersonName';
	count = count + correctNamePairs( name_qid, personName_qid, upto );	

	return count;
END;
$$ LANGUAGE plpgsql;