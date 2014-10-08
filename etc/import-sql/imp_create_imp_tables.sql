--csv failid
CREATE TABLE imp_d_menetlus
(
  id integer,
  prioriteet character varying(50),
  olek character varying(50),
  menetluseliik character varying(50),
  alguskp timestamp with time zone,
  loppkp timestamp with time zone,
  tahtaegkp timestamp with time zone,
  algataja_id integer,
  kirjeldus text,
  looja character varying(100),
  loomisekp timestamp with time zone,
  labibkantselei boolean,
  trykk boolean,
  menetlusy_id integer,
  menetlusa_id integer
 );

CREATE TABLE imp_d_dok_men
(
  menetlus_id integer,
  id integer,
  dokument_id integer,
  urldokument text,
  liik character varying(50),
  looja character varying(100),
  loomisekp timestamp with time zone
);

CREATE TABLE imp_d_isik_men (
menetlus_id integer, 
id integer, 
ylem_id integer,
staatus character varying(50),
kinnitatud character varying(50),
liik character varying(50),
d_org_strukt_id integer,
alguskp timestamp with time zone,
tahtaegkp timestamp with time zone,
loppkp timestamp with time zone,
looja character varying(100),
loomisekp timestamp with time zone
);

CREATE TABLE imp_d_kommentaar (
menetlus_id integer, 
id integer, 
kommentaar text,
looja character varying(100),
loomisekp timestamp with time zone
);

CREATE TABLE imp_d_lisavaljavaartus (
menetlus_id integer, 
id integer, 
menetluseliik character varying(50),
valjatyyp character varying(50),
valjanimi character varying(50),
combo_vaartus text,
string_vaartus text,
date_vaartus date,
int_vaartus integer,
is_selected boolean);


CREATE TABLE imp_resolutsioonid (
id integer, 
isik_men_id integer,
resolutsioon text
);

CREATE TABLE imp_d_kasut_ostr (
id integer, 
d_org_strukt_id integer, 
kasutaja character varying(100),
alguskp timestamp with time zone,
loppkp timestamp with time zone
);

CREATE TABLE imp_d_kasut_adsi (
displayname character varying(255),
samaccountname character varying(100)
);

CREATE TABLE imp_completed_docs (
document_id character varying(255),
node_ref character varying(255),
originallocation text
);

CREATE TABLE imp_delta_users (
name character varying(100),
username character varying(50),
email character varying(255),
job_title character varying(255),
org_name character varying(255)
);

--import v�ljundfailid
--lisamisel
CREATE TABLE imp_casefile (
procedure_id integer,
  volume_mark character varying(50),
  owner_name character varying(100),
  title text,
  user_name character varying(100),
  valid_from timestamp with time zone,
  valid_to timestamp with time zone,
  status character varying(50),
  contact_name character varying(255),
  applicant_type character varying(255),
  applicant_area character varying(255),
  application_language character varying(50),
  case_result text,
  supervision_visit boolean,
  opcat boolean,
  comment text,
  keyword_level1 character varying(50),
  keyword_level2 character varying(255),
  legislation text,
  general_right_to_equality boolean,
  discrimination boolean,
  good_administration boolean,
  child_rights boolean,
  child_applicant boolean,
  to_survey boolean,
  procedure_status character varying(50),
  equality_of_treatment boolean,
  workflow_due_date timestamp with time zone,
  keywords_string text);

CREATE TABLE imp_compound_workflow (
procedure_id integer,
type character varying(50),
title text,
comment text,
status character varying(50),
owner_name character varying(100),
owner_id character varying(50),
owner_email character varying(255),
creator_name character varying(100),
created_date_time timestamp with time zone,
started_date_time timestamp with time zone,
finished_date_time timestamp with time zone,
parent_id integer,
original_procedure_id integer);

CREATE TABLE imp_workflow (
procedure_id integer,
order_no integer,
type character varying(50),
started_date_time timestamp with time zone,
creator_name character varying(100),
status character varying(50));

CREATE TABLE imp_task (
procedure_id integer,
order_no integer,
type character varying(50),
responsible boolean,
owner_name character varying(100),
owner_id character varying(50),
owner_email character varying(255),
creator_name character varying(100),
creator_id character varying(50),
creator_email character varying(255),
resolution text,
started_date_time timestamp with time zone,
due_date timestamp with time zone,
completed_date_time timestamp with time zone,
status character varying(50),
outcome text,
comment text,
owner_job_title character varying(255),
owner_org_name character varying(255),
workflow_order integer);

CREATE TABLE imp_association (
procedure_id integer,
from_node text,
type character varying(50),
creator character varying(100),
created_date_time timestamp with time zone,
main_document boolean,
CONSTRAINT uq_imp_association UNIQUE (procedure_id , from_node )
);

CREATE TABLE imp_completed_procedures (
procedure_id integer,
started_date_time timestamp with time zone,
finished_date_time timestamp with time zone,
comment text,
node_ref character varying(255),
parent_id integer,
original_procedure_id integer);

CREATE TABLE imp_failed_procedures (
procedure_id integer,
started_date_time timestamp with time zone,
finished_date_time timestamp with time zone,
task_user character varying(100),
error_code character varying(50),
error_desc text);

CREATE TABLE imp_error_code (
error_code character varying(50),
error_desc character varying(255));

CREATE TABLE imp_task1
(
  procedure_id integer,
  order_no integer,
  type character varying(50),
  responsible boolean,
  owner_name character varying(100),
  creator_name character varying(100),
  resolution text,
  started_date_time timestamp with time zone,
  due_date timestamp with time zone,
  completed_date_time timestamp with time zone,
  status character varying(50),
  outcome text,
  comment text,
  old_id integer,
  old_parent_id integer,
  workflow_order integer);
  
