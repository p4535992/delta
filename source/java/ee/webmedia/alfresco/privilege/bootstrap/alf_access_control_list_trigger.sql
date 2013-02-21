-- Used to track invalid entries into check_acl_inheritance (see Cl task 209621).
-- When reason for such entries has been recovered and removed, this trigger should be disabled.
-- Currently this trigger has been created manually in some Nortal testing environments.
-- NB! Not recommended in live environments!
-- Author Riina Tens 25.01.2013
-- DROP FUNCTION check_acl_inheritance() CASCADE;
CREATE FUNCTION check_acl_inheritance() RETURNS trigger AS $check_acl_inheritance$
    DECLARE
	parent RECORD;
    BEGIN
         IF NEW.inherits = true AND NEW.inherits_from IS NULL THEN
	    FOR parent IN (SELECT parent_node.acl_id FROM alf_node child_node  
			JOIN alf_child_assoc priParent on priParent.child_node_id = child_node.id and is_primary = true 
			JOIN alf_node parent_node on priParent.parent_node_id = parent_node.id 
			WHERE child_node.acl_id = NEW.id) LOOP
		IF parent.acl_id <> NEW.id THEN
			RAISE EXCEPTION 'Setting inherits_from=null is invalid for acl.id=%', NEW.id;
		END IF;
	    END LOOP;
        END IF;

        RETURN NEW;
    END;
$check_acl_inheritance$ LANGUAGE plpgsql;

CREATE TRIGGER check_acl_inheritance BEFORE INSERT OR UPDATE ON alf_access_control_list
    FOR EACH ROW EXECUTE PROCEDURE check_acl_inheritance();