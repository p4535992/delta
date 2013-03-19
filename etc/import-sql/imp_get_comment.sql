-- Function: imp_get_comment(integer, character varying, timestamp with time zone, character varying, boolean, boolean)

-- DROP FUNCTION imp_get_comment(integer, character varying, timestamp with time zone, character varying, boolean, boolean);

CREATE OR REPLACE FUNCTION imp_get_comment(in_id integer, in_menetluseliik character varying, in_tahtaegkp timestamp with time zone, in_prioriteet character varying, in_labibkantselei boolean, in_trykk boolean)
  RETURNS text AS
$BODY$
DECLARE 
	v_vastus TEXT;
	v_eraldaja character varying(5) := CHR(10);
	v_kommentaar TEXT;
	v_puudu_dok TEXT;
	v_komm_lisaja character varying(255);
	v_komm_rec RECORD;

BEGIN
--Kommentaar
FOR v_komm_rec IN SELECT kommentaar, looja, loomisekp FROM imp_d_kommentaar WHERE menetlus_id = in_id
LOOP
BEGIN
	SELECT displayname INTO STRICT v_komm_lisaja
	FROM imp_d_kasut_adsi
	WHERE samaccountname = v_komm_rec.looja;

	IF v_kommentaar IS NULL THEN
		v_kommentaar := v_komm_lisaja || ' ' || TO_CHAR(v_komm_rec.loomisekp, 'DD.MM.YYYY HH:MI:SS') || ': ' || v_komm_rec.kommentaar;
	ELSE
		v_kommentaar := v_kommentaar || CHR(10) || v_komm_lisaja || ' ' || TO_CHAR(v_komm_rec.loomisekp, 'DD.MM.YYYY HH:MI:SS') || ': ' || v_komm_rec.kommentaar;
	END IF;
EXCEPTION
	WHEN no_data_found THEN
		v_vastus := 'ERROR_16';
		RAISE EXCEPTION '';
	WHEN too_many_rows THEN
		v_vastus := 'ERROR_15';
		RAISE EXCEPTION '';
END;
END LOOP;

v_vastus := 'Liik: '|| CASE WHEN COALESCE(in_menetluseliik, '') <> '' THEN in_menetluseliik ELSE 'väärtustamata' END || v_eraldaja;
v_vastus := v_vastus || 'Tähtaeg: '|| CASE WHEN in_tahtaegkp IS NOT NULL THEN TO_CHAR(in_tahtaegkp, 'DD.MM.YYYY') ELSE 'väärtustamata' END || v_eraldaja;
v_vastus := v_vastus || 'Prioriteet: '|| CASE WHEN COALESCE(in_prioriteet, '') <> '' THEN in_prioriteet ELSE 'väärtustamata' END || v_eraldaja;
v_vastus := v_vastus || 'Kantselei kontroll: '|| CASE WHEN in_labibkantselei = TRUE THEN 'läbib' ELSE 'ei läbi' END || v_eraldaja;
v_vastus := v_vastus || 'Väljatrükid: '|| CASE WHEN in_trykk = TRUE THEN 'olemas' ELSE 'puuduvad' END || v_eraldaja;
v_vastus := v_vastus || 'Kommentaar: ' || CASE WHEN v_kommentaar IS NOT NULL THEN v_kommentaar ELSE 'väärtustamata' END || v_eraldaja;

v_puudu_dok := (SELECT array_to_string(ARRAY(SELECT 'Terviktöövoos asus dokument ' || m.urldokument || ', mis on süsteemist kustutatud.'
FROM imp_d_dok_men2 m
WHERE m.menetlus_id = in_id AND m.liik = 'Seotud dokument' AND node_ref IS NULL), CHR(10)));

IF v_puudu_dok IS NOT NULL THEN 
	v_vastus := v_vastus || CHR(10) || v_puudu_dok;
END IF;

RETURN v_vastus;

EXCEPTION
	WHEN RAISE_EXCEPTION THEN
		RETURN v_vastus;
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;