CREATE TABLE imp_task2 (
procedure_id integer,
owner_id character varying(50),
owner_email character varying(255),
 old_id integer,
 owner_job_title character varying(255),
 owner_org_name character varying(255));

  CREATE TABLE imp_task3 (
procedure_id integer,
creator_id character varying(50),
creator_email character varying(255),
 old_id integer);


INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_0', 'Defineerimata viga menetluse importimisel');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_1', 'Katkestatud menetluse import on loetud �nnestunuks menetlust importimata');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_2', 'Menetluse olek ei vasta �helegi oodatud v��rtusele (Katkestatud, Kinnitatud, Publitseeritud, Kinnitusrajal v�i T�itmine)');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_3', 'Menetluse AlgusKP on v��rtustamata');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_4', 'Menetluse olekuks on Kinnitusrajal v�i T�itmine, kuid menetluse LoppKP on m��ratud');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_5', 'Tervikt��voo vastutaja ei ole tuvastatav. Menetluse Algataja_ID v��rtusele vastab rohkem kui �ks rida failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_6', 'Tervikt��voo vastutaja ei ole tuvastatav. Menetluse Algataja_ID v��rtusele vastav rida ei ole leitav failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_7', 'Menetluse peat�itja ei ole tuvastatav. Failis d_isik_men.csv leidub mitu v�i ei leidu �htegi peat�itja t���lesande rida');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_8', 'T���lesande omanik ei ole tuvastatav. T���lesande d_org_strukt_id v��rtusele vastab rohkem kui �ks rida failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_9', 'T���lesande omanik ei ole tuvastatav. T���lesande d_org_strukt_id v��rtusele ei vasta �htegi rida failis d_kasut_adsi');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_10', 'T���lesande algataja ei ole tuvastatav. T���lesande Looja v��rtusele vastab rohkem kui �ks rida failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_11', 'T���lesande algataja ei ole tuvastatav. T���lesande Looja v��rtusele ei vasta �htegi rida failis d_kasut_adsi');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_12', 'Tervikt��voo seis ei valideeru. T��voog sisaldab rohkem kui �hte teostamisel staatuses t���lesannet');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_13', 'T���lesande liik ei ole tuvastatav');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_14', 'Tervikt��voo seis ei valideeru. T���lesanded ei vasta  kronoloogiliselt j�rjestatuna j�rgmistele tingimustele: esimesena l�petatud v�i teostamata t���lesanded, seej�rel teostamisel t���lesanne ja viimasena k�ivitamata t���lesanded');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_15', 'Tervikt��voo kommentaari sisestaja ei ole tuvastatav. Kommentaari Looja v��rtusele vastab rohkem kui �ks rida failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_16', 'Tervikt��voo kommentaari sisestaja ei ole tuvastatav. Kommentaari Looja v��rtusele ei vasta �htegi rida failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_17', 'Tervikt��voo seis ei valideeru. T��voog sisaldab nii l�petatud/teostamata kui uus staatuses t���lesandeid, kuid puudub teostamisel t���lesanne');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_18', 'Asjamenetluse �Asjanumber� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks v�i ei leidu �htegi sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_19', 'Asjamenetluse �Menetleja perekonnanimi� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_20', 'Asjamenetluse �Avaldaja� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_21', 'Asjamenetluse �Avaldaja liik� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_22', 'Asjamenetluse �Avaldaja piirkond� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_23', 'Asjamenetluse �Avalduse keel� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_24', 'Asjamenetluse �Menetluse v�ljund� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_25', 'Asjamenetluse �Korraldatud kontrollk�ik� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_26', 'Asjamenetluse �OPCAT� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_27', 'Asjamenetluse �M�rkused� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_28', 'Asjamenetluse �Vastustaja� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_29', 'Asjamenetluse ��igusvaldkond� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_30', 'Asjamenetluse ��ldine v�rdsusp�hi�igus� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_31', 'Asjamenetluse �Diskrimineerimine� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_32', 'Asjamenetluse �Hea haldus� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_33', 'Asjamenetluse �Laste �igused� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_34', 'Asjamenetluse �Lapse p��rdumine� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_35', 'Asjamenetluse ��levaatesse� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_36', 'Asjamenetluse �Menetluse staatus� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_37', 'Asjamenetluse �II tasand KOV� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_38', 'Asjamenetluse �II tasand RIIK� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_39', 'Tervikt��voo vastutaja meiliaadress ei ole tuvastatav. Delta kasutaja andmetes puudub kasutaja meiliaadress');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_40', 'Vaikimisi vastutaja meiliaadress ei ole tuvastatav. Delta kasutaja andmetes, kelle isikukood on antud impordil sisendiks, puudub meiliaadress');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_41', 'T���lesande omaniku meiliaadress ei ole tuvastatav. Delta kasutaja andmetes puudub kasutaja meiliaadress');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_42', 'T���lesande algataja meiliaadress ei ole tuvastatav. Delta kasutaja andmetes puudub kasutaja meiliaadress');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_43', 'Asjamenetluse �V�rdne kohtlemine� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_44', 'Tervikt��voos on rohkem kui 2 t��voogu, kuid t���lesanded tulemiga "Ei kinnitanud" puuduvad');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_45', 'Tervikt��voo seis ei vasta reeglile: l�petatud t��vooge 0..n, teostamisel t��vooge 1, uus t��vooge 0..n');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_46', 'Asjamenetluse �M�rks�nad� ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui �ks sobivat v��rtust');