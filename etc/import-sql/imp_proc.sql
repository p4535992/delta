-- Function: imp_proc(character varying, bigint)

-- DROP FUNCTION imp_proc(character varying, bigint);

CREATE OR REPLACE FUNCTION imp_proc(in_default_owner_id character varying, in_number_of_casefile bigint DEFAULT 1)
  RETURNS void AS
$BODY$
DECLARE 
v_vastus BOOLEAN;
v_rec RECORD;
v_task_rec RECORD;
v_temp_task_rec RECORD;
v_error_code character varying(50);
v_task_user character varying(100);
v_owner_name character varying(100);
v_kas_leidub BOOLEAN;
v_task_owner character varying(100);
v_job_title imp_delta_users.job_title%TYPE;
v_org_name imp_delta_users.org_name%TYPE;
v_task_creator character varying(100);
v_task_started_date_time timestamp with time zone;
v_task_completed_date_time timestamp with time zone;
v_task_status character varying(50);
v_task_outcome text;
v_task_comment text;
v_task_username imp_delta_users.username%TYPE;
v_task_email imp_delta_users.email%TYPE;
v_status character varying(50);
v_finished_date_time timestamp with time zone;
v_username imp_delta_users.username%TYPE;
v_email imp_delta_users.email%TYPE;
v_comment text;
v_volume_mark imp_casefile.volume_mark%TYPE;
v_keyword_level1 imp_casefile.keyword_level1%TYPE;
v_default_name character varying(100);
v_default_email imp_delta_users.email%TYPE;
v_default_job_title imp_delta_users.job_title%TYPE;
v_default_org_name imp_delta_users.org_name%TYPE;
--muudatus
v_workflow_order_no imp_workflow.order_no%TYPE;
v_workflow_started imp_workflow.started_date_time%TYPE;
v_workflow_type imp_workflow.type%TYPE;
v_workflow_vaja_lisada integer;
v_workflow_status imp_workflow.status%TYPE;
v_error44_checked integer;

BEGIN

EXECUTE imp_create_indexs();

--parameetrite väärtustamine
SELECT name, email, job_title, org_name 
INTO v_default_name, v_default_email, v_default_job_title, v_default_org_name
FROM imp_delta_users
WHERE username = in_default_owner_id;

CREATE TEMPORARY TABLE temp_lisavaljavaartus
(
  menetluseliik character varying(50),
  valjanimi character varying(50),
  combo_vaartus text,
  string_vaartus text,
  date_vaartus date,
  int_vaartus integer,
  is_selected boolean
) ON COMMIT DROP;

