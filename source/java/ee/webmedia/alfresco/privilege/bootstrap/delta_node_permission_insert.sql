INSERT INTO delta_node_inheritspermissions (node_uuid, inherits, acl_id) 
	(SELECT node.uuid, 
		CASE WHEN ((qname.local_name = 'series' AND ns.uri = 'http://alfresco.webmedia.ee/model/series/1.0')
			OR (qname.local_name = 'documentType' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
			OR (qname.local_name = 'caseFileType' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')) 
			THEN FALSE ELSE acl.inherits END, 
		acl.id
	FROM alf_node node
	JOIN alf_access_control_list as acl on node.acl_id = acl.id
	JOIN alf_qname as qname on node.type_qname_id = qname.id
	JOIN alf_namespace as ns on qname.ns_id = ns.id
	JOIN (SELECT acl_id FROM alf_acl_member WHERE pos = 0 GROUP BY acl_id) as grouped_member on grouped_member.acl_id = acl.id
	WHERE node.type_qname_id in (
		SELECT qname.id from alf_qname qname JOIN alf_namespace ns on ns.id = qname.ns_id
		where (qname.local_name = 'document' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')
		OR (qname.local_name = 'volume' AND ns.uri = 'http://alfresco.webmedia.ee/model/volume/1.0')
		OR (qname.local_name = 'caseFile' AND ns.uri = 'http://alfresco.webmedia.ee/model/casefile/1.0')
		OR (qname.local_name = 'series' AND ns.uri = 'http://alfresco.webmedia.ee/model/series/1.0')
		OR (qname.local_name = 'documentType' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		OR (qname.local_name = 'caseFileType' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/admin/1.0')
		OR (qname.local_name = 'forum' AND ns.uri = 'http://www.alfresco.org/model/forum/1.0')
	)
	AND EXISTS (SELECT * FROM alf_access_control_entry ace
		JOIN alf_acl_member member on ace.id = member.ace_id 
		WHERE member.pos = 0)
	AND node.acl_id IS NOT NULL);

INSERT INTO delta_node_permission (node_uuid, authority, create_document, 
	view_document_meta_data, edit_document, view_document_files, create_case_file, view_case_file, edit_case_file, participate_at_forum)
	(SELECT node_permission.node_uuid, authority.authority, 
	CASE WHEN privilegeCount.createDocumentCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.viewDocumentMetaDataCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.editDocumentCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.viewDocumentFilesCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.createCaseFileCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.viewCaseFileCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.editCaseFileCount > 0
		THEN TRUE ELSE FALSE
	END,
	CASE WHEN privilegeCount.participateAtForumCount > 0
		THEN TRUE ELSE FALSE
	END
	FROM delta_node_inheritspermissions node_permission
	JOIN (SELECT member.acl_id, entry.authority_id
		FROM alf_acl_member as member 
		JOIN alf_access_control_entry entry on member.ace_id = entry.id
		WHERE (entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'createDocument')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewDocumentMetaData')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocument')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocumentMetaData')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocumentFiles')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewDocumentFiles')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'createCaseFile')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewCaseFile')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editCaseFile')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'participateAtForum')
		OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'DocumentFileRead'))
		AND member.pos = 0
		GROUP BY member.acl_id, entry.authority_id) as grouped_ace on grouped_ace.acl_id = node_permission.acl_id
	JOIN alf_authority authority on authority.id = grouped_ace.authority_id
	LEFT JOIN (SELECT member.acl_id, entry.authority_id, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'createDocument') THEN 1 ELSE 0 END) as createDocumentCount,
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewDocumentMetaData')
						OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'DocumentFileRead') THEN 1 ELSE 0 END) as viewDocumentMetaDataCount, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocument')
						OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocumentMetaData')
						OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editDocumentFiles') THEN 1 ELSE 0 END) as editDocumentCount, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewDocumentFiles')
						OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'DocumentFileRead') THEN 1 ELSE 0 END) as viewDocumentFilesCount, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'createCaseFile') THEN 1 ELSE 0 END) as createCaseFileCount, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'viewCaseFile') THEN 1 ELSE 0 END) as viewCaseFileCount, 
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'editCaseFile') THEN 1 ELSE 0 END) as editCaseFileCount,
		sum(CASE WHEN entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'participateAtForum') 
						OR entry.permission_id = (SELECT id FROM alf_permission permission WHERE permission.name = 'DocumentFileRead') THEN 1 ELSE 0 END) as participateAtForumCount
		FROM alf_acl_member as member 
		JOIN alf_access_control_entry entry on member.ace_id = entry.id
		GROUP BY member.acl_id, entry.authority_id) 
		AS privilegeCount on (privilegeCount.acl_id = grouped_ace.acl_id AND privilegeCount.authority_id = grouped_ace.authority_id)
	);
	
	
