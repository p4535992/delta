CREATE OR REPLACE FUNCTION imp_insert_failed(in_procedure_id integer, in_started_date_time timestamp with time zone, in_finished_date_time timestamp with time zone, in_task_user character varying, in_error_code character varying, in_error_msg character varying DEFAULT NULL::character varying)
  RETURNS void AS
$BODY$
DECLARE 
BEGIN

INSERT INTO imp_failed_procedures(
            procedure_id, started_date_time, finished_date_time, task_user, error_code, error_desc)
VALUES (in_procedure_id, in_started_date_time, in_finished_date_time, in_task_user, in_error_code, 
(SELECT error_desc FROM imp_error_code WHERE error_code = in_error_code) || CASE WHEN in_error_msg IS NOT NULL THEN ' [' || in_error_msg || ']' ELSE '' END);
        
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;