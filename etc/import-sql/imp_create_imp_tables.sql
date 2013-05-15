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

--import väljundfailid
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
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_1', 'Katkestatud menetluse import on loetud õnnestunuks menetlust importimata');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_2', 'Menetluse olek ei vasta ühelegi oodatud väärtusele (Katkestatud, Kinnitatud, Publitseeritud, Kinnitusrajal või Täitmine)');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_3', 'Menetluse AlgusKP on väärtustamata');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_4', 'Menetluse olekuks on Kinnitusrajal või Täitmine, kuid menetluse LoppKP on määratud');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_5', 'Terviktöövoo vastutaja ei ole tuvastatav. Menetluse Algataja_ID väärtusele vastab rohkem kui üks rida failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_6', 'Terviktöövoo vastutaja ei ole tuvastatav. Menetluse Algataja_ID väärtusele vastav rida ei ole leitav failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_7', 'Menetluse peatäitja ei ole tuvastatav. Failis d_isik_men.csv leidub mitu või ei leidu ühtegi peatäitja tööülesande rida');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_8', 'Tööülesande omanik ei ole tuvastatav. Tööülesande d_org_strukt_id väärtusele vastab rohkem kui üks rida failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_9', 'Tööülesande omanik ei ole tuvastatav. Tööülesande d_org_strukt_id väärtusele ei vasta ühtegi rida failis d_kasut_adsi');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_10', 'Tööülesande algataja ei ole tuvastatav. Tööülesande Looja väärtusele vastab rohkem kui üks rida failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_11', 'Tööülesande algataja ei ole tuvastatav. Tööülesande Looja väärtusele ei vasta ühtegi rida failis d_kasut_adsi');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_12', 'Terviktöövoo seis ei valideeru. Töövoog sisaldab rohkem kui ühte teostamisel staatuses tööülesannet');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_13', 'Tööülesande liik ei ole tuvastatav');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_14', 'Terviktöövoo seis ei valideeru. Tööülesanded ei vasta  kronoloogiliselt järjestatuna järgmistele tingimustele: esimesena lõpetatud või teostamata tööülesanded, seejärel teostamisel tööülesanne ja viimasena käivitamata tööülesanded');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_15', 'Terviktöövoo kommentaari sisestaja ei ole tuvastatav. Kommentaari Looja väärtusele vastab rohkem kui üks rida failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_16', 'Terviktöövoo kommentaari sisestaja ei ole tuvastatav. Kommentaari Looja väärtusele ei vasta ühtegi rida failis d_kasut_adsi.csv');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_17', 'Terviktöövoo seis ei valideeru. Töövoog sisaldab nii lõpetatud/teostamata kui uus staatuses tööülesandeid, kuid puudub teostamisel tööülesanne');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_18', 'Asjamenetluse „Asjanumber“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks või ei leidu ühtegi sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_19', 'Asjamenetluse „Menetleja perekonnanimi“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_20', 'Asjamenetluse „Avaldaja“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_21', 'Asjamenetluse „Avaldaja liik“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_22', 'Asjamenetluse „Avaldaja piirkond“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_23', 'Asjamenetluse „Avalduse keel“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_24', 'Asjamenetluse „Menetluse väljund“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_25', 'Asjamenetluse „Korraldatud kontrollkäik“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_26', 'Asjamenetluse „OPCAT“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_27', 'Asjamenetluse „Märkused“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_28', 'Asjamenetluse „Vastustaja“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_29', 'Asjamenetluse „Õigusvaldkond“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_30', 'Asjamenetluse „Üldine võrdsuspõhiõigus“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_31', 'Asjamenetluse „Diskrimineerimine“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_32', 'Asjamenetluse „Hea haldus“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_33', 'Asjamenetluse „Laste õigused“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_34', 'Asjamenetluse „Lapse pöördumine“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_35', 'Asjamenetluse „Ülevaatesse“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_36', 'Asjamenetluse „Menetluse staatus“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_37', 'Asjamenetluse „II tasand KOV“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_38', 'Asjamenetluse „II tasand RIIK“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_39', 'Terviktöövoo vastutaja meiliaadress ei ole tuvastatav. Delta kasutaja andmetes puudub kasutaja meiliaadress');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_40', 'Vaikimisi vastutaja meiliaadress ei ole tuvastatav. Delta kasutaja andmetes, kelle isikukood on antud impordil sisendiks, puudub meiliaadress');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_41', 'Tööülesande omaniku meiliaadress ei ole tuvastatav. Delta kasutaja andmetes puudub kasutaja meiliaadress');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_42', 'Tööülesande algataja meiliaadress ei ole tuvastatav. Delta kasutaja andmetes puudub kasutaja meiliaadress');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_43', 'Asjamenetluse „Võrdne kohtlemine“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_44', 'Terviktöövoos on rohkem kui 2 töövoogu, kuid tööülesanded tulemiga "Ei kinnitanud" puuduvad');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_45', 'Terviktöövoo seis ei vasta reeglile: lõpetatud töövooge 0..n, teostamisel töövooge 1, uus töövooge 0..n');
INSERT INTO imp_error_code(error_code, error_desc) values('ERROR_46', 'Asjamenetluse „Märksõnad“ ei ole tuvastatav. Failis d_lisavaljaVaartus.csv leidub rohkem kui üks sobivat väärtust');