CREATE TABLE tmp_permission_node
(
  node_uuid character varying(36) NOT NULL
)
WITH (
  OIDS=FALSE
);

INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_qname qname on qname.id = node.type_qname_id
	JOIN alf_namespace ns on ns.id = qname.ns_id
	WHERE qname.local_name = 'imapFolder' AND ns.uri = 'http://alfresco.webmedia.ee/model/imap/1.0');
	
INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	WHERE child_assoc.qname_localname = 'dvkReceivedCorruptDocuments');
	
INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	WHERE child_assoc.qname_localname = 'scannedDocs');	
	
INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	WHERE child_assoc.qname_localname = 'dvkReceived');	
	
INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	WHERE child_assoc.qname_localname = 'webServiceReceived');		

INSERT INTO delta_node_inheritspermissions (node_uuid, inherits, acl_id) 
	(SELECT node.uuid, FALSE, node.acl_id
	FROM alf_node node
	JOIN tmp_permission_node tmp on tmp.node_uuid = node.uuid
	WHERE node.acl_id IS NOT NULL);
	
INSERT INTO delta_node_permission (node_uuid, authority,  
	view_document_meta_data, edit_document, view_document_files)
	(SELECT node_permission.node_uuid, 'GROUP_DOCUMENT_MANAGERS', TRUE, TRUE, TRUE
	FROM tmp_permission_node tmp
	JOIN delta_node_inheritspermissions node_permission on tmp.node_uuid = node_permission.node_uuid);
	
DELETE FROM tmp_permission_node;

INSERT INTO tmp_permission_node
	(SELECT node.uuid FROM alf_node node
	JOIN alf_child_assoc child_assoc on child_assoc.child_node_id = node.id
	WHERE child_assoc.qname_localname = 'drafts'
	AND node.type_qname_id = 
		(SELECT qname.id from alf_qname qname JOIN alf_namespace ns on ns.id = qname.ns_id
		where (qname.local_name = 'drafts' AND ns.uri = 'http://alfresco.webmedia.ee/model/document/common/1.0')));
		
INSERT INTO delta_node_inheritspermissions (node_uuid, inherits, acl_id) 
	(SELECT node.uuid, FALSE, node.acl_id
	FROM alf_node node
	JOIN tmp_permission_node tmp on tmp.node_uuid = node.uuid
	WHERE node.acl_id IS NOT NULL);		
	
INSERT INTO delta_node_permission (node_uuid, authority,  
	view_document_meta_data, edit_document, view_document_files)
	(SELECT node_permission.node_uuid, 'GROUP_EVERYONE', TRUE, TRUE, TRUE
	FROM tmp_permission_node tmp
	JOIN delta_node_inheritspermissions node_permission on tmp.node_uuid = node_permission.node_uuid);	

DROP TABLE tmp_permission_node;	
	
-- FIXME: comment back in when this column is not needed for verification any more
--ALTER TABLE delta_node_inheritspermissions DROP COLUMN acl_id; 
	
	