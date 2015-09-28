CREATE OR REPLACE FUNCTION fn_get_group_users(usergroup text)  RETURNS SETOF delta_notification_user_log_item AS
$BODY$
DECLARE
	qnIdAuthorityDisplayName bigint;		
	qnIdFisrtName bigint;	
	qnIdLastName bigint;	
	qnIdEmail bigint;	
	qnIdCode bigint;			
	userNodeId bigint;			
	resultItem delta_notification_user_log_item;
BEGIN
	SELECT id INTO qnIdAuthorityDisplayName
	FROM alf_qname WHERE local_name = 'authorityDisplayName';

	SELECT id INTO qnIdFisrtName
	FROM alf_qname WHERE local_name = 'firstName';

	SELECT id INTO qnIdLastName
	FROM alf_qname WHERE local_name = 'lastName';

	SELECT id INTO qnIdEmail
	FROM alf_qname WHERE local_name = 'email';

	SELECT id INTO qnIdCode
	FROM alf_qname WHERE local_name = 'userName';				
	
	FOR userNodeId IN
		SELECT child_node_id
		FROM alf_child_assoc
		WHERE parent_node_id IN (
			SELECT node_id
			FROM alf_node_properties
			WHERE string_value = userGroup AND qname_id = qnIdAuthorityDisplayName 
			)
	LOOP
		SELECT string_value INTO resultItem.first_name
		FROM alf_node_properties WHERE node_id = userNodeId AND qname_id = qnIdFisrtName;
		
		SELECT string_value INTO resultItem.last_name
		FROM alf_node_properties WHERE node_id = userNodeId AND qname_id = qnIdLastName;

		SELECT string_value INTO resultItem.email
		FROM alf_node_properties WHERE node_id = userNodeId AND qname_id = qnIdEmail;		

		SELECT string_value INTO resultItem.id_code
		FROM alf_node_properties WHERE node_id = userNodeId AND qname_id = qnIdCode;				
		

		RETURN NEXT resultItem;
	END LOOP;
			
END
$BODY$
LANGUAGE plpgsql;;

CREATE OR REPLACE FUNCTION fn_log_user_groups_notifications(notificationid bigint, groups text)  RETURNS text AS
$BODY$
DECLARE
	groupsArray text[];
	groupNameHash text[];
	groupItem text;
	notificationGroupId bigint;
	actualUser record;
	loggedUserId bigint;
	loggedUserIds text;
BEGIN
	groupsArray = string_to_array(groups,';-;');

	FOREACH groupItem IN ARRAY groupsArray
	LOOP
		groupNameHash = string_to_array(groupItem,';+;');
		INSERT INTO DELTA_NOTIFICATION_GROUP_LOG(notification_log_id, user_group_name, user_group_hash) 
		VALUES (notificationId, groupNameHash[1], groupNameHash[2])
		RETURNING id INTO notificationGroupId;
		
		loggedUserIds = '';
		FOR actualUser IN 
			SELECT * FROM fn_get_group_users(groupNameHash[1])
		 LOOP
			SELECT id INTO loggedUserId FROM DELTA_NOTIFICATION_USER_LOG 
			WHERE first_name = actualUser.first_name
			AND last_name = actualUser.last_name
			AND email = actualUser.email
			AND id_code = actualUser.id_code;

			IF (loggedUserId is null) THEN
				INSERT INTO DELTA_NOTIFICATION_USER_LOG(first_name, last_name, email, id_code) 
				VALUES (actualUser.first_name, actualUser.last_name, actualUser.email, actualUser.id_code)
				RETURNING id INTO loggedUserId;			
			END IF;

			loggedUserIds = loggedUserIds || ',' || loggedUserId;	
		 END LOOP;

		IF (loggedUserIds != '') THEN
			INSERT INTO DELTA_NOTIFICATION_USER_GROUP_LOG(notification_group_log_id, delta_notification_user_log_ids)
			VALUES(notificationGroupId, substring(loggedUserIds from 2));	
		END IF;			 
	END LOOP;	

RETURN  'DONE';
END
$BODY$
LANGUAGE plpgsql;;
  
  
CREATE OR REPLACE FUNCTION fn_get_log_notificated_users(notificationid bigint,  grouphash character varying)
  RETURNS SETOF delta_notification_user_log_item AS
$BODY$
DECLARE
	userIds text;
	sql text;
BEGIN
	sql = 'SELECT first_name, last_name, email, id_code FROM delta_notification_user_log WHERE id ';
	SELECT ug.delta_notification_user_log_ids INTO userIds
	FROM delta_notification_group_log lg
	INNER JOIN delta_notification_user_group_log ug ON(ug.notification_group_log_id = lg.id)
	WHERE lg.notification_log_id = notificationId AND lg.user_group_hash = groupHash;

	IF (userIds IS NOT NULL) THEN
		sql = sql || 'IN (' || userIds || ') ORDER BY last_name, first_name';
	ELSE
		sql = sql || '= 0 LIMIT 0';
	END IF;

RETURN QUERY EXECUTE sql;
END
$BODY$
LANGUAGE plpgsql;;