CREATE OR REPLACE FUNCTION imp_get_string(in_string character varying)
  RETURNS text AS
$BODY$
DECLARE 
	v_vastus TEXT;

BEGIN
 v_vastus := rtrim(ltrim(in_string));
 IF v_vastus = '-' THEN v_vastus := NULL; END IF;
 
		RETURN v_vastus;
END;
$BODY$
  LANGUAGE plpgsql STABLE
  COST 100;