-- Function: imp_create_indexs()

-- DROP FUNCTION imp_create_indexs();

CREATE OR REPLACE FUNCTION imp_create_indexs()
  RETURNS void AS
$BODY$
DECLARE 

BEGIN

CREATE UNIQUE INDEX imp_completed_docs_idx
  ON imp_completed_docs
  USING btree
  (document_id);

CREATE INDEX imp_d_dok_men_menetlus_id
  ON imp_d_dok_men
  USING btree
  (menetlus_id);

CREATE UNIQUE INDEX imp_d_isik_men_id
  ON imp_d_isik_men
  USING btree
  (id );

CREATE INDEX imp_d_isik_men_menetlus_id
  ON imp_d_isik_men
  USING btree
  (menetlus_id );

CREATE INDEX imp_d_isik_men_org
  ON imp_d_isik_men
  USING btree
  (d_org_strukt_id );

CREATE INDEX imp_d_kasut_adsi_accountname
  ON imp_d_kasut_adsi
  USING btree
  (samaccountname);

CREATE INDEX imp_d_kasut_ostr_kasutaja
  ON imp_d_kasut_ostr
  USING btree
  (kasutaja);

CREATE INDEX imp_d_kasut_ostr_org
  ON imp_d_kasut_ostr
  USING btree
  (d_org_strukt_id );

CREATE INDEX imp_d_kommentaar_menetlus_id
  ON imp_d_kommentaar
  USING btree
  (menetlus_id );

CREATE INDEX imp_resolutsioonid_isik_men_id
  ON imp_resolutsioonid
  USING btree
  (isik_men_id );

CREATE INDEX imp_lisavaljavaartus_menetlus
  ON imp_d_lisavaljavaartus
  USING btree
  (menetlus_id );

CREATE INDEX imp_delta_users_name
  ON imp_delta_users
  USING btree
  (name);

CREATE INDEX imp_task1_old_id
  ON imp_task1
  USING btree
  (old_id );

CREATE INDEX imp_task1_procedure_id
  ON imp_task1
  USING btree
  (procedure_id );

 CREATE INDEX imp_task2_old_id
  ON imp_task2
  USING btree
  (old_id );

 CREATE INDEX imp_task3_old_id
  ON imp_task3
  USING btree
  (old_id );

CREATE INDEX imp_workflow_procedure_id
  ON imp_workflow
  USING btree
  (procedure_id);
  
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;