FOR v_rec IN 
	--vaadata ainult neid, mida ei ole varem imporditud
	SELECT id, olek, menetluseliik, alguskp, loppkp, algataja_id, tahtaegkp, kirjeldus, prioriteet, labibkantselei, trykk, menetlusy_id, menetlusa_id 
	FROM imp_d_menetlus

	LOOP
	BEGIN
		--vigade töötlus
		v_error_code := NULL;
		v_task_user := NULL;
		
		IF v_rec.olek = 'Katkestatud' THEN
			v_error_code := 'ERROR_1';
		ELSIF v_rec.olek NOT IN ('Katkestatud', 'Kinnitatud', 'Publitseeritud', 'Kinnitusrajal', 'Täitmine') OR v_rec.menetluseliik IS NULL THEN
			v_error_code := 'ERROR_2';
		ELSIF v_rec.alguskp IS NULL THEN
			v_error_code := 'ERROR_3';
		ELSIF v_rec.olek NOT IN ('Kinnitatud', 'Publitseeritud') AND v_rec.loppkp IS NOT NULL THEN 
			v_error_code := 'ERROR_4';
		ELSIF v_rec.algataja_id IS NULL THEN 
			v_error_code := 'ERROR_6';	
		ELSE
		--kas leidub responsible ?
		BEGIN
			SELECT TRUE INTO STRICT v_kas_leidub 
			FROM imp_d_isik_men 
			WHERE menetlus_id = v_rec.id AND liik = 'T' AND ylem_id IS NULL AND staatus = 'Põhitäitja';
		EXCEPTION
			WHEN no_data_found THEN
				v_error_code := 'ERROR_7';
				RAISE EXCEPTION '';
			WHEN too_many_rows THEN
				v_error_code := 'ERROR_7';
				RAISE EXCEPTION '';
		END;
		--pärida owner_name
		BEGIN
			 SELECT ka.displayname INTO STRICT v_owner_name
			 FROM imp_d_isik_men m
			 INNER JOIN imp_d_kasut_ostr ko ON ko.d_org_strukt_id = m.d_org_strukt_id
			 INNER JOIN imp_d_kasut_adsi ka ON ka.samaccountname = ko.kasutaja
			 WHERE m.id = v_rec.algataja_id AND m.loomisekp BETWEEN ko.alguskp AND COALESCE(ko.loppkp, CURRENT_TIMESTAMP);
		EXCEPTION
			WHEN no_data_found THEN 
				v_error_code := 'ERROR_6';
				RAISE EXCEPTION '';
			WHEN too_many_rows THEN
				v_error_code := 'ERROR_5';
				RAISE EXCEPTION '';
		END;
		END IF;

		IF v_error_code IS NOT NULL THEN 
			RAISE EXCEPTION '';
		END IF;

		--parameetrite väärtustamine
		v_status := CASE WHEN v_rec.olek IN ('Kinnitatud', 'Publitseeritud') THEN 'lõpetatud' ELSE 'teostamisel' END;
		v_finished_date_time := CASE WHEN v_rec.loppkp IS NULL AND v_rec.olek IN ('Kinnitatud', 'Publitseeritud') THEN current_timestamp ELSE v_rec.loppkp END;
		v_workflow_order_no := 0;
		v_workflow_status := NULL;
		v_error44_checked := 0;
		
		--TASK
		FOR v_task_rec IN SELECT
			m.id,
			m.liik,
			row_number() OVER (PARTITION BY m.liik ORDER BY m.alguskp, m.loomisekp) AS order_no,
			CASE WHEN m.liik = 'T' THEN  'assignment_task' ELSE 'confirmation_task' END AS type,
			CASE WHEN m.liik = 'T' AND m.ylem_id IS NULL AND m.staatus = 'Põhitäitja' THEN TRUE ELSE FALSE END AS responsible,
			m.d_org_strukt_id,
			m.looja,
			(SELECT array_to_string(ARRAY(SELECT r.resolutsioon FROM imp_resolutsioonid r WHERE r.isik_men_id = m.id AND r.resolutsioon IS NOT NULL AND r.resolutsioon <> ''), ' ')) AS resolution,
			CASE WHEN m.alguskp IS NULL AND m.loppkp IS NOT NULL THEN v_rec.alguskp ELSE m.alguskp END started_date_time,
			COALESCE(v_rec.tahtaegkp, TO_TIMESTAMP('31.12.9999', 'DD.MM.YYYY')) AS due_date,
			m.loppkp AS completed_date_time,
			CASE 
				WHEN m.loppkp IS NOT NULL THEN 'lõpetatud'
				WHEN m.alguskp IS NULL THEN 'uus'
				ELSE 'teostamisel'
			END AS status,
			CASE
				WHEN m.loppkp IS NOT NULL AND m.liik = 'T' THEN 'Tööülesanne täidetud'
				WHEN m.loppkp IS NOT NULL AND m.liik = 'K' AND m.kinnitatud IN ('Delegeeris edasi', 'Suunas edasi', 'Suunas tagasi', 'Ei kinnitanud') THEN 'Kinnitamata'
				WHEN m.loppkp IS NOT NULL AND m.liik = 'K' AND (m.kinnitatud IN ('Kinnitas', 'Täitis') OR m.kinnitatud IS NULL) THEN 'Kinnitatud'
			END AS outcome,
			CASE WHEN m.kinnitatud = 'NULL' THEN NULL ELSE m.kinnitatud END AS comment,
			m.loppkp,
			m.loomisekp,
			m.ylem_id,
			m.staatus 
			FROM imp_d_isik_men m WHERE menetlus_id = v_rec.id
			ORDER BY m.alguskp, m.loomisekp, m.id
		LOOP
		
		--vigade töötlus
		
		IF v_task_rec.liik IS NULL OR v_task_rec.liik NOT IN ('K', 'T') THEN
			v_error_code := 'ERROR_13';
		ELSE
		BEGIN
			SELECT ka.displayname INTO STRICT v_task_owner
			FROM imp_d_kasut_adsi ka
			INNER JOIN imp_d_kasut_ostr ko ON ka.samaccountname = ko.kasutaja
			WHERE ko.d_org_strukt_id = v_task_rec.d_org_strukt_id AND v_task_rec.loomisekp BETWEEN ko.alguskp AND COALESCE(ko.loppkp, CURRENT_TIMESTAMP);
		EXCEPTION
			WHEN no_data_found THEN 
				v_error_code := 'ERROR_9';
				RAISE EXCEPTION '';
			WHEN too_many_rows THEN
				v_error_code := 'ERROR_8';
				RAISE EXCEPTION '';
		END;
		IF v_task_rec.looja = 'Menetlussiire' THEN 
			v_task_creator := v_owner_name;
		ELSE
			BEGIN
				SELECT displayname INTO STRICT v_task_creator
				FROM imp_d_kasut_adsi
				WHERE samaccountname = v_task_rec.looja;
			EXCEPTION
				WHEN no_data_found THEN 
				v_error_code := 'ERROR_11';
				RAISE EXCEPTION '';
			WHEN too_many_rows THEN
				v_error_code := 'ERROR_10';
				RAISE EXCEPTION '';
			END;
		END IF;
		END IF;
		

		IF v_error_code IS NOT NULL THEN 
			RAISE EXCEPTION '';
		END IF;

		--parameetrite väärtustamine
		v_task_started_date_time := v_task_rec.started_date_time;
		v_task_completed_date_time := v_task_rec.completed_date_time;
		v_task_status := v_task_rec.status;
		v_task_outcome := v_task_rec.outcome;
		v_task_comment := v_task_rec.comment;

		IF v_workflow_order_no = 0 OR v_workflow_type <> v_task_rec.type THEN
			--v_workflow_started := v_task_rec.started_date_time;
			v_workflow_type := v_task_rec.type;
			v_workflow_order_no := v_workflow_order_no + 1;
		END IF;

		--lisa vigade töötlus
		IF v_workflow_order_no > 2 AND v_error44_checked = 0 THEN
			IF EXISTS (SELECT 1 FROM (SELECT liik, loppkp FROM imp_d_isik_men WHERE menetlus_id = v_rec.id ORDER BY alguskp, loomisekp, id DESC LIMIT 1) m WHERE liik = 'K' AND loppkp IS NOT NULL) THEN
				v_error_code := 'ERROR_44';
				RAISE EXCEPTION '';
			END IF;
			v_error44_checked := 1;
		END IF;
		
		--korrigeerimine
		IF v_task_status IN ('uus', 'teostamisel') AND v_task_rec.comment IS NOT NULL THEN
			v_task_status := 'lõpetatud';
			v_task_completed_date_time := v_finished_date_time;
			v_task_started_date_time := COALESCE(v_task_started_date_time, v_rec.alguskp);
		END IF;
		IF v_task_status IN ('uus', 'teostamisel') AND v_status = 'lõpetatud' THEN
			v_task_status := 'teostamata';
			v_task_completed_date_time := v_finished_date_time;
			v_task_outcome := CASE WHEN v_task_rec.type = 'confirmation_task' THEN 'Kinnitamata' ELSE 'Tööülesanne täidetud' END;
			v_task_comment := 'Märgitud katkestatuks menetluse migreerimise käigus';
			v_task_started_date_time := COALESCE(v_task_started_date_time, v_rec.alguskp);
		END IF;
		--kui ülemtask on lõpetatud, siis lõpetatakse automaatlselt alamtask
		IF v_task_rec.staatus = 'Kaastäitja' AND v_task_rec.loppkp IS NULL AND EXISTS (SELECT 1 FROM imp_task1 WHERE old_id = v_task_rec.ylem_id AND status IN ('lõpetatud', 'teostamata')) THEN
			v_task_status := 'teostamata';
			v_task_completed_date_time = (SELECT completed_date_time FROM imp_task1 WHERE old_id = v_task_rec.ylem_id);
			v_task_outcome := CASE WHEN v_task_rec.type = 'confirmation_task' THEN 'Kinnitamata' ELSE 'Tööülesanne täidetud' END;
			v_task_comment := 'Tööülesanne on lõpetatud automaatselt peatäitja tööülesande täitmise hetkel';
			v_task_started_date_time := COALESCE(v_task_started_date_time, v_rec.alguskp);
		END IF;
		
		--insert IMP_TASK1
		INSERT INTO imp_task1(
			procedure_id, order_no, type, responsible, owner_name, creator_name, 
			resolution, started_date_time, due_date, completed_date_time, 
			status, outcome, comment, old_id, old_parent_id, workflow_order)
		VALUES (
			v_rec.id, v_task_rec.order_no, v_task_rec.type, v_task_rec.responsible, v_task_owner, v_task_creator, 
			v_task_rec.resolution, v_task_started_date_time, v_task_rec.due_date, v_task_completed_date_time, 
			v_task_status, v_task_outcome, v_task_comment, v_task_rec.id, v_task_rec.ylem_id, v_workflow_order_no);
			
		END LOOP;
		
		--lisa TASK korrigeerimine
		IF EXISTS (SELECT 1 FROM imp_task1 WHERE procedure_id = v_rec.id AND type = 'assignment_task' AND status <> 'uus') THEN
			UPDATE imp_task1
			SET (status, started_date_time) = ('teostamisel', current_timestamp)
			WHERE procedure_id = v_rec.id AND type = 'assignment_task' AND status = 'uus'
			AND workflow_order IN (
			SELECT workflow_order
			FROM imp_task1 
			WHERE procedure_id = v_rec.id AND type = 'assignment_task' AND status <> 'uus');
		END IF;

		--OWNER mässamine: TASK
		FOR v_temp_task_rec IN SELECT old_id, status, owner_name, type FROM imp_task1 WHERE procedure_id = v_rec.id
		LOOP
			v_task_username := NULL;
			v_task_email := NULL;
			v_job_title := NULL;
			v_org_name := NULL;
			BEGIN
				SELECT username, email, job_title, org_name INTO STRICT v_task_username, v_task_email, v_job_title, v_org_name
				FROM imp_delta_users
				WHERE name = v_temp_task_rec.owner_name;

				IF v_task_email IS NULL OR v_task_email = '' THEN
					v_error_code := 'ERROR_41';
					RAISE EXCEPTION '';
				ELSE
					INSERT INTO imp_task2(procedure_id, owner_id, owner_email, old_id, owner_job_title, owner_org_name)
					VALUES (v_rec.id, v_task_username, v_task_email, v_temp_task_rec.old_id, v_job_title, v_org_name);
				END IF;
			EXCEPTION
				WHEN no_data_found THEN NULL;
				WHEN too_many_rows THEN NULL;
			END;
			
			IF v_task_username IS NULL OR v_task_username = '' THEN
				IF v_temp_task_rec.status IN ('lõpetatud', 'teostamata') THEN
				
					INSERT INTO imp_task2(procedure_id, owner_id, owner_email, old_id)
					VALUES (v_rec.id, '99999999999', null, v_temp_task_rec.old_id);
					
				ELSIF v_temp_task_rec.status = 'uus' AND (v_default_email IS NULL OR v_default_email = '') THEN
					v_error_code := 'ERROR_40';
					RAISE EXCEPTION '';
				ELSIF v_temp_task_rec.status = 'uus' THEN
				
					INSERT INTO imp_task2(procedure_id, owner_id, owner_email, old_id, owner_job_title, owner_org_name)
					VALUES (v_rec.id, in_default_owner_id, v_default_email, v_temp_task_rec.old_id, v_default_job_title, v_default_org_name);
					
					UPDATE imp_task1
						SET owner_name = v_default_name
					WHERE old_id = v_temp_task_rec.old_id;
					
				ELSIF v_temp_task_rec.status = 'teostamisel' THEN
					INSERT INTO imp_task2(procedure_id, owner_id, owner_email, old_id)
					VALUES (v_rec.id, '99999999999', null, v_temp_task_rec.old_id);
					
					UPDATE imp_task1
						SET (status, completed_date_time, comment, outcome) = (
							'teostamata', 
							current_timestamp, 
							'Märgitud katkestatuks menetluse migreerimise käigus',
							CASE WHEN v_temp_task_rec.type = 'confirmation_task' THEN 'Kinnitamata' ELSE 'Tööülesanne täidetud' END
							)
					WHERE old_id = v_temp_task_rec.old_id;
					
				END IF;
			
			END IF;
			
		END LOOP;

		--COMPOUND_WORKFLOW korrigeerimine
		IF v_status = 'teostamisel' AND NOT EXISTS (SELECT 1 FROM imp_task1 WHERE procedure_id = v_rec.id AND status NOT IN ('lõpetatud', 'teostamata')) THEN
			v_status := 'lõpetatud';
			v_finished_date_time := (SELECT MAX(completed_date_time) FROM imp_task1 WHERE procedure_id = v_rec.id);
		END IF;
			
		--COMPOUND_WORKFLOW korrigeerimine
		v_comment := imp_get_comment(v_rec.id, v_rec.menetluseliik, v_rec.tahtaegkp, v_rec.prioriteet, v_rec.labibkantselei, v_rec.trykk);
		IF v_comment IN ('ERROR_15', 'ERROR_16') THEN
			v_error_code := v_comment;
			RAISE EXCEPTION '';
		END IF;

		--OWNER mässamine: COMPOUND_WORKFLOW
		v_username := NULL;
		v_email := NULL;
		
		BEGIN
			SELECT username, email INTO STRICT v_username, v_email
				FROM imp_delta_users
				WHERE name = v_owner_name;

				IF v_email IS NULL OR v_email = '' THEN
					v_error_code := 'ERROR_39';
					RAISE EXCEPTION '';
				END IF;
		EXCEPTION
			WHEN no_data_found THEN NULL;
			WHEN too_many_rows THEN NULL;
		END;

		IF v_username IS NULL OR v_username = '' THEN
				IF v_status = 'lõpetatud' THEN
					v_username := '99999999999';
				ELSIF v_status = 'teostamisel' AND (v_default_email IS NULL OR v_default_email = '') THEN
					v_error_code := 'ERROR_40';
					RAISE EXCEPTION '';
				ELSIF v_status = 'teostamisel' THEN
					v_username := in_default_owner_id;
					v_email := v_default_email;
					v_comment := (CASE WHEN v_comment IS NULL THEN 'Terviktöövoo algne vastutaja: ' || v_owner_name || ' enne käesolevas punktis teostatud samme' 
					ELSE v_comment || CHR(10) || 'Terviktöövoo algne vastutaja: ' || v_owner_name || ' enne käesolevas punktis teostatud samme'  END);
					v_owner_name := v_default_name;
				END IF;
		END IF;

		--CREATOR mässamine: TASK
		FOR v_temp_task_rec IN SELECT old_id, status, creator_name, type FROM imp_task1 WHERE procedure_id = v_rec.id
		LOOP
			v_task_username := NULL;
			v_task_email := NULL;
			BEGIN
				SELECT username, email INTO STRICT v_task_username, v_task_email
				FROM imp_delta_users
				WHERE name = v_temp_task_rec.creator_name;

				IF v_task_email IS NULL OR v_task_email = '' THEN
					v_error_code := 'ERROR_42';
					RAISE EXCEPTION '';
				ELSE

					INSERT INTO imp_task3(procedure_id, creator_id, creator_email, old_id)
					VALUES (v_rec.id, v_task_username, v_task_email, v_temp_task_rec.old_id);
				END IF;
			EXCEPTION
				WHEN no_data_found THEN NULL;
				WHEN too_many_rows THEN NULL;
			END;
			IF v_task_username IS NULL OR v_task_username = '' THEN
				IF v_temp_task_rec.status IN ('lõpetatud', 'teostamata') THEN
				
					INSERT INTO imp_task3(procedure_id, creator_id, creator_email, old_id)
					VALUES (v_rec.id, '99999999999', NULL, v_temp_task_rec.old_id);
				ELSIF v_temp_task_rec.status = 'teostamisel' THEN
				
					INSERT INTO imp_task3(procedure_id, creator_id, creator_email, old_id)
					VALUES (v_rec.id, v_username, v_email, v_temp_task_rec.old_id);
					
					UPDATE imp_task1
						SET creator_name = v_owner_name
					WHERE old_id = v_temp_task_rec.old_id;
				END IF;
			END IF;
		END LOOP;

		--siia WORKFLOW insert
		--WORKFLOW
		INSERT INTO imp_workflow(
			procedure_id, order_no, type, started_date_time, creator_name, 
			status)
		SELECT 
			procedure_id, order_no, type, started_date_time, creator_name, 
			COALESCE((SELECT t1.status FROM imp_task1 t1 WHERE t1.procedure_id = v_rec.id AND t1.workflow_order = t.order_no AND t1.status = 'teostamisel' LIMIT 1),
			(SELECT t1.status FROM imp_task1 t1 WHERE t1.procedure_id = v_rec.id AND t1.workflow_order = t.order_no AND t1.status = 'uus' LIMIT 1), 'lõpetatud') AS status
		FROM (
		SELECT 
			v_rec.id AS procedure_id,
			workflow_order AS order_no,
			(CASE WHEN type = 'assignment_task' THEN 'assignment_workflow' ELSE 'confirmation_workflow' END) AS type,
			MIN(started_date_time) AS started_date_time, 
			v_owner_name AS creator_name
		FROM imp_task1
		WHERE procedure_id = v_rec.id
		GROUP BY workflow_order, type) t;

		--Kui imp_compound_workflow.status = teostamisel, sisaldab uus, lõpetatud, teostamata taske, kuid ei sisalda ühtegi teostamisel taski, 
		--siis lõpetada compound_workflow ja selles olevad workflow-d ja taskid
		
		IF v_status = 'teostamisel' AND EXISTS (
			SELECT 1
			FROM (
				SELECT
				SUM(uus) AS uus,
				SUM(lopetatud) AS lopetatud,
				SUM(teostamisel) AS teostamisel
				FROM (
					SELECT 
					CASE WHEN status = 'uus' THEN 1 ELSE 0 END AS uus,
					CASE WHEN status = 'lõpetatud' THEN 1 ELSE 0 END AS lopetatud,
					CASE WHEN status = 'teostamisel' THEN 1 ELSE 0 END AS teostamisel
					FROM (
					SELECT workflow_order, (CASE WHEN status = 'teostamata' THEN 'lõpetatud' ELSE status END) AS status 
					FROM imp_task1 WHERE procedure_id = v_rec.id) t) t) t
				WHERE uus > 0 AND lopetatud > 0 AND teostamisel = 0) THEN

		--imp_compound_workflow
		v_status := 'lõpetatud';
		v_finished_date_time := current_timestamp;
		v_comment := 'Menetlusrada ei käinud lõpuni';
		
		--imp_workflow
		UPDATE imp_workflow SET (status, started_date_time) = ('lõpetatud', COALESCE(started_date_time, v_rec.alguskp)) 
		WHERE procedure_id = v_rec.id AND status = 'uus';

		--imp_task1
		UPDATE imp_task1 SET (status, completed_date_time, outcome, comment, started_date_time)
			= ('teostamata', current_timestamp, 'Kinnitamata', 'Märgitud katkestatuks menetluse migreerimise käigus', v_rec.alguskp)
		WHERE procedure_id = v_rec.id AND type = 'confirmation_task' AND status = 'uus';

		UPDATE imp_task1 SET (status, completed_date_time, outcome, comment, started_date_time)
			= ('teostamata', current_timestamp, 'Tööülesanne täidetud', 'Märgitud katkestatuks menetluse migreerimise käigus', v_rec.alguskp)
		WHERE procedure_id = v_rec.id AND type = 'assignment_task' AND status = 'uus';
		
		END IF;
		

		--kontroll: et kas vähemalt 1 task on tesotamisel või siis kõik uus või siis kõik lõpetatud staatusega (ERROR_17)
		--et ei oleks rohkem kui 1 teostamiesl, see kontroll ainult confirmationWorkflow tüüpi workflow-dele (ERROR_12)
		BEGIN 
		SELECT error INTO v_error_code
		FROM (
			SELECT 
			CASE 
			WHEN teostamisel > 1 AND type = 'confirmation_task' THEN 'ERROR_12' 
			WHEN teostamisel = 0 AND uus > 0 AND lopetatud > 0 THEN 'ERROR_17'
			ELSE NULL
			END AS error
			FROM (
				SELECT
				workflow_order,
				type,
				SUM(uus) AS uus,
				SUM(lopetatud) AS lopetatud,
				SUM(teostamisel) AS teostamisel
				FROM (
					SELECT 
					workflow_order,
					type,
					CASE WHEN status = 'uus' THEN 1 ELSE 0 END AS uus,
					CASE WHEN status = 'lõpetatud' THEN 1 ELSE 0 END AS lopetatud,
					CASE WHEN status = 'teostamisel' THEN 1 ELSE 0 END AS teostamisel
					FROM (
					SELECT type, workflow_order, (CASE WHEN status = 'teostamata' THEN 'lõpetatud' ELSE status END) AS status 
					FROM imp_task1 WHERE procedure_id = v_rec.id) t) t
			GROUP BY type, workflow_order) t) t
		WHERE error IS NOT NULL;
		EXCEPTION
			WHEN no_data_found THEN NULL;
		END;

		IF v_error_code IS NOT NULL THEN
			RAISE EXCEPTION '';
		END IF;

		--ERROR_45
		IF v_status = 'teostamisel' THEN
			IF EXISTS (SELECT 1 FROM (
							SELECT
							lead(status) over (order by order_no asc) jrg_status,
							lag(status) over (order by order_no asc) eel_status
							FROM imp_workflow
							WHERE procedure_id = v_rec.id AND status = 'teostamisel'
							) t
				WHERE t.jrg_status IN ('teostamisel', 'lõpetatud') OR t.eel_status IN ('teostamisel', 'uus')) THEN
			v_error_code := 'ERROR_45';
			RAISE EXCEPTION '';
			END IF;
		END IF;
		

		--ANDMETE INSERT
		INSERT INTO imp_compound_workflow(
			procedure_id, type, title, comment, status, owner_name, creator_name, 
			created_date_time, started_date_time, finished_date_time, owner_id, owner_email, parent_id, original_procedure_id)
		VALUES (
			v_rec.id, --procedure_id
			CASE WHEN v_rec.menetluseliik = 'asjamenetlus' THEN 'case_file_workflow' ELSE 'independent_workflow' END, --type
			CASE
			WHEN v_rec.kirjeldus IS NOT NULL AND imp_get_string(v_rec.kirjeldus) <> '' THEN v_rec.kirjeldus || ' menetlus ' || CAST(v_rec.id AS TEXT)
			ELSE v_owner_name || ' menetlus ' || CAST(v_rec.id AS TEXT)
			END, --title, 
			v_comment, --comment, 
			v_status, --status
			v_owner_name,--owner_name, 
			v_owner_name,--creator_name, 
			v_rec.alguskp, --created_date_time, 
			v_rec.alguskp,--started_date_time, 
			v_finished_date_time, --finished_date_time
			v_username,--owner_id,
			v_email,--owner_email
			v_rec.menetlusy_id, 
			v_rec.menetlusa_id
			);

		--ASSOCIATION
		INSERT INTO imp_association(
			procedure_id, from_node, type, creator, created_date_time, main_document)
		SELECT procedure_id, from_node, type, creator, created_date_time,
			CASE WHEN row_no = 1 THEN TRUE ELSE FALSE END as main_document
			FROM (
			SELECT procedure_id, from_node, type, creator, created_date_time , row_number() over (order by d.loomisekp) as row_no
			FROM (
			SELECT DISTINCT procedure_id, from_node, type, creator, created_date_time, loomisekp
		FROM (
		SELECT 
			v_rec.id AS procedure_id, --procedure_id
			CASE 
				WHEN d.liik = 'Seotud dokument' THEN d.node_ref
				WHEN d.liik = 'Seotud menetlus' THEN CAST(d.dokument_id AS character varying)
				WHEN d.liik = 'Seotud url' THEN d.urldokument
			END AS from_node, --from_node
			d.liik AS type,--type
			CASE
				WHEN d.liik = 'Seotud dokument' THEN NULL
				ELSE COALESCE((SELECT a.displayname FROM imp_d_kasut_adsi a WHERE a.samaccountname = d.looja), d.looja)
			END as creator,
			CASE
				WHEN d.liik = 'Seotud dokument' THEN NULL
				ELSE d.loomisekp
			END as created_date_time,
			loomisekp
			
		FROM (
				SELECT 
				liik, 
				urldokument, 
				dokument_id, 
				looja, 
				loomisekp,
				CASE 
					WHEN liik = 'Seotud menetlus' THEN min(loomisekp) over (partition by menetlus_id, liik, dokument_id) 
					ELSE min(loomisekp) over (partition by menetlus_id, liik, urldokument) 
				END AS min_loomisekp,
				menetlus_id,
				node_ref
				FROM imp_d_dok_men2
				WHERE menetlus_id = v_rec.id
			) d
			WHERE d.loomisekp = min_loomisekp) d
		WHERE from_node IS NOT NULL) d) d;
		

		--CASEFILE
		IF v_rec.menetluseliik = 'asjamenetlus' THEN
			TRUNCATE TABLE temp_lisavaljavaartus;
			
			INSERT INTO temp_lisavaljavaartus(valjanimi, combo_vaartus, string_vaartus, date_vaartus, int_vaartus, is_selected)
			SELECT valjanimi, combo_vaartus, string_vaartus, date_vaartus, int_vaartus, is_selected
			FROM imp_d_lisavaljavaartus where menetlus_id = v_rec.id;

			--veatöötlus
			
			--volume_mark
			BEGIN
			SELECT string_vaartus INTO STRICT v_volume_mark FROM temp_lisavaljavaartus WHERE valjanimi = 'Asja number';
			EXCEPTION
				WHEN no_data_found THEN NULL;
					--v_error_code := 'ERROR_18';
					--RAISE EXCEPTION '';
				WHEN too_many_rows THEN NULL;
					--v_error_code := 'ERROR_18';
					--RAISE EXCEPTION '';
			END;

			--ülejäänud väljad
			SELECT 
				CASE
					WHEN valjanimi = 'Menetleja perekonnanimi' THEN 'ERROR_19'
					WHEN valjanimi = 'Avaldaja' THEN 'ERROR_20'
					WHEN valjanimi = 'Avaldaja liik' THEN 'ERROR_21'
					WHEN valjanimi = 'Avaldaja piirkond' THEN 'ERROR_22'
					WHEN valjanimi = 'Avalduse kee' THEN 'ERROR_23'
					WHEN valjanimi = 'Menetluse väljund' THEN 'ERROR_24'
					WHEN valjanimi = 'Korraldatud kontrollkäik' THEN 'ERROR_25'
					WHEN valjanimi = 'OPCAT' THEN 'ERROR_26'
					WHEN valjanimi = 'Märkused' THEN 'ERROR_27'
					WHEN valjanimi = 'Vastustaja' THEN 'ERROR_28'
					WHEN valjanimi = 'Õigusvaldkond' THEN 'ERROR_29'
					WHEN valjanimi = 'Üldine võrdsuspõhiõigus' THEN 'ERROR_30'
					WHEN valjanimi = 'Diskrimineerimine' THEN 'ERROR_31'
					WHEN valjanimi = 'Hea haldus' THEN 'ERROR_32'
					WHEN valjanimi = 'Laste õigused' THEN 'ERROR_33'
					WHEN valjanimi = 'Lapse pöördumine' THEN 'ERROR_34'
					WHEN valjanimi = 'Ülevaatesse' THEN 'ERROR_35'
					WHEN valjanimi = 'Menetluse staatus' THEN 'ERROR_36'
					WHEN valjanimi = 'II tasand KOV' THEN 'ERROR_37'
					WHEN valjanimi = 'II tasand RIIK' THEN 'ERROR_38'
					WHEN valjanimi = 'Võrdne kohtlemine' THEN 'ERROR_43'
					WHEN valjanimi = 'Märksõnad' THEN 'ERROR_46'
				END INTO v_error_code
			FROM (
			SELECT COUNT(*), valjanimi
			FROM temp_lisavaljavaartus 
			GROUP BY valjanimi
			HAVING COUNT(*) > 1) e
			LIMIT 1;

			IF v_error_code IS NOT NULL THEN
				RAISE EXCEPTION '';
			END IF;

			SELECT combo_vaartus INTO v_keyword_level1 FROM temp_lisavaljavaartus WHERE valjanimi = 'Vastustaja';

			INSERT INTO imp_casefile(
				procedure_id, volume_mark, owner_name, title, user_name, valid_from, 
				valid_to, status, contact_name, applicant_type, applicant_area, 
				application_language, case_result, supervision_visit, opcat, 
				comment, keyword_level1, keyword_level2, legislation, general_right_to_equality, 
				discrimination, good_administration, child_rights, child_applicant, 
				to_survey, procedure_status, equality_of_treatment, workflow_due_date, keywords_string)
			SELECT
				v_rec.id AS procedure_id, 
				v_volume_mark AS volume_mark, 
				v_owner_name AS owner_name, 
				v_rec.kirjeldus AS title, 
				(SELECT string_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'Menetleja perekonnanimi') AS user_name, 
				v_rec.alguskp AS valid_from, 
				v_finished_date_time AS valid_to, 
				CASE WHEN v_finished_date_time IS NULL THEN 'avatud' ELSE 'suletud' END AS status, 
				(SELECT string_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'Avaldaja') AS contact_name, 
				(SELECT combo_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'Avaldaja liik') AS applicant_type, 
				(SELECT combo_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'Avaldaja piirkond') AS applicant_area, 
				(SELECT combo_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'Avalduse keel') AS application_language, 
				(SELECT combo_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'Menetluse väljund') AS case_result, 
				(SELECT is_selected FROM temp_lisavaljavaartus WHERE valjanimi = 'Korraldatud kontrollkäik') AS supervision_visit, 
				(SELECT is_selected FROM temp_lisavaljavaartus WHERE valjanimi = 'OPCAT') AS opcat, 
				(SELECT string_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'Märkused') AS comment, 
				v_keyword_level1 AS keyword_level1, 
				CASE 
					WHEN v_keyword_level1 = 'KOV' THEN (SELECT combo_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'II tasand KOV')
					WHEN v_keyword_level1 = 'Riik' THEN (SELECT combo_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'II tasand RIIK')
				END AS keyword_level2, 
				(SELECT combo_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'Õigusvaldkond') AS legislation, 
				(SELECT is_selected FROM temp_lisavaljavaartus WHERE valjanimi = 'Üldine võrdsuspõhiõigus') AS general_right_to_equality, 
				(SELECT is_selected FROM temp_lisavaljavaartus WHERE valjanimi = 'Diskrimineerimine') AS discrimination, 
				(SELECT is_selected FROM temp_lisavaljavaartus WHERE valjanimi = 'Hea haldus') AS good_administration, 
				(SELECT is_selected FROM temp_lisavaljavaartus WHERE valjanimi = 'Laste õigused') AS child_rights, 
				(SELECT is_selected FROM temp_lisavaljavaartus WHERE valjanimi = 'Lapse pöördumine') AS child_applicant, 
				(SELECT is_selected FROM temp_lisavaljavaartus WHERE valjanimi = 'Ülevaatesse') AS to_survey, 
				(SELECT combo_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'Menetluse staatus') AS procedure_status,
				(SELECT is_selected FROM temp_lisavaljavaartus WHERE valjanimi = 'Võrdne kohtlemine') AS equality_of_treatment,
				v_rec.tahtaegkp AS workflow_due_date,
				(SELECT string_vaartus FROM temp_lisavaljavaartus WHERE valjanimi = 'Märksõnad') AS keywords_string
				;
		END IF;	


		--KUI KÕIK ÕNNESTUS
		INSERT INTO imp_completed_procedures(procedure_id, started_date_time, finished_date_time, parent_id, original_procedure_id)
		VALUES (v_rec.id, v_rec.alguskp, v_rec.loppkp, v_rec.menetlusy_id, v_rec.menetlusa_id);
			
	EXCEPTION
		WHEN RAISE_EXCEPTION THEN
			IF v_error_code = 'ERROR_1' THEN
				INSERT INTO imp_completed_procedures(procedure_id, started_date_time, finished_date_time, comment)
				VALUES (v_rec.id, v_rec.alguskp, v_rec.loppkp, (SELECT error_desc FROM imp_error_code WHERE error_code = v_error_code));
			ELSE
				IF v_error_code IN ('ERROR_8', 'ERROR_9') THEN v_task_user := v_task_rec.d_org_strukt_id;
				ELSIF v_error_code IN ('ERROR_10', 'ERROR_11') THEN v_task_user := v_task_rec.looja;
				END IF;
				EXECUTE imp_insert_failed(v_rec.id, v_rec.alguskp, v_rec.loppkp, v_task_user, v_error_code);
			END IF;
	/*
		WHEN others THEN 
			v_error_code := 'ERROR_0';
			EXECUTE imp_insert_failed(v_rec.id, v_rec.alguskp, v_rec.loppkp, v_task_user, v_error_code, SQLERRM);
		*/
	END;
	END LOOP;
	
		INSERT INTO imp_task(
		procedure_id, order_no, type, responsible, owner_name, owner_id, 
		owner_email, creator_name, creator_id, creator_email, resolution, 
		started_date_time, due_date, completed_date_time, status, outcome, 
		comment, owner_job_title, owner_org_name, workflow_order)
		SELECT t1.procedure_id, t1.order_no, t1.type, t1.responsible, t1.owner_name, t2.owner_id, t2.owner_email, 
			t1.creator_name, t3.creator_id, t3.creator_email, 
		t1.resolution, t1.started_date_time, t1.due_date, t1.completed_date_time, 
		t1.status, t1.outcome, t1.comment, t2.owner_job_title, t2.owner_org_name, t1.workflow_order
		FROM imp_task1 t1
		LEFT JOIN imp_task2 t2 ON t1.old_id = t2.old_id
		LEFT JOIN imp_task3 t3 ON t1.old_id = t3.old_id;

END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;