DROP TABLE imp_d_menetlus;
DROP TABLE imp_d_dok_men;
DROP TABLE imp_d_isik_men;
DROP TABLE imp_d_kommentaar;
DROP TABLE imp_d_lisavaljavaartus;
DROP TABLE imp_resolutsioonid;
DROP TABLE imp_d_kasut_ostr;
DROP TABLE imp_d_kasut_adsi;
DROP TABLE imp_completed_docs;
DROP TABLE imp_delta_users;
DROP TABLE imp_casefile;
DROP TABLE imp_compound_workflow;
DROP TABLE imp_workflow;
DROP TABLE imp_task;
DROP TABLE imp_association; 
DROP TABLE imp_completed_procedures;
DROP TABLE imp_failed_procedures;
DROP TABLE imp_error_code;
DROP TABLE imp_task1;
DROP TABLE imp_task2;
DROP TABLE imp_task3;

--Functions
DROP FUNCTION imp_create_indexs();
DROP FUNCTION imp_get_comment(in_id integer, in_menetluseliik character varying, in_tahtaegkp timestamp with time zone, in_prioriteet character varying, in_labibkantselei boolean, in_trykk boolean);
DROP FUNCTION imp_get_string(in_string character varying);
DROP FUNCTION imp_insert_failed(in_procedure_id integer, in_started_date_time timestamp with time zone, in_finished_date_time timestamp with time zone, in_task_user character varying, in_error_code character varying, in_error_msg character varying);
DROP FUNCTION imp_proc(in_default_owner_id character varying, in_number_of_casefile bigint);